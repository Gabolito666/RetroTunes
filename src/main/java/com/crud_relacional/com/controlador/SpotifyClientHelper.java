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
    private final Map<String, String> cacheFechasLanzamiento = new java.util.concurrent.ConcurrentHashMap<>();
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

    public static boolean esPinkFloyd(String titulo) {
        if (titulo == null) return false;
        String t = titulo.toLowerCase();
        return t.contains("pink floyd") || t.contains("dark side");
    }

    public static String extraerFechaDeHtmlSpotify(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        // 1. Meta tag Open Music: <meta name="music:release_date" content="YYYY-MM-DD"> o property="music:release_date"
        java.util.regex.Matcher m1 = java.util.regex.Pattern.compile(
                "<meta\\s+(?:name|property)=[\"']music:release_date[\"']\\s+content=[\"']([0-9]{4}(?:-[0-9]{2}-[0-9]{2})?)[\"']",
                java.util.regex.Pattern.CASE_INSENSITIVE
        ).matcher(html);
        if (m1.find()) {
            return m1.group(1);
        }

        // 2. Variación de orden de atributos: content="..." name="music:release_date"
        java.util.regex.Matcher m2 = java.util.regex.Pattern.compile(
                "content=[\"']([0-9]{4}(?:-[0-9]{2}-[0-9]{2})?)[\"']\\s+(?:name|property)=[\"']music:release_date[\"']",
                java.util.regex.Pattern.CASE_INSENSITIVE
        ).matcher(html);
        if (m2.find()) {
            return m2.group(1);
        }

        // 3. Schema.org / JSON-LD: "datePublished":"YYYY-MM-DD"
        java.util.regex.Matcher m3 = java.util.regex.Pattern.compile(
                "datePublished[\"':\\s]+([0-9]{4}(?:-[0-9]{2}-[0-9]{2})?)",
                java.util.regex.Pattern.CASE_INSENSITIVE
        ).matcher(html);
        if (m3.find()) {
            return m3.group(1);
        }

        // 4. JSON embebido: "release_date":"YYYY-MM-DD"
        java.util.regex.Matcher m4 = java.util.regex.Pattern.compile(
                "\"release_date\"\\s*:\\s*\"([0-9]{4}(?:-[0-9]{2}-[0-9]{2})?)\"",
                java.util.regex.Pattern.CASE_INSENSITIVE
        ).matcher(html);
        if (m4.find()) {
            return m4.group(1);
        }

        return null;
    }

    public String obtenerFechaDeWebSpotify(String cleanId) {
        if (cleanId == null || cleanId.isBlank()) {
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            headers.set("Accept-Language", "es-ES,es;q=0.9,en;q=0.8");
            HttpEntity<Void> req = new HttpEntity<>(headers);

            String url = "https://open.spotify.com/album/" + cleanId;
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, req, String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                return extraerFechaDeHtmlSpotify(resp.getBody());
            }
        } catch (Exception ignored) {
        }
        return null;
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
                boolean fechaValida = cached.fechaLanzamiento() != null && !cached.fechaLanzamiento().isBlank()
                        && (!"1973-03-01".equals(cached.fechaLanzamiento()) || esPinkFloyd(tituloFallback));
                if (fechaValida) {
                    return cached;
                }
            }
        }

        String token = obtenerToken();
        String fecha = null;
        String nombre = null;
        String artista = null;
        String imagen = null;
        List<TrackDto> tracks = new ArrayList<>();

        if (token != null) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                String url = "https://api.spotify.com/v1/albums/" + cleanId;
                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> body = response.getBody();
                    nombre = (String) body.get("name");
                    fecha = extraerFechaLanzamientoDeJson(body);

                    artista = "Varios Artistas";
                    List<Map<String, Object>> artistas = (List<Map<String, Object>>) body.get("artists");
                    if (artistas != null && !artistas.isEmpty()) {
                        artista = (String) artistas.get(0).get("name");
                    }

                    imagen = extraerCaratulaDeJson(body);

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
                }
            } catch (Exception ignored) {
            }
        }

        // Si la API OAuth falló o está rate-limited (HTTP 429), obtener fecha de la web pública de Spotify
        if (fecha == null || fecha.isBlank() || ("1973-03-01".equals(fecha) && !esPinkFloyd(tituloFallback))) {
            fecha = obtenerFechaDeWebSpotify(cleanId);
        }

        // Si aún es nula, consultar el mapa de fechas oficiales conocidas
        if ((fecha == null || fecha.isBlank()) && FECHAS_SPOTIFY_OFICIALES.containsKey(cleanId)) {
            fecha = FECHAS_SPOTIFY_OFICIALES.get(cleanId);
        }

        // Si aún es nula, extraer año del título si viene expresado como (YYYY) o 19XX/20XX
        if (fecha == null || fecha.isBlank()) {
            String anio = extraerAnioDeTitulo(tituloFallback);
            if (anio != null) {
                fecha = anio;
            }
        }

        if (fecha != null && !fecha.isBlank()) {
            cacheFechasLanzamiento.put(cleanId, fecha);
        }

        // Fallback dinámico de carátula y título vía Spotify oEmbed oficial
        if (imagen == null || imagen.isBlank() || nombre == null || nombre.isBlank()) {
            try {
                String oembedUrl = "https://open.spotify.com/oembed?url=https://open.spotify.com/album/" + cleanId;
                ResponseEntity<Map> response = restTemplate.getForEntity(oembedUrl, Map.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Object thumb = response.getBody().get("thumbnail_url");
                    if (thumb != null && !thumb.toString().isBlank() && (imagen == null || imagen.isBlank())) {
                        imagen = thumb.toString().replace("/1e02", "/b273");
                    }
                    Object titleObj = response.getBody().get("title");
                    if (titleObj != null && !titleObj.toString().isBlank() && (nombre == null || nombre.isBlank())) {
                        nombre = titleObj.toString();
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (imagen == null || imagen.isBlank()) {
            imagen = imagenUrlFallback;
        }
        if (imagen == null && CARATULAS_SPOTIFY_OFICIALES.containsKey(cleanId)) {
            imagen = CARATULAS_SPOTIFY_OFICIALES.get(cleanId);
        }
        if (nombre == null || nombre.isBlank()) {
            nombre = tituloFallback != null ? tituloFallback : "Álbum Clásico Analógico";
        }
        if (artista == null || artista.isBlank()) {
            artista = extraerArtistaDeTitulo(nombre != null ? nombre : tituloFallback);
        }

        if (fecha == null || fecha.isBlank()) {
            if (esPinkFloyd(nombre) || esPinkFloyd(tituloFallback)) {
                fecha = "1973-03-01";
            }
        }

        AlbumDto dto = new AlbumDto(cleanId, nombre, artista, fecha, imagen, tracks);
        cacheAlbumes.put(cleanId, dto);
        if (imagen != null && !imagen.isBlank()) {
            cacheCaratulas.put(cleanId, imagen);
        }
        return dto;
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

    public static String extraerFechaLanzamientoDeJson(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object releaseDate = body.get("release_date");
        if (releaseDate != null && !releaseDate.toString().isBlank()) {
            return releaseDate.toString().trim();
        }
        return null;
    }

    public static String extraerAnioDeTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\b(19\\d\\d|20\\d\\d)\\b").matcher(titulo);
        if (matcher.find()) {
            return matcher.group(1);
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
                    Map<String, Object> body = response.getBody();
                    String img = extraerCaratulaDeJson(body);
                    String fecha = extraerFechaLanzamientoDeJson(body);
                    if (fecha != null && !fecha.isBlank()) {
                        cacheFechasLanzamiento.put(cleanId, fecha);
                    }
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

    @SuppressWarnings("unchecked")
    public String obtenerFechaLanzamiento(String spotifyAlbumId) {
        String cleanId = sanitizarAlbumId(spotifyAlbumId);
        if (cleanId == null || cleanId.isBlank()) {
            return null;
        }

        if (cacheFechasLanzamiento.containsKey(cleanId)) {
            String cached = cacheFechasLanzamiento.get(cleanId);
            if (cached != null && !cached.isBlank()) {
                return cached;
            }
        }

        if (cacheAlbumes.containsKey(cleanId)) {
            AlbumDto cached = cacheAlbumes.get(cleanId);
            if (cached.fechaLanzamiento() != null && !cached.fechaLanzamiento().isBlank() && !cached.fechaLanzamiento().equals("1973-03-01")) {
                cacheFechasLanzamiento.put(cleanId, cached.fechaLanzamiento());
                return cached.fechaLanzamiento();
            }
        }

        // 1. Intentar API oficial de Spotify (OAuth Client Credentials)
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
                    String fecha = extraerFechaLanzamientoDeJson(body);
                    String img = extraerCaratulaDeJson(body);
                    if (img != null && !img.isBlank()) {
                        cacheCaratulas.put(cleanId, img);
                    }
                    if (fecha != null && !fecha.isBlank()) {
                        cacheFechasLanzamiento.put(cleanId, fecha);
                        return fecha;
                    }
                }
            } catch (Exception ignored) {
                // Silencioso ante cuotas agotadas (HTTP 429) o fallas de red
            }
        }

        // 2. Extracción directa desde la web pública open.spotify.com/album/{id} (Sin límite de cuotas OAuth)
        String fechaWeb = obtenerFechaDeWebSpotify(cleanId);
        if (fechaWeb != null && !fechaWeb.isBlank()) {
            cacheFechasLanzamiento.put(cleanId, fechaWeb);
            return fechaWeb;
        }

        // 3. Fallback a catálogo curado estático de fechas oficiales
        if (FECHAS_SPOTIFY_OFICIALES.containsKey(cleanId)) {
            String estatica = FECHAS_SPOTIFY_OFICIALES.get(cleanId);
            cacheFechasLanzamiento.put(cleanId, estatica);
            return estatica;
        }

        return null;
    }

    private AlbumDto fallbackAlbum(String id, String titulo, String formato) {
        return fallbackAlbum(id, titulo, formato, null);
    }

    private static final Map<String, String> FECHAS_SPOTIFY_OFICIALES = Map.ofEntries(
            Map.entry("1pb3je8gXTs5dpRRTKhHRC", "2023-06-09"), // Noah Kahan - Stick Season (We'll All Be Here Forever 2LP)
            Map.entry("50ZenUP4O2Q5eCy2NRNvuz", "2022-10-14"), // Noah Kahan - Stick Season Standard
            Map.entry("2iMF2NlOZMfBTdHyubrg6y", "1985-01-01"), // Hombres G
            Map.entry("6clqMga4PMBcBlWCR6idis", "1986-01-01"), // Hombres G - La cagaste... Burt Lancaster
            Map.entry("0hBRqPYPXhr1RkTDG3n4Mk", "2024-10-18"), // Gracie Abrams - The Secret of Us Deluxe
            Map.entry("56bdWeO40o3WfAD2Lja4dl", "2024-06-21"), // Gracie Abrams - The Secret of Us
            Map.entry("78YYcghEDz2dHRx0EcDGXZ", "2023-02-24"), // Gracie Abrams - Good Riddance
            Map.entry("4uG8q3GPuGHQlRbswMIRS6", "1994-02-01"), // Green Day - Dookie
            Map.entry("4uG8q3GPuGHQlGeSpRJbtv", "1994-02-01"), // Green Day - Dookie alt
            Map.entry("5dN7F9DV0Qg1XRdIgW8rke", "2004-09-21"), // Green Day - American Idiot
            Map.entry("0bUTHlWbkSQysoM3VsWldT", "2005-05-23"), // Gorillaz - Demon Days
            Map.entry("0bUTHLuuETwgVsc9L7xR3t", "2005-05-23"), // Gorillaz - Demon Days alt
            Map.entry("4tUxQkrduOE8sfgwJ5BI2F", "2001-03-26"), // Gorillaz - Gorillaz
            Map.entry("3GoSlKTNcVOp1ZxE5OOXeN", "1990-08-07"), // Soda Stereo - Canción Animal
            Map.entry("7s2lDvm4FwnWE5RXEoY8Gi", "1986-11-10"), // Soda Stereo - Signos
            Map.entry("0k2JGzYRZQC3s3ZNTcyHoA", "1995-06-29"), // Soda Stereo - Sueño Stereo
            Map.entry("3y63u5vmuMugqI8lfuUY3a", "1994-08-15"), // Enanitos Verdes - Big Bang
            Map.entry("33VjH4cujQd33gXl0vRz9B", "1994-08-15"), // Enanitos Verdes - Big Bang alt
            Map.entry("0ETFjACtuP2ADo6LFhL6HN", "1969-09-26"), // The Beatles - Abbey Road
            Map.entry("2ANVost0y2y52ema1E9xAZ", "1982-11-30"), // Michael Jackson - Thriller
            Map.entry("4LH4d3cOWNNXdJwFd4G1kc", "1973-03-01"), // Pink Floyd - The Dark Side of the Moon
            Map.entry("4LH4d3cOWNNsVw41Gqt2kv", "1973-03-01"), // Pink Floyd - The Dark Side of the Moon (alt)
            Map.entry("78bpIziExqiI9qztvNFlQu", "2013-09-09"), // Arctic Monkeys - AM
            Map.entry("0BwWUstDMUbgq2NYONRqlu", "1977-02-04"), // Fleetwood Mac - Rumours
            Map.entry("44Ig8dzqOkvkGDzaUof9lK", "1971-11-08"), // Led Zeppelin - Led Zeppelin IV
            Map.entry("4m2880jivSbbyEGAKfITCa", "2013-05-20"), // Daft Punk - Random Access Memories
            Map.entry("1weenld61qoidwYuZ1GESA", "1959-08-17"), // Miles Davis - Kind of Blue
            Map.entry("1TkbyIkf6GSrO5e7gWS4AM", "1975-11-21"), // Queen - A Night at the Opera
            Map.entry("1GbtB4K178q79T9vAgj2HN", "1975-11-21"), // Queen - A Night at the Opera alt
            Map.entry("5eA0qtE7Yu29XiMlwoby2G", "1991-09-24"), // Nirvana - Nevermind
            Map.entry("48D1hRORqJq52qsnUYZX56", "1972-06-06"), // David Bowie - Ziggy Stardust
            Map.entry("655KljKIXl42fiNDMKivbY", "1977-06-03"), // Bob Marley - Exodus
            Map.entry("6FCzvataOZh68j8OKzOt9a", "1979-12-14")  // The Clash - London Calling
    );

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
        String fecha = null;

        if (id != null) {
            fecha = obtenerFechaLanzamiento(id);
        }
        if (fecha == null) {
            String anio = extraerAnioDeTitulo(titulo);
            if (anio != null) {
                fecha = anio;
            }
        }
        if (fecha == null && (esPinkFloyd(nombre) || esPinkFloyd(titulo))) {
            fecha = "1973-03-01";
        }
        
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
