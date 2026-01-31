package com.example.jewell.controller;

import com.example.jewell.dto.PageResponse;
import com.example.jewell.model.Stock;
import com.example.jewell.service.StockService;
import com.example.jewell.service.StockHistoryService;
import com.example.jewell.service.GoldPriceService;
import com.example.jewell.service.DailyRateService;
import com.example.jewell.model.StockHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/stock")
public class StockController {
    @Autowired
    private StockService stockService;

    @Autowired
    private StockHistoryService stockHistoryService;

    @Autowired
    private GoldPriceService goldPriceService;
    @Autowired
    private DailyRateService dailyRateService;

    /**
     * Calculate article selling price from today's (or latest) gold rate: weight × price per gram for carat.
     * Uses unified rates first. GET /api/stock/calculate-price?weightGrams=10&carat=22
     */
    @GetMapping("/calculate-price")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> calculatePriceFromGoldRate(
            @RequestParam BigDecimal weightGrams,
            @RequestParam BigDecimal carat) {
        Map<String, Object> result = new HashMap<>();
        Optional<BigDecimal> calculated = stockService.calculateSellingPriceFromGoldRate(weightGrams, carat);
        if (calculated.isEmpty()) {
            result.put("error", "Gold rate not set or invalid weight/carat. Set today's rates (Rates page) first.");
            return ResponseEntity.badRequest().body(result);
        }
        result.put("calculatedPrice", calculated.get());
        dailyRateService.getGoldRateForCarat(carat).or(() -> goldPriceService.getPricePerGramForCarat(carat))
                .ifPresent(rate -> result.put("goldRatePerGram", rate));
        dailyRateService.getTodayOrLatest().ifPresent(r -> result.put("rateDate", r.getPriceDate().toString()));
        if (result.get("rateDate") == null)
            goldPriceService.getTodayOrLatestGoldPrice().ifPresent(gp -> result.put("rateDate", gp.getPriceDate().toString()));
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<?> getAllStock(@RequestParam(required = false) String search,
                                         @RequestParam(required = false) String category,
                                         @RequestParam(required = false) String material,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        Page<Stock> stockPage;
        if (search != null && !search.trim().isEmpty()) {
            stockPage = stockService.searchStockPaginated(search, page, size);
        } else if (category != null && !category.trim().isEmpty() && material != null && !material.trim().isEmpty()) {
            stockPage = stockService.getStockByCategoryAndMaterialPaginated(category, material, page, size);
        } else if (category != null && !category.trim().isEmpty()) {
            stockPage = stockService.getStockByCategoryPaginated(category, page, size);
        } else if (material != null && !material.trim().isEmpty()) {
            stockPage = stockService.getStockByMaterialPaginated(material, page, size);
        } else {
            stockPage = stockService.getAllStockPaginated(page, size);
        }
        return ResponseEntity.ok(PageResponse.of(stockPage));
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Stock>> searchStock(@RequestParam String query) {
        return ResponseEntity.ok(stockService.searchStock(query));
    }
    
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(stockService.getAllCategories());
    }
    
    @GetMapping("/article-names")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<String>> getAllArticleNames() {
        return ResponseEntity.ok(stockService.getAllArticleNames());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Stock> getStockById(@PathVariable Long id) {
        return stockService.getStockById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/sales-history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStockSalesHistory(@PathVariable Long id) {
        if (stockService.getStockById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stockService.getSalesHistoryForStock(id));
    }

    @GetMapping("/code/{articleCode}")
    public ResponseEntity<Stock> getStockByCode(@PathVariable String articleCode) {
        return stockService.getStockByArticleCode(articleCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Stock>> getStockByStatus(@PathVariable Stock.StockStatus status) {
        return ResponseEntity.ok(stockService.getStockByStatus(status));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createStock(@RequestBody Stock stock) {
        try {
            // Log the received stock for debugging
            System.out.println("Received stock: " + stock.getArticleName());
            System.out.println("Weight: " + stock.getWeightGrams());
            System.out.println("Carat: " + stock.getCarat());
            
            return ResponseEntity.ok(stockService.createStock(stock));
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Validation error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(400).body(error);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create stock");
            String errorMsg = e.getMessage();
            if (e.getCause() != null) {
                errorMsg += " - " + e.getCause().getMessage();
            }
            error.put("message", errorMsg != null ? errorMsg : "Unknown error occurred");
            return ResponseEntity.status(500).body(error);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStock(@PathVariable Long id, @RequestBody Stock stock) {
        try {
            return ResponseEntity.ok(stockService.updateStock(id, stock));
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Validation Error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(400).body(error);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update stock");
            String errorMsg = e.getMessage();
            if (e.getCause() != null) {
                errorMsg += " - " + e.getCause().getMessage();
            }
            error.put("message", errorMsg != null ? errorMsg : "Unknown error occurred");
            return ResponseEntity.status(500).body(error);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStock(@PathVariable Long id) {
        stockService.deleteStock(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/upload-image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = stockService.uploadStockImage(file);
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to upload image: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/{id}/qr-code")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> getQRCodeImage(@PathVariable Long id) {
        Optional<Stock> stockOpt = stockService.getStockById(id);
        if (stockOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Stock stock = stockOpt.get();
        if (stock.getQrCode() == null || stock.getQrCode().isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            byte[] qrCodeBytes = Base64.getDecoder().decode(stock.getQrCode());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentDispositionFormData("attachment", 
                "qr-code-" + (stock.getArticleCode() != null ? stock.getArticleCode() : stock.getId()) + ".png");
            return new ResponseEntity<>(qrCodeBytes, headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/regenerate-qr")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> regenerateQRCode(@PathVariable Long id) {
        try {
            Stock stock = stockService.regenerateQRCode(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "QR code regenerated successfully");
            response.put("qrCode", stock.getQrCode());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to regenerate QR code");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/regenerate-all-qr")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> regenerateAllQRCodes() {
        try {
            int count = stockService.regenerateAllQRCodes();
            Map<String, Object> response = new HashMap<>();
            response.put("message", "QR codes regenerated successfully");
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to regenerate QR codes");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StockHistory>> getStockHistory() {
        return ResponseEntity.ok(stockHistoryService.getAllStockHistory());
    }

    @GetMapping("/history/{originalStockId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StockHistory>> getStockHistoryByOriginalId(@PathVariable Long originalStockId) {
        return ResponseEntity.ok(stockHistoryService.getStockHistoryByOriginalId(originalStockId));
    }
}
