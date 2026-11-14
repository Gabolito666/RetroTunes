package com.example.app;

import com.crud_relacional.com.entidades.ProductoMusica;
import com.crud_relacional.com.entidades.Rol;
import com.crud_relacional.com.entidades.Usuario;
import com.crud_relacional.com.repositorio.ProductoMusicaRepository;
import com.crud_relacional.com.repositorio.RolRepository;
import com.crud_relacional.com.repositorio.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoMusicaRepository productoMusicaRepository;

    public DataInitializer(RolRepository rolRepository,
                           UsuarioRepository usuarioRepository,
                           ProductoMusicaRepository productoMusicaRepository) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoMusicaRepository = productoMusicaRepository;
    }

    @Override
    public void run(String... args) {
        try {
            if (rolRepository.count() == 0) {
                log.info("Inicializando datos semilla en la base de datos (Supabase PostgreSQL)...");

                // 1. Inserción de los 5 Roles del sistema
                Rol rolAdmin = new Rol("ADMIN");
                Rol rolCliente = new Rol("CLIENTE");
                Rol rolArtista = new Rol("ARTISTA");
                Rol rolDiscografica = new Rol("DISCOGRAFICA");
                Rol rolTecnico = new Rol("TECNICO_OPERACIONES");

                List<Rol> rolesGuardados = rolRepository.saveAll(Arrays.asList(
                        rolAdmin, rolCliente, rolArtista, rolDiscografica, rolTecnico
                ));
                log.info("Se han registrado {} roles exitosamente.", rolesGuardados.size());

                // 2. Creación de Usuario Administrador por defecto
                if (!usuarioRepository.existsByCorreo("admin@retrotunes.com")) {
                    Usuario admin = new Usuario();
                    admin.setNombre_completo("Administrador RetroTunes");
                    admin.setCorreo("admin@retrotunes.com");
                    admin.setClave_usuario("admin123");
                    admin.setTelefono("+57 300 000 0000");
                    admin.setActivo(true);
                    admin.setRol(rolAdmin);

                    usuarioRepository.save(admin);
                    log.info("Usuario Administrador semilla creado: admin@retrotunes.com");
                }

                // 3. Registro de al menos 4 productos semilla (Vinilo, Cassette, CD, Equipo de Sonido)
                if (productoMusicaRepository.count() == 0) {
                    ProductoMusica vinilo = new ProductoMusica(
                            "VIN-ABBEY-001",
                            "The Beatles - Abbey Road (Edición Especial 180g)",
                            "VINILO",
                            new BigDecimal("39.99"),
                            new BigDecimal("4.50"),
                            15,
                            "0ETFjACtuP2ADo6LFhL6HN"
                    );

                    ProductoMusica cassette = new ProductoMusica(
                            "CAS-THRILLER-002",
                            "Michael Jackson - Thriller (Cassette Vintage Remastered)",
                            "CASSETTE",
                            new BigDecimal("19.50"),
                            new BigDecimal("2.50"),
                            10,
                            "2ANVost0y2y52ema1E9xAZ"
                    );

                    ProductoMusica cd = new ProductoMusica(
                            "CD-DARK-003",
                            "Pink Floyd - The Dark Side of the Moon (Deluxe CD)",
                            "CD",
                            new BigDecimal("15.90"),
                            new BigDecimal("1.75"),
                            25,
                            "4LH4d3cOWNNXdJwFd4G1kc"
                    );

                    ProductoMusica equipoSonido = new ProductoMusica(
                            "EQ-CROSLEY-004",
                            "Tornamesa Portátil Retro Cruiser Deluxe Bluetooth",
                            "EQUIPO_SONIDO",
                            new BigDecimal("129.99"),
                            new BigDecimal("15.00"),
                            6,
                            null
                    );

                    productoMusicaRepository.saveAll(Arrays.asList(vinilo, cassette, cd, equipoSonido));
                    log.info("4 productos semilla registrados con éxito para verificación en catálogo.");
                }
            } else {
                log.info("La base de datos ya contiene roles registrados. Omitiendo DataInitializer.");
            }
        } catch (Exception e) {
            log.warn("Aviso en inicialización de datos: {}", e.getMessage());
        }
    }
}
