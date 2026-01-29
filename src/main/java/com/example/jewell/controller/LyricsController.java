package com.example.jewell.controller;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

@RestController
@RequestMapping("/api/lyrics")
public class LyricsController {
  private static final Logger log = LoggerFactory.getLogger(LyricsController.class);

   private final RestTemplate restTemplate = new RestTemplate();
  @GetMapping("/search")
  @SuppressWarnings({"rawtypes", "unchecked"})
public ResponseEntity<List<Map<String, String>>> searchSongs(@RequestParam String query) {
    String apiUrl = "https://saavn.dev/api/search/songs?query=" + UriUtils.encodePath(query, StandardCharsets.UTF_8);

    try {
        ResponseEntity<Map> response = restTemplate.getForEntity(apiUrl, Map.class);
        Map<String, Object> body = response.getBody();
        if (body == null || !body.containsKey("data")) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        Map<String, Object> data = (Map<String, Object>) body.get("data");
        List<Map<String, Object>> results = (List<Map<String, Object>>) data.get("results");

        List<Map<String, String>> songs = new ArrayList<>();

        for (Map<String, Object> song : results) {
            String name = (String) song.get("name");
            String songUrl = (String) song.get("url");

            // Safe check for downloadUrl
            List<Map<String, String>> downloadUrls = (List<Map<String, String>>) song.get("downloadUrl");
            String audioUrl = null;

            if (downloadUrls != null && !downloadUrls.isEmpty()) {
                // Prefer 320kbps if available
                for (Map<String, String> dl : downloadUrls) {
                    if ("320kbps".equals(dl.get("quality"))) {
                        audioUrl = dl.get("url");
                        break;
                    }
                }

                // Fallback to first
                if (audioUrl == null) {
                    audioUrl = downloadUrls.get(0).get("url");
                }
            }

            Map<String, String> songMap = new HashMap<>();
            songMap.put("name", name);
            songMap.put("url", songUrl);
            songMap.put("audioUrl", audioUrl != null ? audioUrl : "");

            songs.add(songMap);
        }

        return ResponseEntity.ok(songs);
    } catch (Exception e) {
        log.error("Error fetching lyrics", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body(Collections.emptyList());
    }
}

}