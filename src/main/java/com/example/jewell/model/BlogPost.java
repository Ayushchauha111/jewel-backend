package com.example.jewell.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "blog_posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BlogPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Core content
    private String title;
    
    @Column(unique = true)
    private String slug;
    
    @Column(columnDefinition = "TEXT")
    private String excerpt;
    
    @Column(columnDefinition = "LONGTEXT")
    private String content;
    
    // Author info
    private String authorName;
    private String authorBio;
    private String authorImage;
    private String authorSocial;
    
    // Media
    private String featuredImage;
    private String featuredImageAlt;
    private String thumbnailImage;
    
    // SEO Meta
    @Column(columnDefinition = "TEXT")
    private String metaDescription;
    private String metaTitle;
    
    @ElementCollection
    @CollectionTable(name = "blog_post_keywords", joinColumns = @JoinColumn(name = "blog_post_id"))
    @Column(name = "keyword")
    private List<String> keywords;
    
    // Open Graph / Social
    private String ogTitle;
    private String ogDescription;
    private String ogImage;
    private String twitterCard;
    
    // Schema.org structured data (hidden from public API)
    @Column(columnDefinition = "TEXT")
    @JsonIgnore
    private String schemaMarkup;
    
    // Organization
    private String category;
    
    @ElementCollection
    @CollectionTable(name = "blog_post_tags", joinColumns = @JoinColumn(name = "blog_post_id"))
    @Column(name = "tag")
    private List<String> tags;
    
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
    
    // Series (for multi-part posts)
    private String seriesName;
    private Integer seriesOrder;
    
    // Related posts
    @ElementCollection
    @CollectionTable(name = "blog_related_posts", joinColumns = @JoinColumn(name = "blog_post_id"))
    private List<RelatedPost> relatedPosts;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    
    // Canonical URL for SEO
    private String canonicalUrl;
    
    // Table of contents (auto-generated from headings)
    @Column(columnDefinition = "TEXT")
    @JsonIgnore
    private String tableOfContents;
    
    // User relation (optional for public/system posts)
    // Hidden from API responses to protect user data
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    @JsonIgnore
    private User user;
    
    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (viewCount == null) viewCount = 0L;
        if (likeCount == null) likeCount = 0L;
        if (shareCount == null) shareCount = 0L;
        if (featured == null) featured = false;
        if (published == null) published = false;
        if (trending == null) trending = false;
        updatedAt = LocalDateTime.now();
        
        // Calculate word count and read time
        if (content != null) {
            String plainText = content.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ");
            wordCount = plainText.split("\\s+").length;
            readTimeMinutes = Math.max(1, (int) Math.ceil(wordCount / 200.0));
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        
        // Recalculate word count and read time
        if (content != null) {
            String plainText = content.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ");
            wordCount = plainText.split("\\s+").length;
            readTimeMinutes = Math.max(1, (int) Math.ceil(wordCount / 200.0));
        }
    }
}

// RelatedPost Embeddable
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class RelatedPost {
    private String title;
    private String slug;
    private String thumbnail;
}
