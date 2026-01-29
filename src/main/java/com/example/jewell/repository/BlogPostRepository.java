package com.example.jewell.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.jewell.model.BlogPost;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    
    // Find by slug (primary lookup)
    Optional<BlogPost> findBySlugAndPublishedTrue(String slug);
    Optional<BlogPost> findBySlug(String slug);
    
    // Category and tag filters
    List<BlogPost> findByCategoryAndPublishedTrueOrderByCreatedAtDesc(String category);
    
    @Query("SELECT b FROM BlogPost b JOIN b.tags t WHERE t = :tag AND b.published = true ORDER BY b.createdAt DESC")
    List<BlogPost> findByTagAndPublishedTrue(@Param("tag") String tag);
    
    // Featured and trending
    List<BlogPost> findByFeaturedTrueAndPublishedTrueOrderByPublishedAtDesc();
    List<BlogPost> findByTrendingTrueAndPublishedTrueOrderByViewCountDesc();
    
    // All published posts
    List<BlogPost> findByPublishedTrueOrderByCreatedAtDesc();
    Page<BlogPost> findByPublishedTrueOrderByCreatedAtDesc(Pageable pageable);
    
    // Popular posts by views
    List<BlogPost> findTop10ByPublishedTrueOrderByViewCountDesc();
    
    // Recent posts
    List<BlogPost> findTop5ByPublishedTrueOrderByCreatedAtDesc();
    
    // Search functionality
    @Query("SELECT b FROM BlogPost b WHERE b.published = true AND " +
           "(LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.content) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.excerpt) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY b.createdAt DESC")
    List<BlogPost> searchPosts(@Param("query") String query);
    
    // Series posts
    List<BlogPost> findBySeriesNameAndPublishedTrueOrderBySeriesOrderAsc(String seriesName);
    
    // Get all unique categories
    @Query("SELECT DISTINCT b.category FROM BlogPost b WHERE b.category IS NOT NULL AND b.published = true")
    List<String> findAllCategories();
    
    // Get all unique tags
    @Query("SELECT DISTINCT t FROM BlogPost b JOIN b.tags t WHERE b.published = true")
    List<String> findAllTags();
    
    // Increment view count
    @Modifying
    @Query("UPDATE BlogPost b SET b.viewCount = b.viewCount + 1 WHERE b.id = :id")
    void incrementViewCount(@Param("id") Long id);
    
    // Increment like count
    @Modifying
    @Query("UPDATE BlogPost b SET b.likeCount = b.likeCount + 1 WHERE b.id = :id")
    void incrementLikeCount(@Param("id") Long id);
    
    // Increment share count
    @Modifying
    @Query("UPDATE BlogPost b SET b.shareCount = b.shareCount + 1 WHERE b.id = :id")
    void incrementShareCount(@Param("id") Long id);
    
    // Related posts by category (exclude current post)
    @Query("SELECT b FROM BlogPost b WHERE b.category = :category AND b.id != :excludeId AND b.published = true ORDER BY b.createdAt DESC")
    List<BlogPost> findRelatedByCategory(@Param("category") String category, @Param("excludeId") Long excludeId, Pageable pageable);
    
    // Admin: all posts including drafts
    List<BlogPost> findAllByOrderByCreatedAtDesc();
    Page<BlogPost> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // Admin: paginated search (includes drafts)
    @Query("SELECT b FROM BlogPost b WHERE " +
           "(LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.content) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.excerpt) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY b.createdAt DESC")
    Page<BlogPost> searchPostsPaginated(@Param("query") String query, Pageable pageable);
    
    // Count by category
    @Query("SELECT b.category, COUNT(b) FROM BlogPost b WHERE b.published = true GROUP BY b.category")
    List<Object[]> countByCategory();
    
    // Count published posts
    long countByPublishedTrue();
    
    // Sum view count for published posts
    @Query("SELECT COALESCE(SUM(b.viewCount), 0) FROM BlogPost b WHERE b.published = true")
    Long sumViewCountByPublishedTrue();
    
    // Count distinct categories for published posts
    @Query("SELECT COUNT(DISTINCT b.category) FROM BlogPost b WHERE b.published = true AND b.category IS NOT NULL")
    Long countDistinctCategoriesByPublishedTrue();
    
    // By category (original method for backwards compatibility)
    List<BlogPost> findByCategory(String category);
    
    // By keywords (original method for backwards compatibility)
    List<BlogPost> findByKeywordsContaining(String keyword);
}
