package com.example.jewell.dto;

import com.example.jewell.model.BlogPost;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for BlogPost - excludes sensitive user data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogPostDTO {
    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    private String content;
    private String category;
    private List<String> tags;
    private List<String> keywords;
    
    // Author info (public only)
    private String authorName;
    private String authorBio;
    private String authorImage;
    
    // Media
    private String featuredImage;
    private String featuredImageAlt;
    private String thumbnailImage;
    
    // SEO (public fields only)
    private String metaTitle;
    private String metaDescription;
    
    // Stats
    private Long viewCount;
    private Long likeCount;
    private Long shareCount;
    private Integer readTimeMinutes;
    private Integer wordCount;
    
    // Flags
    private Boolean featured;
    private Boolean published;
    private Boolean trending;
    
    // Series
    private String seriesName;
    private Integer seriesOrder;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    
    // Related posts (simplified)
    private List<RelatedPostDTO> relatedPosts;
    
    /**
     * Convert entity to DTO
     */
    public static BlogPostDTO fromEntity(BlogPost post) {
        if (post == null) return null;
        
        return BlogPostDTO.builder()
            .id(post.getId())
            .title(post.getTitle())
            .slug(post.getSlug())
            .excerpt(post.getExcerpt())
            .content(post.getContent())
            .category(post.getCategory())
            .tags(post.getTags())
            .keywords(post.getKeywords())
            .authorName(post.getAuthorName())
            .authorBio(post.getAuthorBio())
            .authorImage(post.getAuthorImage())
            .featuredImage(post.getFeaturedImage())
            .featuredImageAlt(post.getFeaturedImageAlt())
            .thumbnailImage(post.getThumbnailImage())
            .metaTitle(post.getMetaTitle())
            .metaDescription(post.getMetaDescription())
            .viewCount(post.getViewCount())
            .likeCount(post.getLikeCount())
            .shareCount(post.getShareCount())
            .readTimeMinutes(post.getReadTimeMinutes())
            .wordCount(post.getWordCount())
            .featured(post.getFeatured())
            .published(post.getPublished())
            .trending(post.getTrending())
            .seriesName(post.getSeriesName())
            .seriesOrder(post.getSeriesOrder())
            .createdAt(post.getCreatedAt())
            .publishedAt(post.getPublishedAt())
            .build();
    }
    
    /**
     * Convert list of entities to DTOs
     */
    public static List<BlogPostDTO> fromEntities(List<BlogPost> posts) {
        if (posts == null) return List.of();
        return posts.stream().map(BlogPostDTO::fromEntity).toList();
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RelatedPostDTO {
        private String title;
        private String slug;
        private String thumbnail;
    }
}

