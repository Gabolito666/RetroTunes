package com.crud_relacional.com.controlador;

import com.crud_relacional.com.entidades.ArtistaEmpresa;
import com.crud_relacional.com.entidades.ProductoMusica;
import com.crud_relacional.com.entidades.Usuario;
import com.crud_relacional.com.entidades.Venta;
import com.crud_relacional.com.repositorio.ArtistaEmpresaRepository;
import com.crud_relacional.com.repositorio.ProductoMusicaRepository;
import com.crud_relacional.com.repositorio.VentaRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Controller
@RequestMapping("/artista")
public class ArtistaController {

    private final VentaRepository ventaRepository;
    private final ProductoMusicaRepository productoMusicaRepository;
    private final ArtistaEmpresaRepository artistaEmpresaRepository;
    private final SpotifyClientHelper spotifyClientHelper;

    public ArtistaController(VentaRepository ventaRepository,
                             ProductoMusicaRepository productoMusicaRepository,
                             ArtistaEmpresaRepository artistaEmpresaRepository,
                             SpotifyClientHelper spotifyClientHelper) {
        this.ventaRepository = ventaRepository;
        this.productoMusicaRepository = productoMusicaRepository;
        this.artistaEmpresaRepository = artistaEmpresaRepository;
        this.spotifyClientHelper = spotifyClientHelper;
    }

