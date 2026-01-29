package com.example.jewell.repository;

import com.example.jewell.model.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByArticleCode(String articleCode);
    List<Stock> findByStatus(Stock.StockStatus status);
    List<Stock> findByArticleNameContainingIgnoreCase(String name);
    List<Stock> findByCategory(String category);
    List<Stock> findByCategoryAndStatus(String category, Stock.StockStatus status);
    long countByStatus(Stock.StockStatus status);

    // Search methods
    @Query("SELECT s FROM Stock s WHERE " +
           "(LOWER(s.articleName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.articleCode) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "(s.category IS NOT NULL AND LOWER(s.category) LIKE LOWER(CONCAT('%', :query, '%'))))")
    List<Stock> searchStock(@Param("query") String query);
    
    // Pagination methods
    Page<Stock> findAll(Pageable pageable);
    Page<Stock> findByStatus(Stock.StockStatus status, Pageable pageable);
    Page<Stock> findByCategory(String category, Pageable pageable);
    Page<Stock> findByMaterial(String material, Pageable pageable);
    Page<Stock> findByCategoryAndMaterial(String category, String material, Pageable pageable);
    Page<Stock> findByCategoryAndStatus(String category, Stock.StockStatus status, Pageable pageable);
    
    @Query("SELECT s FROM Stock s WHERE " +
           "(LOWER(s.articleName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.articleCode) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "(s.category IS NOT NULL AND LOWER(s.category) LIKE LOWER(CONCAT('%', :query, '%'))))")
    Page<Stock> searchStock(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT DISTINCT s.category FROM Stock s WHERE s.category IS NOT NULL ORDER BY s.category")
    List<String> findAllDistinctCategories();
    
    @Query("SELECT DISTINCT s.articleName FROM Stock s WHERE s.articleName IS NOT NULL ORDER BY s.articleName")
    List<String> findAllDistinctArticleNames();
    
    @Query("SELECT COUNT(s) FROM Stock s WHERE s.status = 'SOLD' AND DATE(s.updatedAt) = :date")
    Long getSoldStockCountByDate(@Param("date") LocalDate date);
    
    @Query("SELECT s FROM Stock s WHERE s.status = 'SOLD' AND DATE(s.updatedAt) = :date")
    List<Stock> getSoldStockByDate(@Param("date") LocalDate date);
}
