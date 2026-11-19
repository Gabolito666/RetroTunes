package com.crud_relacional.com.controlador;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

@Component
public class SpotifyClientHelper {

    public record TrackDto(int numero, String nombre, String duracion, String previewUrl) {}

    public record AlbumDto(
            String id,
            String nombre,
            String artista,
            String fechaLanzamiento,
            String imagenUrl,
            List<TrackDto> tracks
    ) {}

    @Value("${spotify.client.id:}")
    private String clientId;

    @Value("${spotify.client.secret:}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private String tokenAcceso;
    private Instant tokenExpiracion = Instant.MIN;

    private synchronized String obtenerToken() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            return null;
        }

        if (tokenAcceso != null && Instant.now().isBefore(tokenExpiracion)) {
            return tokenAcceso;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(clientId, clientSecret);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity("https://accounts.spotify.com/api/token", request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                tokenAcceso = (String) response.getBody().get("access_token");
                Integer expiresIn = (Integer) response.getBody().get("expires_in");
                tokenExpiracion = Instant.now().plusSeconds(expiresIn != null ? expiresIn - 60 : 3500);
                return tokenAcceso;
            }
        } catch (Exception e) {
            // Silencioso: Fallback a catálogo curado offline
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public AlbumDto obtenerAlbum(String spotifyAlbumId, String tituloFallback) {
        if (spotifyAlbumId == null || spotifyAlbumId.isBlank()) {
            return fallbackAlbum(spotifyAlbumId, tituloFallback);
        }

        String token = obtenerToken();
        if (token != null) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                String url = "https://api.spotify.com/v1/albums/" + spotifyAlbumId;
                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> body = response.getBody();
                    String nombre = (String) body.get("name");
                    String fecha = (String) body.get("release_date");

                    String artista = "Varios Artistas";
                    List<Map<String, Object>> artistas = (List<Map<String, Object>>) body.get("artists");
                    if (artistas != null && !artistas.isEmpty()) {
                        artista = (String) artistas.get(0).get("name");
                    }

                    String imagen = null;
                    List<Map<String, Object>> imagenes = (List<Map<String, Object>>) body.get("images");
                    if (imagenes != null && !imagenes.isEmpty()) {
                        imagen = (String) imagenes.get(0).get("url");
                    }

                    List<TrackDto> tracks = new ArrayList<>();
                    Map<String, Object> tracksObj = (Map<String, Object>) body.get("tracks");
                    if (tracksObj != null) {
                        List<Map<String, Object>> items = (List<Map<String, Object>>) tracksObj.get("items");
                        if (items != null) {
                            for (Map<String, Object> it : items) {
                                int num = (int) it.getOrDefault("track_number", 1);
                                String tName = (String) it.get("name");
                                int durationMs = (int) it.getOrDefault("duration_ms", 0);
                                int min = durationMs / 60000;
                                int sec = (durationMs % 60000) / 1000;
                                String durationStr = String.format("%d:%02d", min, sec);
                                String preview = (String) it.get("preview_url");

                                tracks.add(new TrackDto(num, tName, durationStr, preview));
                            }
                        }
                    }

                    return new AlbumDto(spotifyAlbumId, nombre, artista, fecha, imagen, tracks);
                }
            } catch (Exception ignored) {
            }
        }

        return fallbackAlbum(spotifyAlbumId, tituloFallback);
    }

    private AlbumDto fallbackAlbum(String id, String titulo) {
        String nombre = titulo != null ? titulo : "Álbum Clásico Analógico";
        String artista = "Artista Curado";
        String fecha = "1973-03-01";
        String imagen = "https://images.unsplash.com/photo-1539185441755-769473a23570?auto=format&fit=crop&w=600&q=80";

        List<TrackDto> tracks = List.of(
                new TrackDto(1, "Lado A - Pista 01 (Muestra Análoga)", "3:45", "/audio/sample-preview.mp3"),
                new TrackDto(2, "Lado A - Pista 02 (Masterizado 180g)", "4:12", "/audio/sample-preview.mp3"),
                new TrackDto(3, "Lado B - Pista 03 (Acústico Direct Cut)", "3:20", "/audio/sample-preview.mp3")
        );

        return new AlbumDto(id != null ? id : "DEMO", nombre, artista, fecha, imagen, tracks);
    }
}
