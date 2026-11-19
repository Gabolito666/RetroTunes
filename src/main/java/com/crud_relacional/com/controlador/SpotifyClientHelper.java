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
    private final Map<String, String> cacheCaratulas = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, AlbumDto> cacheAlbumes = new java.util.concurrent.ConcurrentHashMap<>();

    public static String sanitizarAlbumId(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String trimmed = input.trim();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?:album[/:]|^)([a-zA-Z0-9]{22})(?:[/?&#]|$)").matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1);
        }
        if (trimmed.matches("^[a-zA-Z0-9]{20,24}$")) {
            return trimmed;
        }
        return trimmed;
    }

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

    public AlbumDto obtenerAlbum(String spotifyAlbumId, String tituloFallback) {
        return obtenerAlbum(spotifyAlbumId, tituloFallback, null, null);
    }

    public AlbumDto obtenerAlbum(String spotifyAlbumId, String tituloFallback, String formatoFallback) {
        return obtenerAlbum(spotifyAlbumId, tituloFallback, formatoFallback, null);
    }

    @SuppressWarnings("unchecked")
    public AlbumDto obtenerAlbum(String spotifyAlbumId, String tituloFallback, String formatoFallback, String imagenUrlFallback) {
        String cleanId = sanitizarAlbumId(spotifyAlbumId);
        if (cleanId == null || cleanId.isBlank()) {
            return fallbackAlbum(spotifyAlbumId, tituloFallback, formatoFallback, imagenUrlFallback);
        }

        if (cacheAlbumes.containsKey(cleanId)) {
            AlbumDto cached = cacheAlbumes.get(cleanId);
            if (cached.imagenUrl() != null && !cached.imagenUrl().isBlank()) {
                return cached;
            }
        }

        String token = obtenerToken();
        if (token != null) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                String url = "https://api.spotify.com/v1/albums/" + cleanId;
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

                    String imagen = extraerCaratulaDeJson(body);
                    if (imagen == null || imagen.isBlank()) {
                        imagen = imagenUrlFallback;
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

                    AlbumDto dto = new AlbumDto(cleanId, nombre, artista, fecha, imagen, tracks);
                    cacheAlbumes.put(cleanId, dto);
                    if (imagen != null && !imagen.isBlank()) {
                        cacheCaratulas.put(cleanId, imagen);
                    }
                    return dto;
                }
            } catch (Exception ignored) {
            }
        }

        // Fallback dinámico vía Spotify oEmbed oficial (público, sin cuota de credenciales OAuth)
        String oembedCover = null;
        String oembedTitle = null;
        try {
            String oembedUrl = "https://open.spotify.com/oembed?url=https://open.spotify.com/album/" + cleanId;
            ResponseEntity<Map> response = restTemplate.getForEntity(oembedUrl, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object thumb = response.getBody().get("thumbnail_url");
                if (thumb != null && !thumb.toString().isBlank()) {
                    oembedCover = thumb.toString().replace("/1e02", "/b273");
                    cacheCaratulas.put(cleanId, oembedCover);
                }
                Object titleObj = response.getBody().get("title");
                if (titleObj != null && !titleObj.toString().isBlank()) {
                    oembedTitle = titleObj.toString();
                }
            }
        } catch (Exception ignored) {
        }

        String imgFinal = oembedCover != null ? oembedCover : imagenUrlFallback;
        String tituloFinal = oembedTitle != null ? oembedTitle : tituloFallback;
        AlbumDto fallback = fallbackAlbum(cleanId, tituloFinal, formatoFallback, imgFinal);
        cacheAlbumes.put(cleanId, fallback);
        return fallback;
    }

    public static String extraerArtistaDeTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            return "Artista RetroTunes";
        }
        if (titulo.contains(" - ")) {
            return titulo.split(" - ")[0].trim();
        }
        return titulo.trim();
    }

    @SuppressWarnings("unchecked")
    public static String extraerCaratulaDeJson(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        List<Map<String, Object>> imagenes = (List<Map<String, Object>>) body.get("images");
        if (imagenes != null && !imagenes.isEmpty()) {
            // images[0].url para alta resolución (640x640) o images[1].url para resolución media (300x300)
            Object url = imagenes.get(0).get("url");
            if (url != null && !url.toString().isBlank()) {
                return url.toString();
            }
            if (imagenes.size() > 1) {
                Object urlMed = imagenes.get(1).get("url");
                if (urlMed != null && !urlMed.toString().isBlank()) {
                    return urlMed.toString();
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public String obtenerCaratula(String spotifyAlbumId) {
        String cleanId = sanitizarAlbumId(spotifyAlbumId);
        if (cleanId == null || cleanId.isBlank()) {
            return null;
        }

        if (cacheCaratulas.containsKey(cleanId)) {
            return cacheCaratulas.get(cleanId);
        }

        String token = obtenerToken();
        if (token != null) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);
                HttpEntity<Void> entity = new HttpEntity<>(headers);
                String url = "https://api.spotify.com/v1/albums/" + cleanId;
                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    String img = extraerCaratulaDeJson(response.getBody());
                    if (img != null && !img.isBlank()) {
                        cacheCaratulas.put(cleanId, img);
                        return img;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Fallback a Spotify oEmbed API (Sin cuota de credenciales OAuth)
        try {
            String oembedUrl = "https://open.spotify.com/oembed?url=https://open.spotify.com/album/" + cleanId;
            ResponseEntity<Map> response = restTemplate.getForEntity(oembedUrl, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object thumb = response.getBody().get("thumbnail_url");
                if (thumb != null && !thumb.toString().isBlank()) {
                    String highRes = thumb.toString().replace("/1e02", "/b273");
                    cacheCaratulas.put(cleanId, highRes);
                    return highRes;
                }
            }
        } catch (Exception ignored) {
        }

        // Fallback a mapa estático oficial si existe
        if (CARATULAS_SPOTIFY_OFICIALES.containsKey(cleanId)) {
            String estatica = CARATULAS_SPOTIFY_OFICIALES.get(cleanId);
            cacheCaratulas.put(cleanId, estatica);
            return estatica;
        }

        return null;
    }

    private AlbumDto fallbackAlbum(String id, String titulo, String formato) {
        return fallbackAlbum(id, titulo, formato, null);
    }

    private static final Map<String, String> CARATULAS_SPOTIFY_OFICIALES = Map.ofEntries(
            Map.entry("1pb3je8gXTs5dpRRTKhHRC", "https://i.scdn.co/image/ab67616d0000b27365a472d8326d4571bf725422"), // Noah Kahan - Stick Season (We'll All Be Here Forever 2LP)
            Map.entry("50ZenUP4O2Q5eCy2NRNvuz", "https://i.scdn.co/image/ab67616d0000b2736ee35072df1af802cca09918"), // Noah Kahan - Stick Season Standard
            Map.entry("2iMF2NlOZMfBTdHyubrg6y", "https://i.scdn.co/image/ab67616d0000b27341077fc4e87747e45ce28489"), // Hombres G
            Map.entry("56bdWeO40o3WfAD2Lja4dl", "https://i.scdn.co/image/ab67616d0000b27344aa161eb3a77a94ddf234bf"), // Gracie Abrams
            Map.entry("4uG8q3GPuGHQlGeSpRJbtv", "https://i.scdn.co/image/ab67616d0000b273c51cf94380eb9a3eeb1168f6"), // Green Day
            Map.entry("0bUTHLuuETwgVsc9L7xR3t", "https://i.scdn.co/image/ab67616d0000b273397e5944122d2509176375bc"), // Gorillaz
            Map.entry("0k2JGzYRZQC3s3ZNTcyHoA", "https://i.scdn.co/image/ab67616d0000b273cf32525164bc7dcae7da104e"), // Soda Stereo
            Map.entry("33VjH4cujQd33gXl0vRz9B", "https://i.scdn.co/image/ab67616d0000b27397b973352feee07937d5c58a"), // Enanitos Verdes
            Map.entry("0ETFjACtuP2ADo6LFhL6HN", "https://i.scdn.co/image/ab67616d0000b273dc30583ba717007b00cceb25"), // The Beatles
            Map.entry("4LH4d3cOWNNXdJwFd4G1kc", "https://i.scdn.co/image/ab67616d0000b273ea7caaff71dea1051d49b2fe"), // Pink Floyd
            Map.entry("1GbtB4K178q79T9vAgj2HN", "https://i.scdn.co/image/ab67616d0000b273e319baafd16e84f0408af2a0")  // Queen
    );

    private AlbumDto fallbackAlbum(String id, String titulo, String formato, String imagenUrlFallback) {
        String nombre = titulo != null ? titulo : "Álbum Clásico Analógico";
        String artista = extraerArtistaDeTitulo(titulo);
        String fecha = "1973-03-01";
        
        // Prohibición absoluta de imágenes de stock (Unsplash). Solo carátulas oficiales de Spotify.
        String imagen = (imagenUrlFallback != null && !imagenUrlFallback.isBlank() && !imagenUrlFallback.contains("unsplash")) 
                ? imagenUrlFallback 
                : null;
        
        if (imagen == null && id != null && CARATULAS_SPOTIFY_OFICIALES.containsKey(id)) {
            imagen = CARATULAS_SPOTIFY_OFICIALES.get(id);
        }

        // Remoción total de muestras de audio custom en favor exclusivo de Spotify Embed
        List<TrackDto> tracks = List.of();

        return new AlbumDto(id != null ? id : "DEMO", nombre, artista, fecha, imagen, tracks);
    }
}
