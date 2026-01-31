package com.example.jewell.service;

import com.example.jewell.model.Credit;
import com.example.jewell.model.DailyRate;
import com.example.jewell.model.GoldPrice;
import com.example.jewell.model.Order;
import com.example.jewell.model.SilverPrice;
import com.example.jewell.model.Stock;
import com.example.jewell.repository.BillingRepository;
import com.example.jewell.repository.CreditRepository;
import com.example.jewell.repository.CustomerRepository;
import com.example.jewell.repository.OrderRepository;
import com.example.jewell.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CreditRepository creditRepository;
    @Autowired
    private BillingRepository billingRepository;
    @Autowired
    private GoldPriceService goldPriceService;
    @Autowired
    private SilverPriceService silverPriceService;
    @Autowired
    private DailyRateService dailyRateService;

    /**
     * Returns all data needed for the admin dashboard in one call.
     */
    public Map<String, Object> getDashboardOverview() {
        Map<String, Object> result = new HashMap<>();

        long totalStock = stockRepository.count();
        long availableStock = stockRepository.countByStatus(Stock.StockStatus.AVAILABLE);
        long soldStock = stockRepository.countByStatus(Stock.StockStatus.SOLD);
        result.put("totalStock", totalStock);
        result.put("availableStock", availableStock);
        result.put("soldStock", soldStock);

        result.put("totalCustomers", customerRepository.count());
        result.put("totalOrders", orderRepository.count());

        long pendingCredits = creditRepository.countByStatus(Credit.CreditStatus.PENDING);
        long partialCredits = creditRepository.countByStatus(Credit.CreditStatus.PARTIAL);
        result.put("totalCredits", pendingCredits + partialCredits);

        BigDecimal billingRevenue = billingRepository.getTotalPaidRevenue() != null
                ? billingRepository.getTotalPaidRevenue() : BigDecimal.ZERO;
        BigDecimal orderRevenue = orderRepository.getTotalPaidRevenue() != null
                ? orderRepository.getTotalPaidRevenue() : BigDecimal.ZERO;
        result.put("totalRevenue", billingRevenue.add(orderRevenue));

        // Prefer unified daily rates, then fall back to legacy gold/silver tables
        java.util.Optional<DailyRate> dailyRate = dailyRateService.getTodayOrLatest();
        BigDecimal todayGold = dailyRate.flatMap(r -> dailyRateService.getGoldRateForCarat(java.math.BigDecimal.valueOf(22)))
                .or(() -> goldPriceService.getTodayGoldPrice().map(GoldPrice::getPrice22Carat))
                .or(() -> goldPriceService.getTodayGoldPrice().map(GoldPrice::getPricePerGram))
                .or(() -> goldPriceService.getLatestGoldPrice().map(GoldPrice::getPricePerGram))
                .orElse(null);
        result.put("todayGoldPrice", todayGold);
        BigDecimal todaySilver = dailyRate.flatMap(r -> java.util.Optional.ofNullable(r.getSilverPerGram()))
                .filter(v -> v.compareTo(java.math.BigDecimal.ZERO) > 0)
                .or(() -> silverPriceService.getTodaySilverPrice().map(SilverPrice::getPricePerGram))
                .or(() -> silverPriceService.getLatestSilverPrice().map(SilverPrice::getPricePerGram))
                .orElse(null);
        result.put("todaySilverPrice", todaySilver);
        result.put("todayDiamondPrice", dailyRate.map(DailyRate::getDiamondPerCarat).orElse(null));

        List<Order> recentOrders = orderRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        result.put("recentOrders", recentOrders);

        return result;
    }
}
