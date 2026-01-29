package com.example.jewell.service;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

@Service
public class LyricsService {

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> getLyricsAndAudio(String song, String artist) {
        String query = UriUtils.encodePath(song + " " + artist, StandardCharsets.UTF_8);
        String searchUrl = "https://saavn.dev/api/search/songs?query=" + query;

        ResponseEntity<Map> searchResponse = restTemplate.getForEntity(searchUrl, Map.class);
        List<Map<String, Object>> songs = (List<Map<String, Object>>) ((Map) searchResponse.getBody().get("data")).get("results");

        if (songs == null || songs.isEmpty()) return null;

        Map<String, Object> firstSong = songs.get(0);
        String lyrics = (String) firstSong.get("lyrics");
        String audioUrl = (String) firstSong.get("downloadUrl");

        Map<String, Object> result = new HashMap<>();
        result.put("lyrics", lyrics);
        result.put("audioUrl", audioUrl);
        result.put("title", firstSong.get("title"));
        result.put("artist", firstSong.get("primaryArtists"));

        return result;
    }
}