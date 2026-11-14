package com.example.app;

import com.crud_relacional.com.entidades.Rol;
import com.crud_relacional.com.entidades.Usuario;
import com.crud_relacional.com.entidades.ProductoMusica;
import com.crud_relacional.com.repositorio.RolRepository;
import com.crud_relacional.com.repositorio.UsuarioRepository;
import com.crud_relacional.com.repositorio.ProductoMusicaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
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

    @Test
    @DisplayName("Contexto Spring Boot carga correctamente")
    void contextLoads() {
        assertNotNull(rolRepository);
        assertNotNull(usuarioRepository);
        assertNotNull(productoMusicaRepository);
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
}
