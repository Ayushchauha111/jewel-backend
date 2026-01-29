package com.example.jewell.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.jewell.model.BlogPost;
import com.example.jewell.repository.BlogPostRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BlogPostService {

    @Autowired
    private BlogPostRepository blogPostRepository;

    // ========== PUBLIC ENDPOINTS ==========

    /**
     * Get all published posts
     */
    public List<BlogPost> getAllPublishedPosts() {
        return blogPostRepository.findByPublishedTrueOrderByCreatedAtDesc();
    }

    /**
     * Get paginated published posts
     */
    public Page<BlogPost> getPublishedPostsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return blogPostRepository.findByPublishedTrueOrderByCreatedAtDesc(pageable);
    }

    /**
     * Get post by slug (public)
     */
    @Transactional
    public Optional<BlogPost> getPublishedPostBySlug(String slug) {
        Optional<BlogPost> post = blogPostRepository.findBySlugAndPublishedTrue(slug);
        // Increment view count
        post.ifPresent(p -> blogPostRepository.incrementViewCount(p.getId()));
        return post;
    }

    /**
     * Get featured posts
     */
    public List<BlogPost> getFeaturedPosts() {
        return blogPostRepository.findByFeaturedTrueAndPublishedTrueOrderByPublishedAtDesc();
    }

    /**
     * Get trending posts
     */
    public List<BlogPost> getTrendingPosts() {
        return blogPostRepository.findByTrendingTrueAndPublishedTrueOrderByViewCountDesc();
    }

    /**
     * Get popular posts (top 10 by views)
     */
    public List<BlogPost> getPopularPosts() {
        return blogPostRepository.findTop10ByPublishedTrueOrderByViewCountDesc();
    }

    /**
     * Get recent posts
     */
    public List<BlogPost> getRecentPosts() {
        return blogPostRepository.findTop5ByPublishedTrueOrderByCreatedAtDesc();
    }

    /**
     * Get posts by category
     */
    public List<BlogPost> getPostsByCategory(String category) {
        return blogPostRepository.findByCategoryAndPublishedTrueOrderByCreatedAtDesc(category);
    }

    /**
     * Get posts by tag
     */
    public List<BlogPost> getPostsByTag(String tag) {
        return blogPostRepository.findByTagAndPublishedTrue(tag);
    }

    /**
     * Get posts in a series
     */
    public List<BlogPost> getSeriesPosts(String seriesName) {
        return blogPostRepository.findBySeriesNameAndPublishedTrueOrderBySeriesOrderAsc(seriesName);
    }

    /**
     * Search posts
     */
    public List<BlogPost> searchPosts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return blogPostRepository.searchPosts(query.trim());
    }

    /**
     * Get all categories
     */
    public List<String> getAllCategories() {
        return blogPostRepository.findAllCategories();
    }

    /**
     * Get all tags
     */
    public List<String> getAllTags() {
        return blogPostRepository.findAllTags();
    }

    /**
     * Get category counts
     */
    public Map<String, Long> getCategoryCounts() {
        return blogPostRepository.countByCategory().stream()
                .collect(Collectors.toMap(
                        arr -> (String) arr[0],
                        arr -> (Long) arr[1]
                ));
    }

    /**
     * Get related posts
     */
    public List<BlogPost> getRelatedPosts(Long postId, String category, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return blogPostRepository.findRelatedByCategory(category, postId, pageable);
    }

    /**
     * Like a post
     */
    @Transactional
    public void likePost(Long id) {
        blogPostRepository.incrementLikeCount(id);
    }

    /**
     * Share a post (track)
     */
    @Transactional
    public void sharePost(Long id) {
        blogPostRepository.incrementShareCount(id);
    }

    // ========== ADMIN ENDPOINTS ==========

    /**
     * Get all posts (including drafts) - ADMIN
     */
    public List<BlogPost> getAllPosts() {
        return blogPostRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Get paginated posts (including drafts) - ADMIN
     */
    public Page<BlogPost> getAllPostsPaginated(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        if (search != null && !search.trim().isEmpty()) {
            return blogPostRepository.searchPostsPaginated(search.trim(), pageable);
        }
        return blogPostRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * Get post by slug (admin - includes drafts)
     */
    public Optional<BlogPost> getPostBySlug(String slug) {
        return blogPostRepository.findBySlug(slug);
    }

    /**
     * Get post by ID - ADMIN
     */
    public Optional<BlogPost> getPostById(Long id) {
        return blogPostRepository.findById(id);
    }

    /**
     * Create new post - ADMIN
     */
    @Transactional
    public BlogPost createPost(BlogPost post) {
        // Generate slug if not provided
        if (post.getSlug() == null || post.getSlug().isEmpty()) {
            post.setSlug(generateSlug(post.getTitle()));
        }
        
        // Set published timestamp if publishing
        if (post.getPublished() != null && post.getPublished() && post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }
        
        // Generate table of contents from content
        post.setTableOfContents(generateTableOfContents(post.getContent()));
        
        // Generate schema markup
        post.setSchemaMarkup(generateSchemaMarkup(post));
        
        return blogPostRepository.save(post);
    }

    /**
     * Update post - ADMIN
     */
    @Transactional
    public BlogPost updatePost(Long id, BlogPost postDetails) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));
        
        // Update all fields
        if (postDetails.getTitle() != null) post.setTitle(postDetails.getTitle());
        if (postDetails.getSlug() != null) post.setSlug(postDetails.getSlug());
        if (postDetails.getExcerpt() != null) post.setExcerpt(postDetails.getExcerpt());
        if (postDetails.getContent() != null) post.setContent(postDetails.getContent());
        
        // Author
        if (postDetails.getAuthorName() != null) post.setAuthorName(postDetails.getAuthorName());
        if (postDetails.getAuthorBio() != null) post.setAuthorBio(postDetails.getAuthorBio());
        if (postDetails.getAuthorImage() != null) post.setAuthorImage(postDetails.getAuthorImage());
        if (postDetails.getAuthorSocial() != null) post.setAuthorSocial(postDetails.getAuthorSocial());
        
        // Media
        if (postDetails.getFeaturedImage() != null) post.setFeaturedImage(postDetails.getFeaturedImage());
        if (postDetails.getFeaturedImageAlt() != null) post.setFeaturedImageAlt(postDetails.getFeaturedImageAlt());
        if (postDetails.getThumbnailImage() != null) post.setThumbnailImage(postDetails.getThumbnailImage());
        
        // SEO
        if (postDetails.getMetaDescription() != null) post.setMetaDescription(postDetails.getMetaDescription());
        if (postDetails.getMetaTitle() != null) post.setMetaTitle(postDetails.getMetaTitle());
        if (postDetails.getKeywords() != null) post.setKeywords(postDetails.getKeywords());
        if (postDetails.getCanonicalUrl() != null) post.setCanonicalUrl(postDetails.getCanonicalUrl());
        
        // Open Graph
        if (postDetails.getOgTitle() != null) post.setOgTitle(postDetails.getOgTitle());
        if (postDetails.getOgDescription() != null) post.setOgDescription(postDetails.getOgDescription());
        if (postDetails.getOgImage() != null) post.setOgImage(postDetails.getOgImage());
        if (postDetails.getTwitterCard() != null) post.setTwitterCard(postDetails.getTwitterCard());
        
        // Organization
        if (postDetails.getCategory() != null) post.setCategory(postDetails.getCategory());
        if (postDetails.getTags() != null) post.setTags(postDetails.getTags());
        
        // Series
        if (postDetails.getSeriesName() != null) post.setSeriesName(postDetails.getSeriesName());
        if (postDetails.getSeriesOrder() != null) post.setSeriesOrder(postDetails.getSeriesOrder());
        
        // Flags
        if (postDetails.getFeatured() != null) post.setFeatured(postDetails.getFeatured());
        if (postDetails.getTrending() != null) post.setTrending(postDetails.getTrending());
        
        // Publishing
        if (postDetails.getPublished() != null) {
            boolean wasPublished = post.getPublished() != null && post.getPublished();
            post.setPublished(postDetails.getPublished());
            
            // Set published timestamp when first publishing
            if (postDetails.getPublished() && !wasPublished) {
                post.setPublishedAt(LocalDateTime.now());
            }
        }
        
        // Related posts
        if (postDetails.getRelatedPosts() != null) post.setRelatedPosts(postDetails.getRelatedPosts());
        
        // Regenerate table of contents
        post.setTableOfContents(generateTableOfContents(post.getContent()));
        
        // Regenerate schema markup
        post.setSchemaMarkup(generateSchemaMarkup(post));
        
        return blogPostRepository.save(post);
    }

    /**
     * Delete post - ADMIN
     */
    @Transactional
    public void deletePost(Long id) {
        blogPostRepository.deleteById(id);
    }

    /**
     * Toggle featured status - ADMIN
     */
    @Transactional
    public BlogPost toggleFeatured(Long id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setFeatured(!post.getFeatured());
        return blogPostRepository.save(post);
    }

    /**
     * Toggle published status - ADMIN
     */
    @Transactional
    public BlogPost togglePublished(Long id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        boolean newStatus = !post.getPublished();
        post.setPublished(newStatus);
        
        if (newStatus && post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }
        
        return blogPostRepository.save(post);
    }

    // ========== HELPER METHODS ==========

    private String generateSlug(String title) {
        if (title == null) return "";
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }

    private String generateTableOfContents(String content) {
        if (content == null) return "[]";
        
        // Simple regex to extract h2 and h3 headings
        StringBuilder toc = new StringBuilder("[");
        String[] lines = content.split("\n");
        boolean first = true;
        int index = 0;
        
        for (String line : lines) {
            String heading = null;
            int level = 0;
            
            if (line.matches(".*<h2[^>]*>.*</h2>.*")) {
                heading = line.replaceAll("<[^>]*>", "").trim();
                level = 2;
            } else if (line.matches(".*<h3[^>]*>.*</h3>.*")) {
                heading = line.replaceAll("<[^>]*>", "").trim();
                level = 3;
            }
            
            if (heading != null && !heading.isEmpty()) {
                if (!first) toc.append(",");
                String id = "heading-" + index++;
                toc.append(String.format("{\"id\":\"%s\",\"text\":\"%s\",\"level\":%d}", 
                        id, heading.replace("\"", "\\\""), level));
                first = false;
            }
        }
        
        toc.append("]");
        return toc.toString();
    }

    private String generateSchemaMarkup(BlogPost post) {
        String baseUrl = "https://typogram.in";
        
        return String.format("""
            {
                "@context": "https://schema.org",
                "@type": "BlogPosting",
                "mainEntityOfPage": {
                    "@type": "WebPage",
                    "@id": "%s/blog/%s"
                },
                "headline": "%s",
                "description": "%s",
                "image": "%s",
                "author": {
                    "@type": "Person",
                    "name": "%s"
                },
                "publisher": {
                    "@type": "Organization",
                    "name": "Typogram",
                    "logo": {
                        "@type": "ImageObject",
                        "url": "%s/logo.png"
                    }
                },
                "datePublished": "%s",
                "dateModified": "%s",
                "wordCount": %d,
                "timeRequired": "PT%dM"
            }
            """,
                baseUrl,
                post.getSlug() != null ? post.getSlug() : "",
                post.getTitle() != null ? post.getTitle().replace("\"", "\\\"") : "",
                post.getMetaDescription() != null ? post.getMetaDescription().replace("\"", "\\\"") : 
                    (post.getExcerpt() != null ? post.getExcerpt().replace("\"", "\\\"") : ""),
                post.getFeaturedImage() != null ? post.getFeaturedImage() : "",
                post.getAuthorName() != null ? post.getAuthorName() : "Typogram Team",
                baseUrl,
                post.getPublishedAt() != null ? post.getPublishedAt().toString() : LocalDateTime.now().toString(),
                post.getUpdatedAt() != null ? post.getUpdatedAt().toString() : LocalDateTime.now().toString(),
                post.getWordCount() != null ? post.getWordCount() : 0,
                post.getReadTimeMinutes() != null ? post.getReadTimeMinutes() : 1
        );
    }
}
