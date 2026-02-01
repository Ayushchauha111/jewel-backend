package com.example.jewell.service;

import com.example.jewell.model.Stock;
import com.example.jewell.model.StockHistory;
import com.example.jewell.repository.StockRepository;
import com.example.jewell.repository.StockHistoryRepository;
import com.example.jewell.storage.StockImageStorage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.jewell.model.BillingItem;
import com.example.jewell.repository.BillingItemRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class StockService {
    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private QRCodeService qrCodeService;

    @Autowired
    private StockImageStorage stockImageStorage;

    @Autowired
    private ImageCompressionService imageCompressionService;

    @Autowired
    private IncomeExpenseService incomeExpenseService;

    @Autowired
    private GoldPriceService goldPriceService;

    @Autowired
    private com.example.jewell.service.DailyRateService dailyRateService;

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    @Autowired
    private BillingItemRepository billingItemRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${shop.makingChargesPerGram:1150}")
    private BigDecimal makingChargesPerGram;

    private static final BigDecimal GST_RATE = new BigDecimal("0.03");   // 3% total
    private static final BigDecimal CGST_RATE = new BigDecimal("0.015"); // 1.5%
    private static final BigDecimal SGST_RATE = new BigDecimal("0.015"); // 1.5%

    public List<Stock> getAllStock() {
        return stockRepository.findAll();
    }

    public Page<Stock> getAllStockPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return stockRepository.findAll(pageable);
    }

    public List<Stock> searchStock(String query) {
        return stockRepository.searchStock(query);
    }

    public Page<Stock> searchStockPaginated(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return stockRepository.searchStock(query, pageable);
    }

    public List<Stock> getStockByCategory(String category) {
        return stockRepository.findByCategory(category);
    }

    public Page<Stock> getStockByCategoryPaginated(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return stockRepository.findByCategory(category, pageable);
    }

    public Page<Stock> getStockByMaterialPaginated(String material, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return stockRepository.findByMaterial(material, pageable);
    }

    public Page<Stock> getStockByCategoryAndMaterialPaginated(String category, String material, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return stockRepository.findByCategoryAndMaterial(category, material, pageable);
    }

    public List<String> getAllCategories() {
        return stockRepository.findAllDistinctCategories();
    }

    public List<String> getAllArticleNames() {
        return stockRepository.findAllDistinctArticleNames();
    }

    public Optional<Stock> getStockById(Long id) {
        return stockRepository.findById(id);
    }

    public Optional<Stock> getStockByArticleCode(String articleCode) {
        return stockRepository.findByArticleCode(articleCode);
    }

    public List<Stock> getStockByStatus(Stock.StockStatus status) {
        return stockRepository.findByStatus(status);
    }

    /**
     * Returns sales history for a stock: which bills it was sold in (bill number, date, quantity sold).
     */
    public List<Map<String, Object>> getSalesHistoryForStock(Long stockId) {
        List<BillingItem> items = billingItemRepository.findByStockIdWithBilling(stockId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (BillingItem bi : items) {
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("billId", bi.getBilling().getId());
            row.put("billNumber", bi.getBilling().getBillNumber());
            row.put("createdAt", bi.getBilling().getCreatedAt() != null ? bi.getBilling().getCreatedAt().toString() : null);
            row.put("quantitySold", bi.getQuantity());
            result.add(row);
        }
        return result;
    }

    /**
     * Calculates selling price from today's (or latest) gold rate: weight (grams) × price per gram for given carat.
     * Uses unified DailyRate first, then falls back to GoldPrice. Returns empty if gold rate or inputs are missing.
     */
    public Optional<BigDecimal> calculateSellingPriceFromGoldRate(BigDecimal weightGrams, BigDecimal carat) {
        if (weightGrams == null || weightGrams.compareTo(BigDecimal.ZERO) <= 0
                || carat == null || carat.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        Optional<BigDecimal> ratePerGram = dailyRateService.getGoldRateForCarat(carat)
                .or(() -> goldPriceService.getPricePerGramForCarat(carat));
        return ratePerGram.map(rate -> weightGrams.multiply(rate).setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Full price breakdown: gold value + making charges (per gram) + CGST 1.5% + SGST 1.5% (3% total).
     * Subtotal = goldValue + makingCharges; GST on subtotal; totalPrice = subtotal + GST.
     * If articleMakingChargesPerGram is not null and > 0, use it; else use global shop.makingChargesPerGram.
     */
    public Optional<Map<String, Object>> calculatePriceWithMakingAndGst(BigDecimal weightGrams, BigDecimal carat) {
        return calculatePriceWithMakingAndGst(weightGrams, carat, null);
    }

    public Optional<Map<String, Object>> calculatePriceWithMakingAndGst(BigDecimal weightGrams, BigDecimal carat, BigDecimal articleMakingChargesPerGram) {
        if (weightGrams == null || weightGrams.compareTo(BigDecimal.ZERO) <= 0
                || carat == null || carat.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        Optional<BigDecimal> ratePerGram = dailyRateService.getGoldRateForCarat(carat)
                .or(() -> goldPriceService.getPricePerGramForCarat(carat));
        if (ratePerGram.isEmpty()) return Optional.empty();

        BigDecimal mcPerGram = (articleMakingChargesPerGram != null && articleMakingChargesPerGram.compareTo(BigDecimal.ZERO) > 0)
                ? articleMakingChargesPerGram : makingChargesPerGram;
        BigDecimal goldValue = weightGrams.multiply(ratePerGram.get()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal makingCharges = weightGrams.multiply(mcPerGram).setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = goldValue.add(makingCharges);
        BigDecimal gstTotal = subtotal.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cgst = subtotal.multiply(CGST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sgst = subtotal.multiply(SGST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPrice = subtotal.add(gstTotal);

        Map<String, Object> out = new java.util.HashMap<>();
        out.put("goldValue", goldValue);
        out.put("makingCharges", makingCharges);
        out.put("subtotal", subtotal);
        out.put("cgst", cgst);
        out.put("sgst", sgst);
        out.put("gstTotal", gstTotal);
        out.put("totalPrice", totalPrice);
        out.put("goldRatePerGram", ratePerGram.get());
        return Optional.of(out);
    }

    public Stock createStock(Stock stock) {
        try {
            System.out.println("Creating stock with articleName: " + stock.getArticleName());
            System.out.println("WeightGrams: " + stock.getWeightGrams() + " (type: " + (stock.getWeightGrams() != null ? stock.getWeightGrams().getClass().getName() : "null") + ")");
            System.out.println("Carat: " + stock.getCarat() + " (type: " + (stock.getCarat() != null ? stock.getCarat().getClass().getName() : "null") + ")");
            
            // Ensure BigDecimal fields are properly set (handle null/empty strings)
            if (stock.getWeightGrams() == null && stock.getArticleName() != null) {
                throw new IllegalArgumentException("Weight in grams is required");
            }
            if (stock.getCarat() == null && stock.getArticleName() != null) {
                throw new IllegalArgumentException("Carat is required");
            }
            
            // Validate purity percentage (0-100)
            if (stock.getPurityPercentage() != null) {
                double purity = stock.getPurityPercentage().doubleValue();
                if (purity < 0 || purity > 100) {
                    throw new IllegalArgumentException("Purity percentage must be between 0 and 100");
                }
            }
            
            // Auto-generate article code if not provided
            if (stock.getArticleCode() == null || stock.getArticleCode().trim().isEmpty()) {
                String articleCode = generateArticleCode(stock.getArticleName());
                // Ensure uniqueness
                int counter = 1;
                String uniqueCode = articleCode;
                while (stockRepository.findByArticleCode(uniqueCode).isPresent()) {
                    uniqueCode = articleCode + "-" + counter;
                    counter++;
                }
                stock.setArticleCode(uniqueCode);
            }
            
            // Generate QR code if not provided
            if (stock.getQrCode() == null || stock.getQrCode().isEmpty()) {
                String qrData = generateQRData(stock);
                stock.setQrCode(qrCodeService.generateQRCode(qrData));
            }
            
            System.out.println("Saving stock to database...");
            Stock saved = stockRepository.save(stock);
            System.out.println("Stock saved successfully with ID: " + saved.getId());
            
            // Record expense transaction for stock purchase
            incomeExpenseService.recordStockPurchaseExpense(saved);
            
            return saved;
        } catch (IllegalArgumentException e) {
            System.err.println("Validation error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Error creating stock: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error creating stock: " + e.getMessage(), e);
        }
    }

    public Stock updateStock(Long id, Stock stockDetails) {
        try {
            System.out.println("Updating stock with ID: " + id);
            System.out.println("Received stock details - ArticleName: " + stockDetails.getArticleName());
            System.out.println("WeightGrams: " + stockDetails.getWeightGrams());
            System.out.println("Carat: " + stockDetails.getCarat());
            
            Stock stock = stockRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Stock not found with id: " + id));
            
            // Validate required fields
            if (stockDetails.getWeightGrams() == null) {
                throw new IllegalArgumentException("Weight in grams is required");
            }
            if (stockDetails.getCarat() == null) {
                throw new IllegalArgumentException("Carat is required");
            }
            
            // Validate purity percentage before setting
            if (stockDetails.getPurityPercentage() != null) {
                try {
                    double purity = stockDetails.getPurityPercentage().doubleValue();
                    if (Double.isNaN(purity) || Double.isInfinite(purity)) {
                        throw new IllegalArgumentException("Purity percentage must be a valid number");
                    }
                    if (purity < 0 || purity > 100) {
                        throw new IllegalArgumentException("Purity percentage must be between 0 and 100");
                    }
                } catch (NumberFormatException | NullPointerException e) {
                    throw new IllegalArgumentException("Invalid purity percentage value: " + stockDetails.getPurityPercentage());
                }
            }
            
            stock.setArticleName(stockDetails.getArticleName());
            stock.setCategory(stockDetails.getCategory());
            stock.setMaterial(stockDetails.getMaterial());
            
            // Only update articleCode if it's provided and doesn't conflict
            if (stockDetails.getArticleCode() != null) {
                String newArticleCode = stockDetails.getArticleCode().trim();
                if (!newArticleCode.isEmpty()) {
                    Optional<Stock> existingStock = stockRepository.findByArticleCode(newArticleCode);
                    if (existingStock.isPresent() && !existingStock.get().getId().equals(id)) {
                        throw new IllegalArgumentException("Article code already exists: " + newArticleCode);
                    }
                    stock.setArticleCode(newArticleCode);
                } else {
                    // Allow clearing article code
                    stock.setArticleCode(null);
                }
            }
            
            stock.setWeightGrams(stockDetails.getWeightGrams());
            stock.setCarat(stockDetails.getCarat());
            stock.setDiamondCarat(stockDetails.getDiamondCarat());
            stock.setPurityPercentage(stockDetails.getPurityPercentage());
            stock.setPurchasePrice(stockDetails.getPurchasePrice());
            stock.setSellingPrice(stockDetails.getSellingPrice());
            stock.setMakingChargesPerGram(stockDetails.getMakingChargesPerGram());
            stock.setQuantity(stockDetails.getQuantity() != null ? stockDetails.getQuantity() : stock.getQuantity() != null ? stock.getQuantity() : 1);
            stock.setSize(stockDetails.getSize() != null ? stockDetails.getSize() : stock.getSize());
            stock.setDescription(stockDetails.getDescription() != null ? stockDetails.getDescription() : stock.getDescription());
            // Only update status if provided, otherwise keep existing
            if (stockDetails.getStatus() != null) {
                stock.setStatus(stockDetails.getStatus());
            }
            if (stockDetails.getImageUrl() != null) {
                stock.setImageUrl(stockDetails.getImageUrl());
            }
            
            // Regenerate QR code if needed
            if (stock.getQrCode() == null || stock.getQrCode().isEmpty()) {
                String qrData = generateQRData(stock);
                stock.setQrCode(qrCodeService.generateQRCode(qrData));
            }
            
            return stockRepository.save(stock);
        } catch (IllegalArgumentException e) {
            System.err.println("Validation error in updateStock: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Error updating stock: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error updating stock: " + e.getMessage(), e);
        }
    }

    public void deleteStock(Long id) {
        // Fetch the stock before deletion to create history
        Stock stock = stockRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock not found with id: " + id));
        
        // Create history record before deletion
        StockHistory stockHistory = new StockHistory(stock, "Stock deleted by admin");
        stockHistoryRepository.save(stockHistory);
        
        // First, set stock_id to NULL in billing_items to avoid foreign key constraint violation
        entityManager.createNativeQuery("UPDATE billing_items SET stock_id = NULL WHERE stock_id = :stockId")
                .setParameter("stockId", id)
                .executeUpdate();
        
        // Set stock_id to NULL in transaction_history to avoid foreign key constraint violation
        entityManager.createNativeQuery("UPDATE transaction_history SET stock_id = NULL WHERE stock_id = :stockId")
                .setParameter("stockId", id)
                .executeUpdate();
        
        // Now safe to delete the stock
        stockRepository.deleteById(id);
    }

    public String uploadStockImage(MultipartFile file) throws IOException {
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
            ? originalFilename.substring(originalFilename.lastIndexOf("."))
            : ".jpg";
        String filename = UUID.randomUUID().toString() + extension;
        String objectName = "stock-images/" + filename;

        // Compress the image
        InputStream compressedImageStream = imageCompressionService.compressImage(file);

        // Upload to OCI
        stockImageStorage.upload(objectName, compressedImageStream);

        // Return the public URL
        String publicUrl = stockImageStorage.getPublicUrl(objectName);
        return publicUrl != null ? publicUrl : objectName;
    }

    public Stock regenerateQRCode(Long id) {
        Stock stock = stockRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock not found with id: " + id));
        String qrData = generateQRData(stock);
        stock.setQrCode(qrCodeService.generateQRCode(qrData));
        return stockRepository.save(stock);
    }

    public int regenerateAllQRCodes() {
        List<Stock> allStock = stockRepository.findAll();
        int count = 0;
        for (Stock stock : allStock) {
            try {
                String qrData = generateQRData(stock);
                stock.setQrCode(qrCodeService.generateQRCode(qrData));
                stockRepository.save(stock);
                count++;
            } catch (Exception e) {
                System.err.println("Error regenerating QR code for stock ID " + stock.getId() + ": " + e.getMessage());
            }
        }
        return count;
    }

    private String generateQRData(Stock stock) {
        // Create JSON structure for better QR code scanner compatibility
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"id\":").append(stock.getId() != null ? stock.getId() : 0).append(",");
        json.append("\"articleName\":\"").append(escapeJson(stock.getArticleName() != null ? stock.getArticleName() : "")).append("\",");
        if (stock.getArticleCode() != null && !stock.getArticleCode().isEmpty()) {
            json.append("\"articleCode\":\"").append(escapeJson(stock.getArticleCode())).append("\",");
        }
        json.append("\"weightGrams\":").append(stock.getWeightGrams() != null ? stock.getWeightGrams() : "0").append(",");
        json.append("\"carat\":").append(stock.getCarat() != null ? stock.getCarat() : "0").append(",");
        if (stock.getPurityPercentage() != null) {
            json.append("\"purityPercentage\":").append(stock.getPurityPercentage()).append(",");
        }
        json.append("\"sellingPrice\":").append(stock.getSellingPrice() != null ? stock.getSellingPrice() : "0");
        json.append("}");
        return json.toString();
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    private String generateArticleCode(String articleName) {
        if (articleName == null || articleName.trim().isEmpty()) {
            return "ART-" + System.currentTimeMillis();
        }
        
        // Generate code from article name: take first 3 letters of each word, uppercase
        String[] words = articleName.trim().toUpperCase().split("\\s+");
        StringBuilder code = new StringBuilder();
        
        for (String word : words) {
            if (word.length() >= 3) {
                code.append(word.substring(0, 3));
            } else {
                code.append(word);
            }
        }
        
        // Add timestamp suffix for uniqueness
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(7);
        return code.toString() + "-" + timestamp;
    }
}
