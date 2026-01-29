package com.example.jewell.controller;

import com.example.jewell.exception.FeatureDisabledException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.jewell.model.BlogPost;
import com.example.jewell.service.BlogPostService;
import com.example.jewell.service.FeatureFlagService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/blog")
@CrossOrigin(origins = "*", maxAge = 3600)
public class BlogController {
    private static final Logger log = LoggerFactory.getLogger(BlogController.class);

    @Autowired
    private BlogPostService blogService;

    @Autowired
    private com.example.jewell.repository.BlogPostRepository blogPostRepository;

    @Autowired
    private FeatureFlagService featureFlagService;

    private void checkBlogSystemEnabled() {
        if (!featureFlagService.isFeatureEnabled("blog_system")) {
            throw new FeatureDisabledException("Blog system feature is currently disabled");
        }
    }

    // ========== PUBLIC ENDPOINTS ==========

    /**
     * Get paginated published blog posts (default endpoint)
     */
    @GetMapping
    public ResponseEntity<?> getAllPublishedPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            checkBlogSystemEnabled();
            org.springframework.data.domain.Page<BlogPost> postsPage = blogService.getPublishedPostsPaginated(page, size);
            return ResponseEntity.ok(postsPage);
        } catch (FeatureDisabledException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching paginated posts", e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    /**
     * Get paginated published posts (explicit endpoint)
     */
    @GetMapping("/paginated")
    public ResponseEntity<Page<BlogPost>> getPublishedPostsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(blogService.getPublishedPostsPaginated(page, size));
    }

    /**
     * Get a single post by slug (public view)
     */
    @GetMapping("/{slug}")
    public ResponseEntity<BlogPost> getPostBySlug(@PathVariable String slug) {
        return blogService.getPublishedPostBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get featured posts
     */
    @GetMapping("/featured")
    public ResponseEntity<List<BlogPost>> getFeaturedPosts() {
        return ResponseEntity.ok(blogService.getFeaturedPosts());
    }

    /**
     * Get trending posts
     */
    @GetMapping("/trending")
    public ResponseEntity<List<BlogPost>> getTrendingPosts() {
        return ResponseEntity.ok(blogService.getTrendingPosts());
    }

    /**
     * Get popular posts (top 10 by views)
     */
    @GetMapping("/popular")
    public ResponseEntity<List<BlogPost>> getPopularPosts() {
        return ResponseEntity.ok(blogService.getPopularPosts());
    }

    /**
     * Get recent posts
     */
    @GetMapping("/recent")
    public ResponseEntity<List<BlogPost>> getRecentPosts() {
        return ResponseEntity.ok(blogService.getRecentPosts());
    }

    /**
     * Get posts by category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<BlogPost>> getPostsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(blogService.getPostsByCategory(category));
    }

    /**
     * Get posts by tag
     */
    @GetMapping("/tag/{tag}")
    public ResponseEntity<List<BlogPost>> getPostsByTag(@PathVariable String tag) {
        return ResponseEntity.ok(blogService.getPostsByTag(tag));
    }

    /**
     * Get posts in a series
     */
    @GetMapping("/series/{seriesName}")
    public ResponseEntity<List<BlogPost>> getSeriesPosts(@PathVariable String seriesName) {
        return ResponseEntity.ok(blogService.getSeriesPosts(seriesName));
    }

    /**
     * Search posts
     */
    @GetMapping("/search")
    public ResponseEntity<List<BlogPost>> searchPosts(@RequestParam String q) {
        // Sanitize search query to prevent injection attacks
        if (q == null || q.length() > 100) {
            return ResponseEntity.badRequest().build();
        }
        String sanitizedQuery = q.replaceAll("[<>\"']", "").trim();
        if (sanitizedQuery.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(blogService.searchPosts(sanitizedQuery));
    }

    /**
     * Get all categories
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(blogService.getAllCategories());
    }

    /**
     * Get all tags
     */
    @GetMapping("/tags")
    public ResponseEntity<List<String>> getAllTags() {
        return ResponseEntity.ok(blogService.getAllTags());
    }

    /**
     * Get category counts
     */
    @GetMapping("/categories/counts")
    public ResponseEntity<Map<String, Long>> getCategoryCounts() {
        return ResponseEntity.ok(blogService.getCategoryCounts());
    }

    /**
     * Get blog statistics (total posts, total views, total categories, average views)
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getBlogStats() {
        try {
            long totalPosts = blogPostRepository.countByPublishedTrue();
            Long totalViewsObj = blogPostRepository.sumViewCountByPublishedTrue();
            Long totalCategoriesObj = blogPostRepository.countDistinctCategoriesByPublishedTrue();
            
            long totalViews = totalViewsObj != null ? totalViewsObj : 0L;
            long totalCategories = totalCategoriesObj != null ? totalCategoriesObj : 0;
            
            // Calculate average views per post for more meaningful metric
            long averageViews = totalPosts > 0 ? totalViews / totalPosts : 0;
            
            Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("totalPosts", totalPosts);
            stats.put("totalViews", totalViews);
            stats.put("totalCategories", totalCategories);
            stats.put("averageViews", averageViews);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching blog stats", e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get related posts for a given post
     */
    @GetMapping("/{id}/related")
    public ResponseEntity<List<BlogPost>> getRelatedPosts(
            @PathVariable Long id,
            @RequestParam String category,
            @RequestParam(defaultValue = "3") int limit) {
        return ResponseEntity.ok(blogService.getRelatedPosts(id, category, limit));
    }

    /**
     * Like a post
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likePost(@PathVariable Long id) {
        blogService.likePost(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Track share of a post
     */
    @PostMapping("/{id}/share")
    public ResponseEntity<Void> sharePost(@PathVariable Long id) {
        blogService.sharePost(id);
        return ResponseEntity.ok().build();
    }

    // ========== ADMIN ENDPOINTS ==========

    /**
     * Get all posts including drafts - ADMIN (DEPRECATED: Use paginated endpoint)
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BlogPost>> getAllPosts() {
        return ResponseEntity.ok(blogService.getAllPosts());
    }

    /**
     * Get paginated posts including drafts - ADMIN
     */
    @GetMapping("/admin/paginated")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<BlogPost>> getAllPostsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(blogService.getAllPostsPaginated(page, size, search));
    }

    /**
     * Get post by ID - ADMIN
     */
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BlogPost> getPostById(@PathVariable Long id) {
        return blogService.getPostById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new post - ADMIN
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BlogPost> createPost(@Valid @RequestBody BlogPost post) {
        // Sanitize content to prevent XSS
        if (post.getContent() != null) {
            post.setContent(sanitizeHtml(post.getContent()));
        }
        if (post.getExcerpt() != null) {
            post.setExcerpt(sanitizeHtml(post.getExcerpt()));
        }
        return ResponseEntity.ok(blogService.createPost(post));
    }

    /**
     * Update post - ADMIN
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BlogPost> updatePost(@PathVariable Long id, @Valid @RequestBody BlogPost post) {
        // Sanitize content to prevent XSS
        if (post.getContent() != null) {
            post.setContent(sanitizeHtml(post.getContent()));
        }
        if (post.getExcerpt() != null) {
            post.setExcerpt(sanitizeHtml(post.getExcerpt()));
        }
        return ResponseEntity.ok(blogService.updatePost(id, post));
    }
    
    /**
     * Basic HTML sanitization to prevent XSS attacks
     */
    private String sanitizeHtml(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        // Remove script tags and event handlers
        return html.replaceAll("(?i)<script[^>]*>.*?</script>", "")
                   .replaceAll("(?i)on\\w+\\s*=\\s*[\"'][^\"']*[\"']", "")
                   .replaceAll("(?i)javascript:", "")
                   .replaceAll("(?i)vbscript:", "");
    }

    /**
     * Delete post - ADMIN
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        blogService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggle featured status - ADMIN
     */
    @PostMapping("/{id}/toggle-featured")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BlogPost> toggleFeatured(@PathVariable Long id) {
        return ResponseEntity.ok(blogService.toggleFeatured(id));
    }

    /**
     * Toggle published status - ADMIN
     */
    @PostMapping("/{id}/toggle-published")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BlogPost> togglePublished(@PathVariable Long id) {
        return ResponseEntity.ok(blogService.togglePublished(id));
    }
}
