package com.example.jewell.service;

import com.example.jewell.model.StockHistory;
import com.example.jewell.repository.StockHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockHistoryService {
    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    public List<StockHistory> getAllStockHistory() {
        return stockHistoryRepository.findByOrderByDeletedAtDesc();
    }

    public List<StockHistory> getStockHistoryByOriginalId(Long originalStockId) {
        return stockHistoryRepository.findByOriginalStockId(originalStockId);
    }
}
