package com.crud_relacional.com.controlador;

import com.crud_relacional.com.entidades.ProductoMusica;
import com.crud_relacional.com.entidades.Usuario;
import com.crud_relacional.com.repositorio.ProductoMusicaRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/catalogo")
public class CatalogoController {

    private final ProductoMusicaRepository productoMusicaRepository;
    private final SpotifyClientHelper spotifyClientHelper;

    public CatalogoController(ProductoMusicaRepository productoMusicaRepository,
                              SpotifyClientHelper spotifyClientHelper) {
        this.productoMusicaRepository = productoMusicaRepository;
        this.spotifyClientHelper = spotifyClientHelper;
    }

    @GetMapping
    public String listar(@RequestParam(value = "formato", required = false, defaultValue = "TODOS") String formato,
                         @RequestParam(value = "creado", required = false) String creado,
                         @RequestParam(value = "eliminado", required = false) String eliminado,
                         @RequestParam(value = "compraExitosa", required = false) String compraExitosa,
                         @RequestParam(value = "factura", required = false) String factura,
                         Model model) {
        List<ProductoMusica> productos;
        String fmt = formato != null ? formato.trim().toUpperCase() : "TODOS";

        switch (fmt) {
            case "VINILO":
                productos = productoMusicaRepository.findByFormato("VINILO");
                break;
            case "CASSETTE":
                productos = productoMusicaRepository.findByFormato("CASSETTE");
                break;
            case "CD":
                productos = productoMusicaRepository.findByFormato("CD");
                break;
            case "EQUIPO_SONIDO":
                productos = productoMusicaRepository.findSoloEquipos();
                break;
            default:
                // SEPARACIÓN ESTRICTA: Exclusivamente formatos físicos de música (VINILO, CASSETTE, CD).
                // Queda estrictamente prohibido mostrar equipos de sonido en el menú del catálogo.
                productos = productoMusicaRepository.findSoloMusica();
                fmt = "TODOS";
                break;
        }

        model.addAttribute("productos", productos);
        model.addAttribute("formatoActual", fmt);
        if (creado != null) {
            model.addAttribute("mensajeExito", "Producto musical publicado exitosamente en el catálogo.");
        }
        if (eliminado != null) {
            model.addAttribute("mensajeExito", "Producto eliminado exitosamente del catálogo.");
        }
        if (compraExitosa != null && factura != null) {
            model.addAttribute("mensajeCompra", "¡Compra procesada con éxito! Factura generada: " + factura);
        }

        return "catalogo/listar";
    }

    @GetMapping("/vinilos")
    public String listarVinilos(@RequestParam(value = "creado", required = false) String creado,
                                @RequestParam(value = "eliminado", required = false) String eliminado,
                                Model model) {
        return listar("VINILO", creado, eliminado, null, null, model);
    }

    @GetMapping("/cassettes")
    public String listarCassettes(@RequestParam(value = "creado", required = false) String creado,
                                  @RequestParam(value = "eliminado", required = false) String eliminado,
                                  Model model) {
        return listar("CASSETTE", creado, eliminado, null, null, model);
    }

    @GetMapping("/cds")
    public String listarCds(@RequestParam(value = "creado", required = false) String creado,
                            @RequestParam(value = "eliminado", required = false) String eliminado,
                            Model model) {
        return listar("CD", creado, eliminado, null, null, model);
    }

    @GetMapping("/equipos")
    public String listarEquipos(@RequestParam(value = "creado", required = false) String creado,
                                @RequestParam(value = "eliminado", required = false) String eliminado,
                                Model model) {
        return listar("EQUIPO_SONIDO", creado, eliminado, null, null, model);
    }