    private boolean tienePermisoArtista(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            usuario = (Usuario) session.getAttribute("usuario");
        }
        if (usuario == null || usuario.getRol() == null) return false;
        String rol = usuario.getRol().getNombre_rol();
        return "ARTISTA".equalsIgnoreCase(rol) || "DISCOGRAFICA".equalsIgnoreCase(rol) || "ADMIN".equalsIgnoreCase(rol);
    }

    private Usuario obtenerUsuarioActual(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            usuario = (Usuario) session.getAttribute("usuario");
        }
        return usuario;
    }

    private ArtistaEmpresa obtenerOCrearArtistaEmpresa(Usuario usuario) {
        return artistaEmpresaRepository.findByUsuario(usuario).orElseGet(() -> {
            String tipo = (usuario.getRol() != null && "DISCOGRAFICA".equalsIgnoreCase(usuario.getRol().getNombre_rol()))
                    ? "DISCOGRAFICA" : "ARTISTA";
            ArtistaEmpresa nueva = new ArtistaEmpresa(
                    usuario.getNombre_completo(),
                    tipo,
                    "Perfil artístico registrado en RetroTunes Archivo Analógico.",
                    null,
                    usuario
            );
            return artistaEmpresaRepository.save(nueva);
        });
    }

    @GetMapping("/propuestas")
    public String listarPropuestas(HttpSession session, Model model) {
        if (!tienePermisoArtista(session)) {
            return "redirect:/login";
        }

        Usuario usuario = obtenerUsuarioActual(session);
        ArtistaEmpresa artistaEmpresa = obtenerOCrearArtistaEmpresa(usuario);

        List<ProductoMusica> misPropuestas = productoMusicaRepository.findByArtista(artistaEmpresa);

        model.addAttribute("usuario", usuario);
        model.addAttribute("artistaEmpresa", artistaEmpresa);
        model.addAttribute("propuestas", misPropuestas);
        model.addAttribute("totalPropuestas", misPropuestas.size());

        return "artista/propuestas";
    }

    @PostMapping("/propuestas")
    public String enviarPropuesta(@RequestParam("titulo") String titulo,
                                  @RequestParam("formato") String formato,
                                  @RequestParam("precio_venta") BigDecimal precioVenta,
                                  @RequestParam(value = "stock_propuesto", defaultValue = "50") Integer stockPropuesto,
                                  @RequestParam(value = "spotify_album_id", required = false) String spotifyAlbumId,
                                  @RequestParam(value = "imagen_url", required = false) String imagenUrl,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        if (!tienePermisoArtista(session)) {
            return "redirect:/login";
        }

        if (titulo == null || titulo.trim().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "El título del álbum o lanzamiento es obligatorio.");
            return "redirect:/artista/propuestas";
        }

        if (formato == null || (!"VINILO".equalsIgnoreCase(formato) && !"CASSETTE".equalsIgnoreCase(formato) && !"CD".equalsIgnoreCase(formato))) {
            redirectAttributes.addFlashAttribute("error", "El formato físico debe ser Vinilo, Cassette o CD.");
            return "redirect:/artista/propuestas";
        }

        if (precioVenta == null || precioVenta.compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("error", "El precio de venta debe ser mayor a cero.");
            return "redirect:/artista/propuestas";
        }

        Usuario usuario = obtenerUsuarioActual(session);
        ArtistaEmpresa artistaEmpresa = obtenerOCrearArtistaEmpresa(usuario);

        String cleanId = SpotifyClientHelper.sanitizarAlbumId(spotifyAlbumId);
        String cover = imagenUrl;
        String fecha = null;

        if (cleanId != null && !cleanId.isBlank()) {
            if (cover == null || cover.isBlank()) {
                cover = spotifyClientHelper.obtenerCaratula(cleanId);
            }
            fecha = spotifyClientHelper.obtenerFechaLanzamiento(cleanId);
        }

        ProductoMusica propuesta = new ProductoMusica();
        propuesta.setTitulo(titulo.trim());
        propuesta.setFormato(formato.trim().toUpperCase());
        propuesta.setPrecio_venta(precioVenta);
        propuesta.setPrecio_alquiler_dia(BigDecimal.ZERO);
        propuesta.setStock(stockPropuesto != null && stockPropuesto > 0 ? stockPropuesto : 50);
        propuesta.setSpotify_album_id(cleanId);
        propuesta.setImagen_url(cover);
        propuesta.setFecha_lanzamiento(fecha);

        // REQUISITO SDD: Registro con status 'PENDIENTE_APROBACION' asociado a su 'id_artista'
        propuesta.setEstado("PENDIENTE_APROBACION");
        propuesta.setArtista(artistaEmpresa);

        productoMusicaRepository.save(propuesta);

        redirectAttributes.addFlashAttribute("mensajeExito",
                "Propuesta '" + propuesta.getTitulo() + "' registrada con éxito bajo estado PENDIENTE_APROBACION (SKU: " + propuesta.getSku() + ").");
        return "redirect:/artista/propuestas";
    }

    @GetMapping("/liquidacion")
    public String liquidacion(HttpSession session, Model model) {
        if (!tienePermisoArtista(session)) {
            return "redirect:/login";
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        BigDecimal totalVentas = ventaRepository.sumTotalVentas();

        // 70% neto para creadores / 30% administración y custodia física
        BigDecimal regalia70 = totalVentas.multiply(new BigDecimal("0.70")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tienda30 = totalVentas.subtract(regalia70);

        model.addAttribute("usuario", usuario);
        model.addAttribute("totalVentas", totalVentas);
        model.addAttribute("regalia70", regalia70);
        model.addAttribute("tienda30", tienda30);

        return "artista/liquidacion";
    }

    @GetMapping("/liquidacion/docx")
    public void descargarLiquidacionWord(HttpSession session, HttpServletResponse response) throws Exception {
        if (!tienePermisoArtista(session)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        BigDecimal totalVentas = ventaRepository.sumTotalVentas();
        BigDecimal regalia70 = totalVentas.multiply(new BigDecimal("0.70")).setScale(2, RoundingMode.HALF_UP);

        byte[] wordBytes = DocumentoWordHelper.generarLiquidacionRegalias(usuario, totalVentas, regalia70);

        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=\"liquidacion-regalias-" + usuario.getId_usuario() + ".docx\"");
        response.setContentLength(wordBytes.length);
        response.getOutputStream().write(wordBytes);
        response.getOutputStream().flush();
    }

    @GetMapping("/estado-cuenta/docx")
    public void descargarEstadoCuentaWord(HttpSession session, HttpServletResponse response) throws Exception {
        if (!tienePermisoArtista(session)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        Usuario usuario = obtenerUsuarioActual(session);
        List<Venta> todasVentas = ventaRepository.findAll();
        BigDecimal totalVentas = ventaRepository.sumTotalVentas();
        BigDecimal regalia70 = totalVentas.multiply(new BigDecimal("0.70")).setScale(2, RoundingMode.HALF_UP);

        byte[] docxBytes = DocumentoWordHelper.generarEstadoDeCuentaWord(usuario, todasVentas, totalVentas, regalia70);

        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=\"estado-cuenta-ventas-" + usuario.getId_usuario() + ".docx\"");
        response.setContentLength(docxBytes.length);
        response.getOutputStream().write(docxBytes);
        response.getOutputStream().flush();
    }
}
