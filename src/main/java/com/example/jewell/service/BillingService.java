package com.example.jewell.service;

import com.example.jewell.model.*;
import com.example.jewell.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class BillingService {
    @Autowired
    private BillingRepository billingRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private CreditRepository creditRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private IncomeExpenseService incomeExpenseService;

    @Autowired
    private PromoCodeService promoCodeService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LoyaltyTransactionRepository loyaltyTransactionRepository;

    @Autowired
    private GiftVoucherService giftVoucherService;

    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;

    @Autowired
    private BillingItemRepository billingItemRepository;

    @Autowired
    private BillingEditHistoryRepository billingEditHistoryRepository;

    /** Points earned per 100 rupees of bill (configurable). */
    private static final int LOYALTY_POINTS_PER_100 = 1;

    public List<Billing> getAllBills() {
        return billingRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public Page<Billing> getAllBillsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return billingRepository.findAll(pageable);
    }

    public List<Billing> searchBills(String query) {
        return billingRepository.searchBills(query).stream()
                .sorted(Comparator.comparing(Billing::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public Page<Billing> searchBillsPaginated(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return billingRepository.searchBills(query, pageable);
    }

    public Optional<Billing> getBillById(Long id) {
        return billingRepository.findById(id);
    }

    public Optional<Billing> getBillByBillNumber(String billNumber) {
        return billingRepository.findByBillNumber(billNumber);
    }

    public Billing createBill(Billing billing) {
        // Generate bill number
        if (billing.getBillNumber() == null || billing.getBillNumber().isEmpty()) {
            billing.setBillNumber(generateBillNumber());
        }

        // Set billing reference and item name/articleCode from stock for each item
        if (billing.getItems() != null) {
            for (BillingItem item : billing.getItems()) {
                item.setBilling(billing);
                if (item.getStock() != null && item.getStock().getId() != null) {
                    Stock s = stockRepository.findById(item.getStock().getId()).orElse(null);
                    if (s != null) {
                        if (item.getItemName() == null || item.getItemName().isEmpty()) {
                            item.setItemName(s.getArticleName());
                        }
                        item.setArticleCode(s.getArticleCode());
                        if (item.getWeightGrams() == null && s.getWeightGrams() != null) item.setWeightGrams(s.getWeightGrams());
                        if (item.getCarat() == null && s.getCarat() != null) item.setCarat(s.getCarat());
                        if (item.getDiamondCarat() == null && s.getDiamondCarat() != null) item.setDiamondCarat(s.getDiamondCarat());
                    }
                }
            }
        }

        // Calculate totals (filter out null prices)
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (billing.getItems() != null) {
            totalAmount = billing.getItems().stream()
                    .filter(item -> item.getTotalPrice() != null)
                    .map(BillingItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        billing.setTotalAmount(totalAmount);

        BigDecimal discount = billing.getDiscountAmount() != null ? billing.getDiscountAmount() : BigDecimal.ZERO;
        if (billing.getPromoCode() != null && !billing.getPromoCode().trim().isEmpty()) {
            BigDecimal promoDiscount = promoCodeService.validateAndGetDiscount(billing.getPromoCode().trim(), totalAmount).orElse(BigDecimal.ZERO);
            discount = discount.add(promoDiscount);
            promoCodeService.applyAndIncrementUsage(billing.getPromoCode().trim(), totalAmount);
        }
        BigDecimal makingCharges = billing.getMakingCharges() != null ? billing.getMakingCharges() : BigDecimal.ZERO;
        BigDecimal finalAmount = totalAmount.subtract(discount).add(makingCharges);

        // Gift voucher: reduce final amount by voucher value
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        if (billing.getVoucherCode() != null && !billing.getVoucherCode().trim().isEmpty()) {
            voucherDiscount = giftVoucherService.validateAndGetAmount(billing.getVoucherCode().trim()).orElse(BigDecimal.ZERO);
            if (voucherDiscount.compareTo(finalAmount) > 0) voucherDiscount = finalAmount;
            if (voucherDiscount.compareTo(BigDecimal.ZERO) > 0) {
                finalAmount = finalAmount.subtract(voucherDiscount);
                discount = discount.add(voucherDiscount);
            }
        }

        // Loyalty redeem: reduce final amount by points (1 point = 1 rupee)
        int toRedeem = 0;
        if (billing.getRedeemPoints() != null && billing.getRedeemPoints() > 0 && billing.getCustomer() != null && billing.getCustomer().getId() != null) {
            Customer cust = customerRepository.findById(billing.getCustomer().getId()).orElse(null);
            if (cust != null && cust.getLoyaltyPoints() != null && cust.getLoyaltyPoints().compareTo(BigDecimal.ZERO) > 0) {
                int currentPoints = cust.getLoyaltyPoints().intValue();
                toRedeem = Math.min(billing.getRedeemPoints(), currentPoints);
                if (toRedeem > 0) {
                    BigDecimal redeemValue = BigDecimal.valueOf(toRedeem);
                    finalAmount = finalAmount.subtract(redeemValue);
                    discount = discount.add(redeemValue);
                }
            }
        }
        billing.setFinalAmount(finalAmount);
        billing.setDiscountAmount(discount);

        // Handle paid amount: support split payment (paymentBreakdown) or single method + amount
        BigDecimal paidAmount = BigDecimal.ZERO;
        String breakdown = billing.getPaymentBreakdown();
        if (breakdown != null && !breakdown.trim().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> payments = mapper.readValue(breakdown, new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> p : payments) {
                    Object amt = p.get("amount");
                    if (amt != null) {
                        if (amt instanceof Number) paidAmount = paidAmount.add(BigDecimal.valueOf(((Number) amt).doubleValue()));
                        else paidAmount = paidAmount.add(new BigDecimal(amt.toString()));
                    }
                }
                paidAmount = paidAmount.setScale(2, java.math.RoundingMode.HALF_UP);
                billing.setPaidAmount(paidAmount);
                billing.setPaymentMethod(Billing.PaymentMethod.MIXED);
            } catch (Exception e) {
                // fallback to single payment
                paidAmount = billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO;
                billing.setPaidAmount(paidAmount);
            }
        } else {
            paidAmount = billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO;
            if (billing.getPaymentMethod() == Billing.PaymentMethod.CASH && paidAmount.compareTo(BigDecimal.ZERO) == 0) {
                paidAmount = finalAmount;
            }
            billing.setPaidAmount(paidAmount);
        }
        
        // Set payment status based on paid amount
        if (paidAmount.compareTo(finalAmount) >= 0) {
            billing.setPaymentStatus(Billing.PaymentStatus.PAID);
        } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            billing.setPaymentStatus(Billing.PaymentStatus.PARTIAL);
        } else {
            billing.setPaymentStatus(Billing.PaymentStatus.PENDING);
        }

        // Validate stock quantity before saving: total ordered must not exceed available
        if (billing.getItems() != null) {
            Map<Long, Integer> stockIdToQtyRequested = new HashMap<>();
            for (BillingItem item : billing.getItems()) {
                if (item.getStock() != null && item.getStock().getId() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                    Long sid = item.getStock().getId();
                    stockIdToQtyRequested.merge(sid, item.getQuantity(), Integer::sum);
                }
            }
            for (Map.Entry<Long, Integer> e : stockIdToQtyRequested.entrySet()) {
                Stock stock = stockRepository.findById(e.getKey())
                        .orElseThrow(() -> new RuntimeException("Stock not found with id: " + e.getKey()));
                int available = stock.getQuantity() != null ? stock.getQuantity() : 0;
                if (e.getValue() > available) {
                    throw new IllegalArgumentException("Insufficient stock for " + stock.getArticleName() + " (" + stock.getArticleCode() + "): only " + available + " available, " + e.getValue() + " requested.");
                }
            }
        }

        // Save billing
        Billing savedBilling = billingRepository.save(billing);

        // Deduct sold quantity from stock; set SOLD only when quantity reaches 0
        if (savedBilling.getItems() != null) {
            Map<Long, Integer> stockIdToQtySold = new HashMap<>();
            for (BillingItem item : savedBilling.getItems()) {
                if (item.getStock() != null && item.getStock().getId() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                    Long sid = item.getStock().getId();
                    stockIdToQtySold.merge(sid, item.getQuantity(), Integer::sum);
                }
            }
            for (Map.Entry<Long, Integer> e : stockIdToQtySold.entrySet()) {
                Stock stock = stockRepository.findById(e.getKey())
                        .orElseThrow(() -> new RuntimeException("Stock not found with id: " + e.getKey()));
                int currentQty = stock.getQuantity() != null ? stock.getQuantity() : 0;
                int sold = e.getValue();
                int newQty = Math.max(0, currentQty - sold);
                stock.setQuantity(newQty);
                if (newQty <= 0) {
                    stock.setStatus(Stock.StockStatus.SOLD);
                }
                stockRepository.save(stock);
            }
        }

        // Create credit entry if payment method is CREDIT or if partial payment was made
        BigDecimal remainingAmount = finalAmount.subtract(paidAmount);
        
        if (billing.getPaymentMethod() == Billing.PaymentMethod.CREDIT) {
            // Full credit for CREDIT payment method
            Credit credit = new Credit();
            credit.setCustomer(billing.getCustomer());
            credit.setBilling(savedBilling);
            credit.setCreditAmount(finalAmount);
            credit.setRemainingAmount(finalAmount);
            credit.setStatus(Credit.CreditStatus.PENDING);
            credit.setDescription("Credit for Bill: " + savedBilling.getBillNumber());
            creditRepository.save(credit);
        } else if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Partial payment - create credit for remaining amount
            Credit credit = new Credit();
            credit.setCustomer(billing.getCustomer());
            credit.setBilling(savedBilling);
            credit.setCreditAmount(remainingAmount);
            credit.setRemainingAmount(remainingAmount);
            credit.setStatus(Credit.CreditStatus.PENDING);
            credit.setDescription("Partial payment - Remaining amount for Bill: " + savedBilling.getBillNumber() + 
                                 " (Paid: ₹" + paidAmount + ", Remaining: ₹" + remainingAmount + ")");
            creditRepository.save(credit);
        }

        // Record income transaction for the paid amount
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0 && savedBilling.getPaymentStatus() == Billing.PaymentStatus.PAID) {
            incomeExpenseService.recordIncomeFromBilling(savedBilling);
        } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0 && savedBilling.getPaymentStatus() == Billing.PaymentStatus.PARTIAL) {
            // For partial payments, record income for the paid amount
            incomeExpenseService.recordIncomeFromBilling(savedBilling);
        }

        // Loyalty: earn points (1 per 100 rupees) and apply redeem
        if (savedBilling.getCustomer() != null && savedBilling.getCustomer().getId() != null) {
            Customer cust = customerRepository.findById(savedBilling.getCustomer().getId()).orElse(null);
            if (cust != null) {
                BigDecimal finalAmt = savedBilling.getFinalAmount();
                if (finalAmt != null && finalAmt.compareTo(BigDecimal.ZERO) > 0) {
                    int pointsEarned = finalAmt.divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN).intValue() * LOYALTY_POINTS_PER_100;
                    if (pointsEarned > 0) {
                        BigDecimal current = cust.getLoyaltyPoints() != null ? cust.getLoyaltyPoints() : BigDecimal.ZERO;
                        cust.setLoyaltyPoints(current.add(BigDecimal.valueOf(pointsEarned)));
                        customerRepository.save(cust);
                        LoyaltyTransaction earn = new LoyaltyTransaction();
                        earn.setCustomer(cust);
                        earn.setType(LoyaltyTransaction.TransactionType.EARN);
                        earn.setPoints(BigDecimal.valueOf(pointsEarned));
                        earn.setBilling(savedBilling);
                        earn.setDescription("Earned on Bill " + savedBilling.getBillNumber());
                        loyaltyTransactionRepository.save(earn);
                    }
                }
                if (toRedeem > 0) {
                    BigDecimal current = cust.getLoyaltyPoints() != null ? cust.getLoyaltyPoints() : BigDecimal.ZERO;
                    cust.setLoyaltyPoints(current.subtract(BigDecimal.valueOf(toRedeem)));
                    customerRepository.save(cust);
                    LoyaltyTransaction redeem = new LoyaltyTransaction();
                    redeem.setCustomer(cust);
                    redeem.setType(LoyaltyTransaction.TransactionType.REDEEM);
                    redeem.setPoints(BigDecimal.valueOf(toRedeem));
                    redeem.setBilling(savedBilling);
                    redeem.setDescription("Redeemed on Bill " + savedBilling.getBillNumber());
                    loyaltyTransactionRepository.save(redeem);
                }
            }
        }

        // Mark gift voucher as redeemed if used
        if (billing.getVoucherCode() != null && !billing.getVoucherCode().trim().isEmpty() && voucherDiscount.compareTo(BigDecimal.ZERO) > 0) {
            giftVoucherService.redeemVoucher(billing.getVoucherCode().trim(), savedBilling);
        }

        return savedBilling;
    }

    public void sendBillViaEmail(Long billingId, String receiptType) {
        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new RuntimeException("Billing not found"));
        
        emailService.sendBillEmail(billing, receiptType != null ? receiptType.toUpperCase() : "NORMAL");
        billing.setEmailSent(true);
        billingRepository.save(billing);
    }

    public void sendBillViaWhatsApp(Long billingId, String receiptType) {
        Billing billing = billingRepository.findById(billingId)
                .orElseThrow(() -> new RuntimeException("Billing not found"));
        
        whatsAppService.sendBillWhatsApp(billing, receiptType != null ? receiptType.toUpperCase() : "NORMAL");
        billing.setWhatsappSent(true);
        billingRepository.save(billing);
    }

    /**
     * Update billing: payment details, notes, and optionally remove items.
     * When items are removed: stock is restored, totals recalculated.
     * When paid amount changes: updates credit remainingAmount and income transactions.
     */
    public Billing updateBill(Long id, Billing updates) {
        Billing billing = billingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Billing not found"));

        // Save snapshot of current state before any changes (for edit history)
        saveBillingSnapshot(billing);

        // Remove items if requested: restore stock and delete BillingItems
        List<Long> itemIdsToRemove = updates.getItemIdsToRemove();
        if (itemIdsToRemove != null && !itemIdsToRemove.isEmpty()) {
            if (billing.getItems().size() <= itemIdsToRemove.size()) {
                throw new IllegalArgumentException("Cannot remove all items. At least one item must remain.");
            }
            for (Long itemId : itemIdsToRemove) {
                BillingItem bi = billingItemRepository.findById(itemId)
                        .orElseThrow(() -> new IllegalArgumentException("Billing item not found: " + itemId));
                if (!bi.getBilling().getId().equals(id)) {
                    throw new IllegalArgumentException("Billing item " + itemId + " does not belong to this bill");
                }
                // Restore stock if item was from stock
                if (bi.getStock() != null && bi.getStock().getId() != null && bi.getQuantity() != null && bi.getQuantity() > 0) {
                    Stock stock = stockRepository.findById(bi.getStock().getId())
                            .orElseThrow(() -> new RuntimeException("Stock not found: " + bi.getStock().getId()));
                    int currentQty = stock.getQuantity() != null ? stock.getQuantity() : 0;
                    stock.setQuantity(currentQty + bi.getQuantity());
                    if (stock.getStatus() == Stock.StockStatus.SOLD) {
                        stock.setStatus(Stock.StockStatus.AVAILABLE);
                    }
                    stockRepository.save(stock);
                }
                billing.getItems().remove(bi);
                billingItemRepository.delete(bi); // delete after remove so totals exclude it
            }
            // Recalculate totals from remaining items
            BigDecimal newTotalAmount = billing.getItems().stream()
                    .filter(item -> item.getTotalPrice() != null)
                    .map(BillingItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal newTotalDiamond = billing.getItems().stream()
                    .filter(item -> item.getDiamondAmount() != null)
                    .map(BillingItem::getDiamondAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal discount = billing.getDiscountAmount() != null ? billing.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal making = billing.getMakingCharges() != null ? billing.getMakingCharges() : BigDecimal.ZERO;
            billing.setTotalAmount(newTotalAmount);
            billing.setTotalDiamondAmount(newTotalDiamond.compareTo(BigDecimal.ZERO) > 0 ? newTotalDiamond : null);
            billing.setFinalAmount(newTotalAmount.subtract(discount).add(making));
        }

        // Add new items if requested
        List<BillingItem> itemsToAdd = updates.getItemsToAdd();
        if (itemsToAdd != null && !itemsToAdd.isEmpty()) {
            for (BillingItem newItem : itemsToAdd) {
                newItem.setBilling(billing);
                if (newItem.getStock() != null && newItem.getStock().getId() != null) {
                    Stock s = stockRepository.findById(newItem.getStock().getId()).orElse(null);
                    if (s != null) {
                        if (newItem.getItemName() == null || newItem.getItemName().isEmpty()) {
                            newItem.setItemName(s.getArticleName());
                        }
                        newItem.setArticleCode(s.getArticleCode());
                        if (newItem.getWeightGrams() == null && s.getWeightGrams() != null) newItem.setWeightGrams(s.getWeightGrams());
                        if (newItem.getCarat() == null && s.getCarat() != null) newItem.setCarat(s.getCarat());
                        if (newItem.getDiamondCarat() == null && s.getDiamondCarat() != null) newItem.setDiamondCarat(s.getDiamondCarat());
                    }
                }
                billing.getItems().add(newItem);
                billingItemRepository.save(newItem);
            }
            // Validate stock for NEW items only (existing items already had stock deducted)
            Map<Long, Integer> stockIdToQtyRequested = new HashMap<>();
            for (BillingItem item : itemsToAdd) {
                if (item.getStock() != null && item.getStock().getId() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                    Long sid = item.getStock().getId();
                    stockIdToQtyRequested.merge(sid, item.getQuantity(), Integer::sum);
                }
            }
            for (Map.Entry<Long, Integer> e : stockIdToQtyRequested.entrySet()) {
                Stock stock = stockRepository.findById(e.getKey())
                        .orElseThrow(() -> new RuntimeException("Stock not found with id: " + e.getKey()));
                int available = stock.getQuantity() != null ? stock.getQuantity() : 0;
                if (e.getValue() > available) {
                    throw new IllegalArgumentException("Insufficient stock for " + stock.getArticleName() + " (" + stock.getArticleCode() + "): only " + available + " available, " + e.getValue() + " requested.");
                }
            }
            // Deduct stock for new items
            for (BillingItem item : itemsToAdd) {
                if (item.getStock() != null && item.getStock().getId() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                    Stock stock = stockRepository.findById(item.getStock().getId())
                            .orElseThrow(() -> new RuntimeException("Stock not found: " + item.getStock().getId()));
                    int currentQty = stock.getQuantity() != null ? stock.getQuantity() : 0;
                    int newQty = Math.max(0, currentQty - item.getQuantity());
                    stock.setQuantity(newQty);
                    if (newQty <= 0) stock.setStatus(Stock.StockStatus.SOLD);
                    stockRepository.save(stock);
                }
            }
            // Recalculate totals
            BigDecimal newTotalAmount = billing.getItems().stream()
                    .filter(item -> item.getTotalPrice() != null)
                    .map(BillingItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal newTotalDiamond = billing.getItems().stream()
                    .filter(item -> item.getDiamondAmount() != null)
                    .map(BillingItem::getDiamondAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal discount = billing.getDiscountAmount() != null ? billing.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal making = billing.getMakingCharges() != null ? billing.getMakingCharges() : BigDecimal.ZERO;
            billing.setTotalAmount(newTotalAmount);
            billing.setTotalDiamondAmount(newTotalDiamond.compareTo(BigDecimal.ZERO) > 0 ? newTotalDiamond : null);
            billing.setFinalAmount(newTotalAmount.subtract(discount).add(making));
        }

        BigDecimal finalAmount = billing.getFinalAmount() != null ? billing.getFinalAmount() : BigDecimal.ZERO;

        // Apply updates
        if (updates.getNotes() != null) {
            billing.setNotes(updates.getNotes());
        }
        if (updates.getDiscountAmount() != null) {
            billing.setDiscountAmount(updates.getDiscountAmount());
        }
        if (updates.getMakingCharges() != null) {
            billing.setMakingCharges(updates.getMakingCharges());
        }
        if (updates.getPaymentBreakdown() != null) {
            billing.setPaymentBreakdown(updates.getPaymentBreakdown());
        }
        if (updates.getPaymentMethod() != null) {
            billing.setPaymentMethod(updates.getPaymentMethod());
        }
        if (updates.getPaidAmount() != null) {
            billing.setPaidAmount(updates.getPaidAmount());
        }

        // Recompute paid amount from breakdown if provided
        String breakdown = billing.getPaymentBreakdown();
        if (breakdown != null && !breakdown.trim().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> payments = mapper.readValue(breakdown, new TypeReference<List<Map<String, Object>>>() {});
                BigDecimal computed = BigDecimal.ZERO;
                for (Map<String, Object> p : payments) {
                    Object amt = p.get("amount");
                    if (amt != null) {
                        if (amt instanceof Number) computed = computed.add(BigDecimal.valueOf(((Number) amt).doubleValue()));
                        else computed = computed.add(new BigDecimal(amt.toString()));
                    }
                }
                billing.setPaidAmount(computed.setScale(2, RoundingMode.HALF_UP));
                billing.setPaymentMethod(Billing.PaymentMethod.MIXED);
            } catch (Exception ignored) { }
        }

        BigDecimal newPaid = billing.getPaidAmount() != null ? billing.getPaidAmount() : BigDecimal.ZERO;

        // Recalculate finalAmount when discount or making changed
        BigDecimal totalAmt = billing.getTotalAmount() != null ? billing.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal discount = billing.getDiscountAmount() != null ? billing.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal making = billing.getMakingCharges() != null ? billing.getMakingCharges() : BigDecimal.ZERO;
        billing.setFinalAmount(totalAmt.subtract(discount).add(making));
        finalAmount = billing.getFinalAmount();

        // Recalculate payment status
        if (newPaid.compareTo(finalAmount) >= 0) {
            billing.setPaymentStatus(Billing.PaymentStatus.PAID);
        } else if (newPaid.compareTo(BigDecimal.ZERO) > 0) {
            billing.setPaymentStatus(Billing.PaymentStatus.PARTIAL);
        } else {
            billing.setPaymentStatus(Billing.PaymentStatus.PENDING);
        }

        Billing saved = billingRepository.save(billing);

        // Update credit entries for this billing
        List<Credit> credits = creditRepository.findByBilling_Id(id);
        BigDecimal remainingAmount = finalAmount.subtract(newPaid);
        for (Credit credit : credits) {
            credit.setRemainingAmount(remainingAmount);
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                credit.setStatus(Credit.CreditStatus.PAID);
                credit.setPaidAmount(credit.getCreditAmount());
            } else {
                credit.setStatus(credit.getPaidAmount() != null && credit.getPaidAmount().compareTo(BigDecimal.ZERO) > 0
                        ? Credit.CreditStatus.PARTIAL : Credit.CreditStatus.PENDING);
            }
            creditRepository.save(credit);
        }

        // Re-record income: delete old transactions for this billing, then record with new amounts
        transactionHistoryRepository.deleteByBilling_Id(id);
        if (newPaid.compareTo(BigDecimal.ZERO) > 0 && (saved.getPaymentStatus() == Billing.PaymentStatus.PAID || saved.getPaymentStatus() == Billing.PaymentStatus.PARTIAL)) {
            incomeExpenseService.recordIncomeFromBilling(saved);
        }

        return saved;
    }

    private String generateBillNumber() {
        String prefix = "BILL";
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long count = billingRepository.count() + 1;
        return String.format("%s-%s-%05d", prefix, date, count);
    }

    private void saveBillingSnapshot(Billing billing) {
        try {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("billNumber", billing.getBillNumber());
            snapshot.put("createdAt", billing.getCreatedAt() != null ? billing.getCreatedAt().toString() : null);
            if (billing.getCustomer() != null) {
                Map<String, Object> cust = new HashMap<>();
                cust.put("id", billing.getCustomer().getId());
                cust.put("name", billing.getCustomer().getName());
                cust.put("phone", billing.getCustomer().getPhone());
                snapshot.put("customer", cust);
            }
            snapshot.put("totalAmount", billing.getTotalAmount());
            snapshot.put("totalDiamondAmount", billing.getTotalDiamondAmount());
            snapshot.put("discountAmount", billing.getDiscountAmount());
            snapshot.put("makingCharges", billing.getMakingCharges());
            snapshot.put("finalAmount", billing.getFinalAmount());
            snapshot.put("paidAmount", billing.getPaidAmount());
            snapshot.put("paymentMethod", billing.getPaymentMethod() != null ? billing.getPaymentMethod().name() : null);
            snapshot.put("paymentBreakdown", billing.getPaymentBreakdown());
            snapshot.put("paymentStatus", billing.getPaymentStatus() != null ? billing.getPaymentStatus().name() : null);
            snapshot.put("notes", billing.getNotes());
            if (billing.getItems() != null) {
                List<Map<String, Object>> items = billing.getItems().stream()
                        .map(item -> {
                            Map<String, Object> m = new HashMap<>();
                            m.put("itemName", item.getItemName());
                            m.put("articleCode", item.getArticleCode());
                            m.put("quantity", item.getQuantity());
                            m.put("unitPrice", item.getUnitPrice());
                            m.put("totalPrice", item.getTotalPrice());
                            m.put("weightGrams", item.getWeightGrams());
                            m.put("carat", item.getCarat());
                            return m;
                        })
                        .collect(Collectors.toList());
                snapshot.put("items", items);
            }
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(snapshot);
            BillingEditHistory history = new BillingEditHistory();
            history.setBilling(billing);
            history.setSnapshotData(json);
            billingEditHistoryRepository.save(history);
        } catch (Exception e) {
            // Log but don't fail the update if snapshot fails
            // org.slf4j.Logger could be used
        }
    }

    public List<BillingEditHistory> getBillEditHistory(Long billingId) {
        return billingEditHistoryRepository.findByBillingIdOrderByCreatedAtDesc(billingId);
    }
}
