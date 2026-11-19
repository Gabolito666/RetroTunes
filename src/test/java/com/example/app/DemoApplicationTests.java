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
}
