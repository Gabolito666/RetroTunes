package com.example.app;

import com.crud_relacional.com.controlador.SpotifyClientHelper;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoMusicaRepository productoMusicaRepository;
    private final SpotifyClientHelper spotifyClientHelper;

    public DataInitializer(RolRepository rolRepository,
                           UsuarioRepository usuarioRepository,
                           ProductoMusicaRepository productoMusicaRepository,
                           SpotifyClientHelper spotifyClientHelper) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoMusicaRepository = productoMusicaRepository;
        this.spotifyClientHelper = spotifyClientHelper;
    }

    @Override
    public void run(String... args) {
        try {
            inicializarRolesYAdmin();
            sincronizarCatalogoCorregido();
        } catch (Exception e) {
            log.warn("Aviso en inicialización de datos: {}", e.getMessage(), e);
        }
    }

    private void inicializarRolesYAdmin() {
        if (rolRepository.count() == 0) {
            log.info("Inicializando roles del sistema en Supabase PostgreSQL...");
            rolRepository.saveAll(Arrays.asList(
                    new Rol("ADMIN"),
                    new Rol("CLIENTE"),
                    new Rol("ARTISTA"),
                    new Rol("DISCOGRAFICA"),
                    new Rol("TECNICO_OPERACIONES")
            ));
        }

        if (!usuarioRepository.existsByCorreo("admin@retrotunes.com")) {
            Rol rolAdmin = rolRepository.findByNombre_rol("ADMIN")
                    .orElseGet(() -> rolRepository.save(new Rol("ADMIN")));

            Usuario admin = new Usuario();
            admin.setNombre_completo("Administrador RetroTunes");
            admin.setCorreo("admin@retrotunes.com");
            admin.setClave_usuario("admin123");
            admin.setTelefono("+57 300 000 0000");
            admin.setActivo(true);
            admin.setRol(rolAdmin);

            usuarioRepository.save(admin);
            log.info("Usuario Administrador semilla verificado: admin@retrotunes.com");
        }
    }

    private void sincronizarCatalogoCorregido() {
        // 1. Si la base de datos está completamente vacía (por ejemplo en entorno de pruebas H2 en memoria),
        // insertar los 4 productos semilla requeridos por el sistema y las pruebas unitarias.
        if (productoMusicaRepository.count() == 0) {
            log.info("Base de datos sin productos detectada. Inicializando los 4 productos semilla base...");
            List<ProductoMusica> semillas = List.of(
                    new ProductoMusica(
                            "VIN-0001",
                            "The Beatles - Abbey Road (Edición Especial 180g)",
                            "VINILO",
                            new BigDecimal("39.99"),
                            new BigDecimal("4.50"),
                            15,
                            "0ETFjACtuP2ADo6LFhL6HN"
                    ),
                    new ProductoMusica(
                            "CAS-0001",
                            "Michael Jackson - Thriller (Cassette Vintage Remastered)",
                            "CASSETTE",
                            new BigDecimal("19.50"),
                            new BigDecimal("2.50"),
                            10,
                            "2ANVost0y2y52ema1E9xAZ"
                    ),
                    new ProductoMusica(
                            "CD-0001",
                            "Pink Floyd - The Dark Side of the Moon (Deluxe CD)",
                            "CD",
                            new BigDecimal("15.90"),
                            new BigDecimal("1.75"),
                            25,
                            "4LH4d3cOWNNsVw41Gqt2kv"
                    ),
                    new ProductoMusica(
                            "EQP-0001",
                            "Tornamesa Portátil Retro Cruiser Deluxe Bluetooth",
                            "EQUIPO_SONIDO",
                            new BigDecimal("129.99"),
                            new BigDecimal("15.00"),
                            6,
                            null
                    )
            );
            productoMusicaRepository.saveAll(semillas);
            log.info("4 productos semilla base registrados con éxito en base de datos vacía.");
            return;
        }

        // 2. Si la base de datos ya contiene catálogo (ej. Supabase PostgreSQL en producción),
        // proteger estrictamente la información y modificaciones de los usuarios.
        // Solo sanitizar URLs de stock (Unsplash) y URLs completas de Spotify sin alterar álbumes ni títulos.
        log.info("Base de datos ya contiene {} productos. Protegiendo datos de inventario sin sobrescrituras.", productoMusicaRepository.count());
        try {
            List<ProductoMusica> existentesEnDb = productoMusicaRepository.findAll();
            for (ProductoMusica p : existentesEnDb) {
                boolean modificado = false;
                if (p.getImagen_url() != null && p.getImagen_url().contains("unsplash")) {
                    p.setImagen_url(null);
                    modificado = true;
                }
                if (p.getSpotify_album_id() != null && !p.getSpotify_album_id().isBlank()) {
                    String cleanId = SpotifyClientHelper.sanitizarAlbumId(p.getSpotify_album_id());
                    if (cleanId != null && !cleanId.equals(p.getSpotify_album_id())) {
                        p.setSpotify_album_id(cleanId);
                        modificado = true;
                    }
                }
                if (modificado) {
                    productoMusicaRepository.save(p);
                }
            }
        } catch (Exception e) {
            log.warn("Aviso al sanitizar registros de base de datos: {}", e.getMessage());
        }
    }
}