    @GetMapping({"/detalle/{id}", "/{id}"})
    public String verDetalle(@PathVariable("id") Long id,
                             @RequestParam(value = "formato", required = false) String formatoOrigen,
                             @RequestParam(value = "actualizado", required = false) String actualizado,
                             Model model) {
        Optional<ProductoMusica> opt = productoMusicaRepository.findById(id);
        if (opt.isEmpty()) {
            return "redirect:/catalogo";
        }
        ProductoMusica producto = opt.get();

        // Preservación contextual de formato de origen para retorno navegacional
        String formatoRetorno = (formatoOrigen != null && !formatoOrigen.isBlank()) 
                ? formatoOrigen.trim().toUpperCase() 
                : producto.getFormato();

        String cleanId = SpotifyClientHelper.sanitizarAlbumId(producto.getSpotify_album_id());
        if (cleanId != null && !cleanId.equals(producto.getSpotify_album_id())) {
            producto.setSpotify_album_id(cleanId);
            try {
                productoMusicaRepository.save(producto);
            } catch (Exception ignored) {
            }
        }

        SpotifyClientHelper.AlbumDto album = spotifyClientHelper.obtenerAlbum(
                cleanId,
                producto.getTitulo(),
                producto.getFormato(),
                producto.getImagen_url()
        );

        String spotifyArtistName = album.artista();
        String spotifyCoverUrl = album.imagenUrl();

        // Eliminación estricta de imágenes de stock (Unsplash)
        if (producto.getImagen_url() != null && producto.getImagen_url().contains("unsplash")) {
            producto.setImagen_url(null);
        }

        // Si se extrajo una carátula oficial válida de Spotify y la entidad no la tiene o está desactualizada, persistirla
        if (spotifyCoverUrl != null && !spotifyCoverUrl.isBlank() && !spotifyCoverUrl.contains("unsplash")) {
            if (producto.getImagen_url() == null || !producto.getImagen_url().equals(spotifyCoverUrl)) {
                producto.setImagen_url(spotifyCoverUrl);
                try {
                    productoMusicaRepository.save(producto);
                } catch (Exception ignored) {
                }
            }
        }

        model.addAttribute("producto", producto);
        model.addAttribute("albumSpotify", album);
        model.addAttribute("spotifyArtistName", spotifyArtistName);
        model.addAttribute("spotifyCoverUrl", spotifyCoverUrl);
        model.addAttribute("formatoOrigen", formatoRetorno);
        if (actualizado != null) {
            model.addAttribute("mensajeExito", "Ficha técnica y metadatos del producto actualizados exitosamente.");
        }

        return "catalogo/detalle";
    }

