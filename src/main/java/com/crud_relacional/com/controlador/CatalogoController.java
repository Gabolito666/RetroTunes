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

import java.util.List;
import java.util.Optional;

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
                         @RequestParam(value = "compraExitosa", required = false) String compraExitosa,
                         @RequestParam(value = "factura", required = false) String factura,
                         Model model) {
        List<ProductoMusica> productos;

        if (formato == null || formato.trim().isEmpty() || "TODOS".equalsIgnoreCase(formato)) {
            productos = productoMusicaRepository.findAll();
            formato = "TODOS";
        } else {
            productos = productoMusicaRepository.findByFormato(formato.toUpperCase());
        }

        model.addAttribute("productos", productos);
        model.addAttribute("formatoActual", formato.toUpperCase());
        if (creado != null) {
            model.addAttribute("mensajeExito", "Producto musical publicado exitosamente en el catálogo.");
        }
        if (compraExitosa != null && factura != null) {
            model.addAttribute("mensajeCompra", "¡Compra procesada con éxito! Factura generada: " + factura);
        }

        return "catalogo/listar";
    }

    @GetMapping("/{id}")
    public String verDetalle(@PathVariable("id") Long id, Model model) {
        Optional<ProductoMusica> opt = productoMusicaRepository.findById(id);
        if (opt.isEmpty()) {
            return "redirect:/catalogo";
        }
        ProductoMusica producto = opt.get();
        SpotifyClientHelper.AlbumDto album = spotifyClientHelper.obtenerAlbum(
                producto.getSpotify_album_id(),
                producto.getTitulo()
        );

        model.addAttribute("producto", producto);
        model.addAttribute("albumSpotify", album);
        return "catalogo/detalle";
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

    @PostMapping("/nuevo")
    public String procesarNuevo(@Valid @ModelAttribute("producto") ProductoMusica producto,
                                BindingResult bindingResult,
                                HttpSession session,
                                Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:/login";
        }

        String nombreRol = usuario.getRol() != null ? usuario.getRol().getNombre_rol() : "";
        if (!"ADMIN".equalsIgnoreCase(nombreRol) && !"DISCOGRAFICA".equalsIgnoreCase(nombreRol)) {
            return "redirect:/catalogo?errorPermisos=true";
        }

        if (producto.getSku() != null && productoMusicaRepository.existsBySku(producto.getSku().trim())) {
            bindingResult.rejectValue("sku", "error.producto", "El SKU ya se encuentra registrado en el inventario.");
        }

        if (bindingResult.hasErrors()) {
            return "catalogo/nuevo";
        }

        producto.setSku(producto.getSku().trim().toUpperCase());
        producto.setFormato(producto.getFormato().toUpperCase());
        productoMusicaRepository.save(producto);

        return "redirect:/catalogo?creado=true";
    }
}
