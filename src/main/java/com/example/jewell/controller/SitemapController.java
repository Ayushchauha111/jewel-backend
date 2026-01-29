package com.example.jewell.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.jewell.model.BlogPost;
import com.example.jewell.repository.BlogPostRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/sitemap")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SitemapController {

    @Autowired
    private BlogPostRepository blogPostRepository;

    private static final String BASE_URL = "https://typogram.in";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'+00:00'");

    @GetMapping(value = "/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String generateSitemap() {
        StringBuilder sitemap = new StringBuilder();
        sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sitemap.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Static pages - Only public, indexable pages
        addUrl(sitemap, BASE_URL + "/", "1.0", "daily");
        // Note: /home redirects to /, so not included to avoid redirect issue
        addUrl(sitemap, BASE_URL + "/courses", "0.9", "weekly");
        addUrl(sitemap, BASE_URL + "/typing-display", "0.9", "weekly");
        addUrl(sitemap, BASE_URL + "/start-test", "0.8", "weekly");
        addUrl(sitemap, BASE_URL + "/blog", "0.95", "daily");
        addUrl(sitemap, BASE_URL + "/games", "0.8", "weekly");
        addUrl(sitemap, BASE_URL + "/games/word-tetris", "0.7", "monthly");
        addUrl(sitemap, BASE_URL + "/games/zombie-defense", "0.7", "monthly");
        addUrl(sitemap, BASE_URL + "/games/space-invaders", "0.7", "monthly");
        addUrl(sitemap, BASE_URL + "/games/word-blaster", "0.7", "monthly");
        addUrl(sitemap, BASE_URL + "/daily-challenge", "0.8", "daily");
        addUrl(sitemap, BASE_URL + "/race", "0.8", "weekly");
        addUrl(sitemap, BASE_URL + "/tournaments", "0.8", "weekly");
        addUrl(sitemap, BASE_URL + "/friends", "0.7", "weekly");
        addUrl(sitemap, BASE_URL + "/community", "0.8", "weekly");
        addUrl(sitemap, BASE_URL + "/leaderboard", "0.7", "daily");
        addUrl(sitemap, BASE_URL + "/about", "0.8", "monthly");
        addUrl(sitemap, BASE_URL + "/contact-us", "0.8", "monthly");
        addUrl(sitemap, BASE_URL + "/privacy-policy", "0.8", "monthly");
        // Note: /register and /login are excluded as they're authentication pages
        
        // Excluded pages (user-specific, admin, or internal):
        // /add, /active-courses, /profile, /user-result, /typing-display-test, /admin, /social

        // Blog posts - Include ALL published blog posts
        List<BlogPost> publishedPosts = blogPostRepository.findByPublishedTrueOrderByCreatedAtDesc();
        for (BlogPost post : publishedPosts) {
            // Only include posts with valid slugs
            if (post.getSlug() != null && !post.getSlug().trim().isEmpty()) {
                String lastmod = post.getUpdatedAt() != null 
                    ? post.getUpdatedAt().format(DATE_FORMATTER)
                    : (post.getPublishedAt() != null 
                        ? post.getPublishedAt().format(DATE_FORMATTER)
                        : LocalDateTime.now().format(DATE_FORMATTER));
                // Use canonical URL if available, otherwise use slug-based URL
                String blogUrl = post.getCanonicalUrl() != null && !post.getCanonicalUrl().trim().isEmpty()
                    ? post.getCanonicalUrl()
                    : BASE_URL + "/blog/" + post.getSlug();
                addUrl(sitemap, blogUrl, "0.9", "weekly", lastmod);
            }
        }

        // Jewelry shop pages
        addUrl(sitemap, BASE_URL + "/products", "0.9", "daily");

        sitemap.append("</urlset>");
        return sitemap.toString();
    }

    private void addUrl(StringBuilder sitemap, String loc, String priority, String changefreq) {
        addUrl(sitemap, loc, priority, changefreq, null);
    }

    private void addUrl(StringBuilder sitemap, String loc, String priority, String changefreq, String lastmod) {
        sitemap.append("  <url>\n");
        sitemap.append("    <loc>").append(loc).append("</loc>\n");
        if (lastmod != null) {
            sitemap.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        } else {
            sitemap.append("    <lastmod>").append(LocalDateTime.now().format(DATE_FORMATTER)).append("</lastmod>\n");
        }
        sitemap.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        sitemap.append("    <priority>").append(priority).append("</priority>\n");
        sitemap.append("  </url>\n");
    }
}

