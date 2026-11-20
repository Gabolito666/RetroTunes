package com.example.app;

import com.crud_relacional.com.entidades.*;
import com.crud_relacional.com.repositorio.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DemoApplicationTests {

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProductoMusicaRepository productoMusicaRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private AlquilerEquipoRepository alquilerEquipoRepository;

    @Autowired
    private ReservaEspacioRepository reservaEspacioRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Contexto Spring Boot carga correctamente")
    void contextLoads() {
        assertNotNull(rolRepository);
        assertNotNull(usuarioRepository);
        assertNotNull(productoMusicaRepository);
        assertNotNull(ventaRepository);
        assertNotNull(alquilerEquipoRepository);
        assertNotNull(reservaEspacioRepository);
    }

    @Test
    @DisplayName("DataInitializer inserta roles, admin y productos semilla")
    void testDataInitializer() {
        assertTrue(rolRepository.count() >= 5, "Deben existir al menos 5 roles");
        assertTrue(usuarioRepository.existsByCorreo("admin@retrotunes.com"), "El admin debe existir");
        assertTrue(productoMusicaRepository.count() >= 4, "Deben existir al menos 4 productos semilla");

        Optional<Usuario> adminOpt = usuarioRepository.findByCorreo("admin@retrotunes.com");
        assertTrue(adminOpt.isPresent());
        assertEquals("ADMIN", adminOpt.get().getRol().getNombre_rol());
    }

    @Test
    @Transactional
    @DisplayName("Sprint 2: Creación transaccional de Venta y DetalleVenta")
    void testVentaYDetalle() {
        Usuario admin = usuarioRepository.findByCorreo("admin@retrotunes.com").orElseThrow();
        ProductoMusica vinilo = productoMusicaRepository.findByFormato("VINILO").get(0);

        String codFactura = "FAC-TEST-001";
        BigDecimal total = vinilo.getPrecio_venta().multiply(BigDecimal.valueOf(2));

        Venta venta = new Venta(codFactura, LocalDateTime.now(), total, "TARJETA_CREDITO", admin);
        DetalleVenta detalle = new DetalleVenta(venta, vinilo, 2, vinilo.getPrecio_venta(), total);
        venta.agregarDetalle(detalle);

        Venta guardada = ventaRepository.save(venta);
        assertNotNull(guardada.getId_venta());
        assertEquals(codFactura, guardada.getCodigo_factura());
        assertEquals(1, guardada.getDetalles().size());
        assertTrue(ventaRepository.existsByCodigoFactura(codFactura));
    }

    @Test
    @Transactional
    @DisplayName("Sprint 2: Alquiler con cálculo de 30% de depósito de garantía")
    void testAlquilerEquipo() {
        Usuario admin = usuarioRepository.findByCorreo("admin@retrotunes.com").orElseThrow();
        ProductoMusica equipo = productoMusicaRepository.findByFormato("EQUIPO_SONIDO").get(0);

        BigDecimal depositoEsperado = equipo.getPrecio_venta()
                .multiply(new BigDecimal("0.30"))
                .setScale(2, RoundingMode.HALF_UP);

        LocalDate inicio = LocalDate.now().plusDays(1);
        LocalDate fin = LocalDate.now().plusDays(4); // 4 días

        BigDecimal montoAlquiler = equipo.getPrecio_alquiler_dia().multiply(BigDecimal.valueOf(4));

        AlquilerEquipo alquiler = new AlquilerEquipo(
                "ALQ-TEST-001",
                inicio,
                fin,
                depositoEsperado,
                montoAlquiler,
                "RESERVADO",
                admin,
                equipo
        );

        AlquilerEquipo guardado = alquilerEquipoRepository.save(alquiler);
        assertNotNull(guardado.getId_alquiler());
        assertEquals(depositoEsperado, guardado.getDeposito_garantia());
        assertEquals("RESERVADO", guardado.getEstado());
    }

    @Test
    @Transactional
    @DisplayName("Sprint 2: Reserva de espacio para audiciones")
    void testReservaEspacio() {
        Usuario admin = usuarioRepository.findByCorreo("admin@retrotunes.com").orElseThrow();
        LocalDate fecha = LocalDate.now().plusDays(7);

        ReservaEspacio reserva = new ReservaEspacio(
                fecha,
                "14:00 - 17:00",
                "google-evt-12345",
                admin
        );

        ReservaEspacio guardada = reservaEspacioRepository.save(reserva);
        assertNotNull(guardada.getId_reserva());
        assertFalse(reservaEspacioRepository.findByFechaReserva(fecha).isEmpty());
    }

    @Test
    @Transactional
    @DisplayName("Facturación PDF: Generación en memoria con OpenPDF institucional")
    void testGenerarFacturaPdf() throws Exception {
        Usuario admin = usuarioRepository.findByCorreo("admin@retrotunes.com").orElseThrow();
        ProductoMusica vinilo = productoMusicaRepository.findByFormato("VINILO").get(0);

        String codFactura = "FAC-RETRO-TEST-PDF-001";
        BigDecimal total = new BigDecimal("120000.00");

        Venta venta = new Venta(codFactura, LocalDateTime.now(), total, "TARJETA_CREDITO", admin);
        DetalleVenta detalle = new DetalleVenta(venta, vinilo, 1, total, total);
        venta.agregarDetalle(detalle);

        Venta guardada = ventaRepository.save(venta);
        byte[] pdfBytes = com.crud_relacional.com.controlador.FacturaPdfHelper.generarFactura(guardada);

        assertNotNull(pdfBytes, "El PDF generado no debe ser nulo");
        assertTrue(pdfBytes.length > 500, "El tamaño del PDF debe ser representativo");

        // Verificación de firma/cabecera estándar de documento PDF (%PDF)
        String pdfHeader = new String(pdfBytes, 0, 4);
        assertEquals("%PDF", pdfHeader, "El binario generado debe comenzar con %PDF");
    }

    @Test
    @Transactional
    @DisplayName("Reportes Avanzados: Generación de Excel y PDF por Tipo de Producto")
    void testReportesAvanzados() throws Exception {
        List<com.crud_relacional.com.controlador.AdminController.VentaPorTipoDto> reporteTipo = List.of(
                new com.crud_relacional.com.controlador.AdminController.VentaPorTipoDto("VINILO", 10, new BigDecimal("350.00"), 5),
                new com.crud_relacional.com.controlador.AdminController.VentaPorTipoDto("CASSETTE", 8, new BigDecimal("160.00"), 4),
                new com.crud_relacional.com.controlador.AdminController.VentaPorTipoDto("CD", 15, new BigDecimal("240.00"), 7)
        );

        byte[] pdfReporte = com.crud_relacional.com.controlador.ReportePdfHelper.generarReporteVentasPorTipo(reporteTipo);
        assertNotNull(pdfReporte);
        assertEquals("%PDF", new String(pdfReporte, 0, 4));

        byte[] excelReporte = com.crud_relacional.com.controlador.ReporteExcelHelper.generarReporteVentasPorTipoExcel(reporteTipo);
        assertNotNull(excelReporte);
        assertTrue(excelReporte.length > 1000);
    }

    @Test
    @DisplayName("Catálogo: Normalización de SKUs estándar VIN-XXXX, CAS-XXXX, CD-XXXX, EQP-XXXX")
    void testSkusNormalizados() {
        assertTrue(productoMusicaRepository.existsBySku("VIN-0001"), "Debe existir VIN-0001");
        assertTrue(productoMusicaRepository.existsBySku("CAS-0001"), "Debe existir CAS-0001");
        assertTrue(productoMusicaRepository.existsBySku("CD-0001"), "Debe existir CD-0001");
        assertTrue(productoMusicaRepository.existsBySku("EQP-0001"), "Debe existir EQP-0001");
    }

    @Test
    @DisplayName("Catálogo vs Alquileres: Separación estricta de música física y equipos de sonido")
    void testSeparacionEstrictaMusicaYEquipos() {
        List<ProductoMusica> soloMusica = productoMusicaRepository.findSoloMusica();
        assertFalse(soloMusica.isEmpty(), "Debe existir catálogo de música física");
        assertTrue(soloMusica.stream().allMatch(p -> List.of("VINILO", "CASSETTE", "CD").contains(p.getFormato())),
                "El catálogo de música solo debe contener Vinilos, Cassettes y CDs");
        assertFalse(soloMusica.stream().anyMatch(p -> "EQUIPO_SONIDO".equals(p.getFormato())),
                "El catálogo de música nunca debe incluir equipos de sonido");

        List<ProductoMusica> soloEquipos = productoMusicaRepository.findSoloEquipos();
        assertFalse(soloEquipos.isEmpty(), "Debe existir catálogo de equipos de sonido");
        assertTrue(soloEquipos.stream().allMatch(p -> "EQUIPO_SONIDO".equals(p.getFormato())),
                "El catálogo de alquileres solo debe contener equipos de sonido");
    }

    @Test
    @DisplayName("Spotify API: Extracción dinámica de artista real y eliminación de Artista Curado")
    void testSpotifyArtistExtraction() {
        String artista1 = com.crud_relacional.com.controlador.SpotifyClientHelper.extraerArtistaDeTitulo("Hombres G - Voy a pasármelo bien (1989)");
        assertEquals("Hombres G", artista1);

        String artista2 = com.crud_relacional.com.controlador.SpotifyClientHelper.extraerArtistaDeTitulo("Gorillaz - Demon Days (2005)");
        assertEquals("Gorillaz", artista2);

        String artista3 = com.crud_relacional.com.controlador.SpotifyClientHelper.extraerArtistaDeTitulo("Green Day - Dookie (1994)");
        assertEquals("Green Day", artista3);

        String artista4 = com.crud_relacional.com.controlador.SpotifyClientHelper.extraerArtistaDeTitulo("Gracie Abrams - The Secret of Us (2024)");
        assertEquals("Gracie Abrams", artista4);
    }

    @Test
    @DisplayName("Spotify API: Extracción de carátula principal de JSON y fallback limpio")
    void testSpotifyCoverExtractionAndFallback() {
        // Simulación de respuesta JSON de Spotify con arreglo 'images'
        java.util.Map<String, Object> fakeSpotifyJson = java.util.Map.of(
                "images", java.util.List.of(
                        java.util.Map.of("url", "https://i.scdn.co/image/ab67616d0000b27341077fc4e87747e45ce28489", "height", 640, "width", 640),
                        java.util.Map.of("url", "https://i.scdn.co/image/ab67616d00001e0241077fc4e87747e45ce28489", "height", 300, "width", 300)
                )
        );

        String coverUrl = com.crud_relacional.com.controlador.SpotifyClientHelper.extraerCaratulaDeJson(fakeSpotifyJson);
        assertNotNull(coverUrl);
        assertEquals("https://i.scdn.co/image/ab67616d0000b27341077fc4e87747e45ce28489", coverUrl);

        // Verificación de fallback sin imágenes
        String fallbackNull = com.crud_relacional.com.controlador.SpotifyClientHelper.extraerCaratulaDeJson(java.util.Collections.emptyMap());
        assertNull(fallbackNull);

        // Verificación de extracción de Apple Music Animated Artwork (.mp4)
        String directMp4 = "https://artwork.apple.com/videos/demon-days-artwork.mp4";
        assertEquals(directMp4, com.crud_relacional.com.controlador.CatalogoController.extraerAppleMusicAnimatedArtwork(directMp4));

        String embeddedUrl = "https://music.apple.com/us/album/demon-days/1234?video=https://artwork.apple.com/animated.mp4";
        assertEquals("https://artwork.apple.com/animated.mp4", com.crud_relacional.com.controlador.CatalogoController.extraerAppleMusicAnimatedArtwork(embeddedUrl));

        ProductoMusica pm = new ProductoMusica();
        pm.setAnimatedArtworkUrl(directMp4);
        assertEquals(directMp4, pm.getAnimatedArtworkUrl());

        // Verificación de prohibición de imágenes de stock y portada oficial de Noah Kahan
        List<ProductoMusica> catalogo = productoMusicaRepository.findAll();
        assertFalse(catalogo.stream().anyMatch(p -> p.getImagen_url() != null && p.getImagen_url().contains("unsplash")),
                "Ningún producto del catálogo debe tener imágenes de stock de Unsplash");

        ProductoMusica noah = productoMusicaRepository.findBySku("VIN-0004").orElse(null);
        if (noah != null) {
            assertEquals("50ZenUP4O2Q5eCy2NRNvuz", noah.getSpotify_album_id());
            assertEquals("https://i.scdn.co/image/ab67616d0000b2736ee35072df1af802cca09918", noah.getImagen_url(),
                    "Noah Kahan debe tener la portada oficial extraída de Spotify");
        }
    }

    @Test
    @DisplayName("Spotify API: Extracción automática de fecha de lanzamiento desde JSON y Helper")
    void testSpotifyReleaseDateExtraction() {
        // 1. Simulación de respuesta JSON de la API de Spotify con release_date
        java.util.Map<String, Object> fakeSpotifyJson = java.util.Map.of(
                "id", "0ETFjACtuP2ADo6LFhL6HN",
                "name", "Abbey Road",
                "release_date", "1969-09-26",
                "release_date_precision", "day"
        );

        String fechaJson = com.crud_relacional.com.controlador.SpotifyClientHelper.extraerFechaLanzamientoDeJson(fakeSpotifyJson);
        assertNotNull(fechaJson);
        assertEquals("1969-09-26", fechaJson, "La fecha de lanzamiento debe coincidir con release_date de la API");

        // 2. Verificación de fallback nulo para JSON vacío o sin campo
        String fechaNull = com.crud_relacional.com.controlador.SpotifyClientHelper.extraerFechaLanzamientoDeJson(java.util.Collections.emptyMap());
        assertNull(fechaNull);

        // 3. Extracción de año desde título con formato (YYYY)
        String anioTitulo = com.crud_relacional.com.controlador.SpotifyClientHelper.extraerAnioDeTitulo("Gorillaz - Demon Days (2005)");
        assertEquals("2005", anioTitulo);

        // 4. Verificación de persistencia de fecha_lanzamiento en ProductoMusica
        ProductoMusica vinilo = productoMusicaRepository.findBySku("VIN-0001").orElse(null);
        assertNotNull(vinilo, "El vinilo VIN-0001 debe existir");
        assertEquals("1969-09-26", vinilo.getFecha_lanzamiento(), "Abbey Road debe tener su fecha oficial de lanzamiento 1969-09-26");

        // 5. Verificación de obtención de fecha vía SpotifyClientHelper
        com.crud_relacional.com.controlador.SpotifyClientHelper helper = new com.crud_relacional.com.controlador.SpotifyClientHelper();
        String fechaBeatles = helper.obtenerFechaLanzamiento("0ETFjACtuP2ADo6LFhL6HN");
        assertEquals("1969-09-26", fechaBeatles);

        String fechaThriller = helper.obtenerFechaLanzamiento("2ANVost0y2y52ema1E9xAZ");
        assertEquals("1982-11-30", fechaThriller);

        String fechaPinkFloyd = helper.obtenerFechaLanzamiento("4LH4d3cOWNNXdJwFd4G1kc");
        assertEquals("1973-03-01", fechaPinkFloyd);
    }

    @Test
    @DisplayName("Seguridad: Cifrado BCrypt de contraseñas y validación de hash")
    void testBCryptPasswordHashing() {
        assertNotNull(passwordEncoder, "El bean PasswordEncoder debe estar disponible");

        // Verificación de hash para admin inicializado
        Optional<Usuario> adminOpt = usuarioRepository.findByCorreo("admin@retrotunes.com");
        assertTrue(adminOpt.isPresent(), "El administrador debe existir");
        String adminHash = adminOpt.get().getClave_usuario();
        assertNotNull(adminHash);
        assertTrue(adminHash.startsWith("$2a$") || adminHash.startsWith("$2b$") || adminHash.startsWith("$2y$"),
                "La clave del administrador debe estar cifrada con BCrypt (inicia con $2)");
        assertTrue(passwordEncoder.matches("admin123", adminHash),
                "La contraseña 'admin123' debe coincidir con el hash BCrypt");
        assertFalse(passwordEncoder.matches("wrongpassword", adminHash),
                "Una clave errónea no debe coincidir");

        // Verificación de nuevo cifrado
        String rawPass = "Segura2026!";
        String encoded = passwordEncoder.encode(rawPass);
        assertNotEquals(rawPass, encoded);
        assertTrue(passwordEncoder.matches(rawPass, encoded));
    }

    @Test
    @Transactional
    @DisplayName("Auditoría: Registro transaccional de Venta y generación de Factura PDF")
    void testVentaYGeneracionFacturaPdf() throws Exception {
        Usuario admin = usuarioRepository.findByCorreo("admin@retrotunes.com").orElseThrow();
        ProductoMusica vinilo = productoMusicaRepository.findByFormato("VINILO").get(0);

        String codFactura = "FAC-TEST-PDF-001";
        BigDecimal total = vinilo.getPrecio_venta();

        Venta venta = new Venta(codFactura, LocalDateTime.now(), total, "TARJETA", admin);
        DetalleVenta detalle = new DetalleVenta(venta, vinilo, 1, vinilo.getPrecio_venta(), total);
        venta.agregarDetalle(detalle);

        Venta guardada = ventaRepository.save(venta);
        assertNotNull(guardada.getId_venta());

        // Generar Factura PDF oficial
        byte[] pdfBytes = com.crud_relacional.com.controlador.FacturaPdfHelper.generarFactura(guardada);
        assertNotNull(pdfBytes, "El PDF de factura no debe ser nulo");
        assertTrue(pdfBytes.length > 500, "El PDF generado debe contener bytes válidos");

        // Cabecera PDF: %PDF-
        String header = new String(pdfBytes, 0, 4, java.nio.charset.StandardCharsets.ISO_8859_1);
        assertEquals("%PDF", header, "El archivo generado debe ser un PDF válido");
    }
}