    public static String extraerSpotifyAlbumId(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String trimmed = input.trim();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:album[/:]|^)([a-zA-Z0-9]{22})(?:[/?&#]|$)");
        java.util.regex.Matcher matcher = pattern.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1);
        }
        if (trimmed.matches("^[a-zA-Z0-9]{20,24}$")) {
            return trimmed;
        }
        return trimmed;
    }

    public static String extraerAppleMusicAnimatedArtwork(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String trimmed = input.trim();
        // 1. Extraer URL de video .mp4 si viene embebido en un parámetro o query string
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(https?://[^\\s\"'<>&=]+\\.mp4(?:\\?[^\\s\"'<>]*)?)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1);
        }
        // 2. Enlace directo de video .mp4 (Apple Music Artwork Finder, Apple CDN, etc.)
        if (trimmed.toLowerCase().endsWith(".mp4")) {
            return trimmed;
        }
        return trimmed;
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:/login";
        }

        String nombreRol = usuario.getRol() != null ? usuario.getRol().getNombre_rol() : "";
        if (!"ADMIN".equalsIgnoreCase(nombreRol) && !"DISCOGRAFICA".equalsIgnoreCase(nombreRol)) {
            return "redirect:/catalogo?errorPermisos=true";
        }

        model.addAttribute("producto", new ProductoMusica());
        return "catalogo/nuevo";
    }

    private String guardarFotoEquipo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }

        try {
            Path uploadsDir = Paths.get("uploads", "equipos");
            if (!Files.exists(uploadsDir)) {
                Files.createDirectories(uploadsDir);
            }

            String originalFilename = archivo.getOriginalFilename();
            String extension = ".jpg";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            }

            String fileName = "eqp-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10) + extension;
            Path destino = uploadsDir.resolve(fileName);
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            // Copia secundaria a static/img/equipos para redundancia en classpath
            try {
                Path staticDir = Paths.get("src", "main", "resources", "static", "img", "equipos");
                if (!Files.exists(staticDir)) {
                    Files.createDirectories(staticDir);
                }
                Files.copy(destino, staticDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

                Path targetDir = Paths.get("target", "classes", "static", "img", "equipos");
                if (Files.exists(targetDir.getParent())) {
                    if (!Files.exists(targetDir)) {
                        Files.createDirectories(targetDir);
                    }
                    Files.copy(destino, targetDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception ignored) {
            }

            return "/uploads/equipos/" + fileName;
        } catch (Exception e) {
            return null;
        }
    }

    @PostMapping("/nuevo")
    public String procesarNuevo(@Valid @ModelAttribute("producto") ProductoMusica producto,
                                BindingResult bindingResult,
                                @RequestParam(value = "imagenArchivo", required = false) MultipartFile imagenArchivo,
                                HttpSession session,
                                Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            usuario = (Usuario) session.getAttribute("usuario");
        }

        if (usuario == null) {
            return "redirect:/login";
        }

        String nombreRol = "";
        if (usuario.getRol() != null) {
            nombreRol = usuario.getRol().getNombre_rol() != null ? usuario.getRol().getNombre_rol() : usuario.getRol().getNombreRol();
        }
        if (!"ADMIN".equalsIgnoreCase(nombreRol) && !"DISCOGRAFICA".equalsIgnoreCase(nombreRol)) {
            return "redirect:/catalogo?errorPermisos=true";
        }

        // Generación automática del SKU en servidor si se deja en blanco, o validación si se especifica manualmente
        if (producto.getSku() == null || producto.getSku().isBlank()) {
            String nuevoSku;
            do {
                nuevoSku = ProductoMusica.generarSkuAutomatico(producto.getFormato());
            } while (productoMusicaRepository.existsBySku(nuevoSku));
            producto.setSku(nuevoSku);
        } else {
            String skuLimpio = producto.getSku().trim().toUpperCase();
            producto.setSku(skuLimpio);
            if (productoMusicaRepository.existsBySku(skuLimpio)) {
                bindingResult.rejectValue("sku", "error.producto", "El SKU ya se encuentra registrado en el inventario.");
            }
        }

        boolean esEquipoSonido = "EQUIPO_SONIDO".equalsIgnoreCase(producto.getFormato());

        if (esEquipoSonido) {
            // Los equipos de sonido son objetos físicos, no álbumes musicales
            producto.setSpotify_album_id(null);
            producto.setAnimatedArtworkUrl(null);
            producto.setAnimated_artwork_url(null);

            if (imagenArchivo != null && !imagenArchivo.isEmpty()) {
                String rutaSubida = guardarFotoEquipo(imagenArchivo);
                if (rutaSubida != null) {
                    producto.setImagenUrl(rutaSubida);
                    producto.setImagen_url(rutaSubida);
                }
            }
        } else {
            // Soporte y extracción de Animated Artwork (.mp4) de Apple Music para formatos musicales
            if (producto.getAnimatedArtworkUrl() != null && !producto.getAnimatedArtworkUrl().isBlank()) {
                producto.setAnimatedArtworkUrl(extraerAppleMusicAnimatedArtwork(producto.getAnimatedArtworkUrl()));
            } else if (producto.getAnimated_artwork_url() != null && !producto.getAnimated_artwork_url().isBlank()) {
                producto.setAnimatedArtworkUrl(extraerAppleMusicAnimatedArtwork(producto.getAnimated_artwork_url()));
            }

            // Soporte para enlaces de Spotify con extracción automática de carátula
            if (producto.getSpotify_album_id() != null && !producto.getSpotify_album_id().isBlank()) {
                String extractedId = extraerSpotifyAlbumId(producto.getSpotify_album_id());
                producto.setSpotify_album_id(extractedId);

                String caratulaApi = spotifyClientHelper.obtenerCaratula(extractedId);
                if (caratulaApi != null && !caratulaApi.isBlank() && !caratulaApi.contains("unsplash")) {
                    producto.setImagenUrl(caratulaApi);
                } else {
                    SpotifyClientHelper.AlbumDto dto = spotifyClientHelper.obtenerAlbum(extractedId, producto.getTitulo(), producto.getFormato(), null);
                    if (dto != null && dto.imagenUrl() != null && !dto.imagenUrl().isBlank() && !dto.imagenUrl().contains("unsplash")) {
                        producto.setImagenUrl(dto.imagenUrl());
                    }
                }
            }
        }

        if (bindingResult.hasErrors()) {
            return "catalogo/nuevo";
        }

        try {
            producto.setSku(producto.getSku().trim().toUpperCase());
            producto.setFormato(producto.getFormato().toUpperCase());
            productoMusicaRepository.save(producto);
        } catch (Exception e) {
            model.addAttribute("mensajeError", "Error al guardar el producto: " + e.getMessage());
            return "catalogo/nuevo";
        }

        return "redirect:/catalogo?creado=true";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable("id") Long id, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            usuario = (Usuario) session.getAttribute("usuario");
        }
        if (usuario == null) {
            return "redirect:/login";
        }

        String nombreRol = usuario.getRol() != null ? usuario.getRol().getNombre_rol() : "";
        if (!"ADMIN".equalsIgnoreCase(nombreRol) && !"DISCOGRAFICA".equalsIgnoreCase(nombreRol)) {
            return "redirect:/catalogo?errorPermisos=true";
        }

        Optional<ProductoMusica> opt = productoMusicaRepository.findById(id);
        if (opt.isEmpty()) {
            return "redirect:/catalogo";
        }

        model.addAttribute("producto", opt.get());
        return "catalogo/editar";
    }

    @PostMapping("/editar/{id}")
    public String procesarEdicion(@PathVariable("id") Long id,
                                  @Valid @ModelAttribute("producto") ProductoMusica producto,
                                  BindingResult bindingResult,
                                  @RequestParam(value = "imagenArchivo", required = false) MultipartFile imagenArchivo,
                                  HttpSession session,
                                  Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            usuario = (Usuario) session.getAttribute("usuario");
        }
        if (usuario == null) {
            return "redirect:/login";
        }

        String nombreRol = usuario.getRol() != null ? usuario.getRol().getNombre_rol() : "";
        if (!"ADMIN".equalsIgnoreCase(nombreRol) && !"DISCOGRAFICA".equalsIgnoreCase(nombreRol)) {
            return "redirect:/catalogo?errorPermisos=true";
        }

        if (bindingResult.hasErrors()) {
            return "catalogo/editar";
        }

        Optional<ProductoMusica> opt = productoMusicaRepository.findById(id);
        if (opt.isEmpty()) {
            return "redirect:/catalogo";
        }

        ProductoMusica existente = opt.get();
        existente.setTitulo(producto.getTitulo());
        existente.setFormato(producto.getFormato().toUpperCase());
        existente.setPrecio_venta(producto.getPrecio_venta());
        existente.setPrecio_alquiler_dia(producto.getPrecio_alquiler_dia());
        existente.setStock(producto.getStock());

        boolean esEquipoSonido = "EQUIPO_SONIDO".equalsIgnoreCase(existente.getFormato());

        if (esEquipoSonido) {
            // Objeto físico: Limpiar metadatos de streaming musical
            existente.setSpotify_album_id(null);
            existente.setAnimatedArtworkUrl(null);
            existente.setAnimated_artwork_url(null);

            if (imagenArchivo != null && !imagenArchivo.isEmpty()) {
                String rutaSubida = guardarFotoEquipo(imagenArchivo);
                if (rutaSubida != null) {
                    existente.setImagenUrl(rutaSubida);
                    existente.setImagen_url(rutaSubida);
                }
            } else if (producto.getImagen_url() != null && !producto.getImagen_url().isBlank()) {
                existente.setImagenUrl(producto.getImagen_url().trim());
                existente.setImagen_url(producto.getImagen_url().trim());
            }
        } else {
            // Soporte y extracción de Animated Artwork (.mp4) de Apple Music
            if (producto.getAnimatedArtworkUrl() != null && !producto.getAnimatedArtworkUrl().isBlank()) {
                existente.setAnimatedArtworkUrl(extraerAppleMusicAnimatedArtwork(producto.getAnimatedArtworkUrl()));
            } else if (producto.getAnimated_artwork_url() != null && !producto.getAnimated_artwork_url().isBlank()) {
                existente.setAnimatedArtworkUrl(extraerAppleMusicAnimatedArtwork(producto.getAnimated_artwork_url()));
            }

            if (producto.getSpotify_album_id() != null && !producto.getSpotify_album_id().isBlank()) {
                String extractedId = extraerSpotifyAlbumId(producto.getSpotify_album_id());
                existente.setSpotify_album_id(extractedId);

                String caratulaApi = spotifyClientHelper.obtenerCaratula(extractedId);
                if (caratulaApi != null && !caratulaApi.isBlank()) {
                    existente.setImagenUrl(caratulaApi);
                } else {
                    SpotifyClientHelper.AlbumDto dto = spotifyClientHelper.obtenerAlbum(extractedId, existente.getTitulo(), existente.getFormato(), null);
                    if (dto != null && dto.imagenUrl() != null && !dto.imagenUrl().isBlank()) {
                        existente.setImagenUrl(dto.imagenUrl());
                    }
                }
            }
        }

        productoMusicaRepository.save(existente);
        return "redirect:/catalogo/" + id + "?actualizado=true";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarProducto(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return "redirect:/login";
        }

        String nombreRol = usuario.getRol() != null ? usuario.getRol().getNombre_rol() : "";
        if (!"ADMIN".equalsIgnoreCase(nombreRol)) {
            return "redirect:/catalogo?errorPermisos=true";
        }

        Optional<ProductoMusica> opt = productoMusicaRepository.findById(id);
        if (opt.isPresent()) {
            try {
                productoMusicaRepository.delete(opt.get());
                redirectAttributes.addFlashAttribute("mensajeExito", "Producto eliminado exitosamente del catálogo.");
            } catch (Exception e) {
                ProductoMusica p = opt.get();
                p.setStock(0);
                productoMusicaRepository.save(p);
                redirectAttributes.addFlashAttribute("mensajeExito", "El producto cuenta con registros contables vinculados; su stock fue actualizado a 0.");
            }
        }
        return "redirect:/catalogo";
    }
}
