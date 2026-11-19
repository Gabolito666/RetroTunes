package com.crud_relacional.com.controlador;

import com.crud_relacional.com.entidades.*;
import com.crud_relacional.com.repositorio.ProductoMusicaRepository;
import com.crud_relacional.com.repositorio.UsuarioRepository;
import com.crud_relacional.com.repositorio.VentaRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/compras")
public class VentaController {

    private static final String SESSION_CARRITO = "carritoCompras";

    private final ProductoMusicaRepository productoMusicaRepository;
    private final VentaRepository ventaRepository;
    private final UsuarioRepository usuarioRepository;

    public VentaController(ProductoMusicaRepository productoMusicaRepository,
                           VentaRepository ventaRepository,
                           UsuarioRepository usuarioRepository) {
        this.productoMusicaRepository = productoMusicaRepository;
        this.ventaRepository = ventaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, ItemCarrito> obtenerCarrito(HttpSession session) {
        Map<Long, ItemCarrito> carrito = (Map<Long, ItemCarrito>) session.getAttribute(SESSION_CARRITO);
        if (carrito == null) {
            carrito = new LinkedHashMap<>();
            session.setAttribute(SESSION_CARRITO, carrito);
        }
        return carrito;
    }

    private BigDecimal calcularTotal(Map<Long, ItemCarrito> carrito) {
        return carrito.values().stream()
                .map(ItemCarrito::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @GetMapping("/carrito")
    public String verCarrito(HttpSession session, Model model) {
        Map<Long, ItemCarrito> carrito = obtenerCarrito(session);
        model.addAttribute("items", carrito.values());
        model.addAttribute("total", calcularTotal(carrito));
        model.addAttribute("cantidadItems", carrito.values().stream().mapToInt(ItemCarrito::getCantidad).sum());
        return "compras/carrito";
    }

    @PostMapping("/carrito/agregar")
    public String agregarAlCarrito(@RequestParam("idProducto") Long idProducto,
                                   @RequestParam(value = "cantidad", defaultValue = "1") Integer cantidad,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        if (cantidad == null || cantidad <= 0) {
            redirectAttributes.addFlashAttribute("errorStock", "La cantidad debe ser mayor a cero.");
            return "redirect:/catalogo/" + idProducto;
        }

        Optional<ProductoMusica> optProducto = productoMusicaRepository.findById(idProducto);
        if (optProducto.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorStock", "El producto solicitado no existe.");
            return "redirect:/catalogo";
        }

        ProductoMusica producto = optProducto.get();
        Map<Long, ItemCarrito> carrito = obtenerCarrito(session);
        ItemCarrito existente = carrito.get(idProducto);
        int nuevaCantidad = (existente != null ? existente.getCantidad() : 0) + cantidad;

        if (producto.getStock() < nuevaCantidad) {
            redirectAttributes.addFlashAttribute("errorStock",
                    "Stock insuficiente para '" + producto.getTitulo() + "'. Disponibles: " + producto.getStock());
            return "redirect:/catalogo/" + idProducto;
        }

        if (existente != null) {
            existente.setCantidad(nuevaCantidad);
        } else {
            carrito.put(idProducto, new ItemCarrito(
                    producto.getId_producto(),
                    producto.getSku(),
                    producto.getTitulo(),
                    producto.getFormato(),
                    producto.getPrecio_venta(),
                    cantidad
            ));
        }

        redirectAttributes.addFlashAttribute("mensajeExito", "'" + producto.getTitulo() + "' agregado al carrito.");
        return "redirect:/compras/carrito";
    }

    @PostMapping("/carrito/actualizar")
    public String actualizarCantidad(@RequestParam("idProducto") Long idProducto,
                                     @RequestParam("cantidad") Integer cantidad,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        Map<Long, ItemCarrito> carrito = obtenerCarrito(session);
        if (cantidad == null || cantidad <= 0) {
            carrito.remove(idProducto);
            redirectAttributes.addFlashAttribute("mensajeExito", "Producto eliminado del carrito.");
            return "redirect:/compras/carrito";
        }

        Optional<ProductoMusica> optProducto = productoMusicaRepository.findById(idProducto);
        if (optProducto.isPresent()) {
            ProductoMusica producto = optProducto.get();
            if (producto.getStock() < cantidad) {
                redirectAttributes.addFlashAttribute("errorStock",
                        "Stock insuficiente. Solo quedan " + producto.getStock() + " unidades disponibles.");
            } else {
                ItemCarrito item = carrito.get(idProducto);
                if (item != null) {
                    item.setCantidad(cantidad);
                }
            }
        }
        return "redirect:/compras/carrito";
    }

    @PostMapping("/carrito/eliminar")
    public String eliminarDelCarrito(@RequestParam("idProducto") Long idProducto,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        Map<Long, ItemCarrito> carrito = obtenerCarrito(session);
        carrito.remove(idProducto);
        redirectAttributes.addFlashAttribute("mensajeExito", "Ítem eliminado del carrito.");
        return "redirect:/compras/carrito";
    }

    @GetMapping("/checkout")
    public String mostrarCheckout(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "Debes iniciar sesión para realizar la compra.");
            return "redirect:/login";
        }

        Map<Long, ItemCarrito> carrito = obtenerCarrito(session);
        if (carrito.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorStock", "Tu carrito de compras está vacío.");
            return "redirect:/compras/carrito";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("items", carrito.values());
        model.addAttribute("total", calcularTotal(carrito));
        return "compras/checkout";
    }

    @PostMapping("/checkout/procesar")
    @Transactional
    public String procesarCheckout(@RequestParam("metodoPago") String metodoPago,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        Usuario usuarioEnSesion = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioEnSesion == null) {
            return "redirect:/login";
        }

        Usuario cliente = usuarioRepository.findById(usuarioEnSesion.getId_usuario())
                .orElse(null);
        if (cliente == null) {
            return "redirect:/login";
        }

        Map<Long, ItemCarrito> carrito = obtenerCarrito(session);
        if (carrito.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorStock", "El carrito de compras está vacío.");
            return "redirect:/compras/carrito";
        }

        // Validación estricta de stock en base de datos antes de confirmar
        for (ItemCarrito item : carrito.values()) {
            ProductoMusica producto = productoMusicaRepository.findById(item.getIdProducto())
                    .orElse(null);
            if (producto == null || producto.getStock() < item.getCantidad()) {
                redirectAttributes.addFlashAttribute("errorStock",
                        "Stock insuficiente para '" + item.getTitulo() + "'. La compra no pudo completarse.");
                return "redirect:/compras/carrito";
            }
        }

        BigDecimal montoTotal = calcularTotal(carrito);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String codigoFactura = "FAC-RETRO-" + timestamp + "-" + (int)(Math.random() * 900 + 100);

        Venta venta = new Venta(
                codigoFactura,
                LocalDateTime.now(),
                montoTotal,
                metodoPago,
                cliente
        );

        // Descuento atómico de stock y creación de detalle
        for (ItemCarrito item : carrito.values()) {
            ProductoMusica producto = productoMusicaRepository.findById(item.getIdProducto()).get();
            producto.setStock(producto.getStock() - item.getCantidad());
            productoMusicaRepository.save(producto);

            DetalleVenta detalle = new DetalleVenta(
                    venta,
                    producto,
                    item.getCantidad(),
                    item.getPrecioUnitario(),
                    item.getSubtotal()
            );
            venta.agregarDetalle(detalle);
        }

        ventaRepository.save(venta);
        carrito.clear();
        session.removeAttribute(SESSION_CARRITO);

        redirectAttributes.addFlashAttribute("compraExitosa", true);
        redirectAttributes.addFlashAttribute("facturaGenerada", codigoFactura);
        redirectAttributes.addFlashAttribute("montoTotal", montoTotal);
        return "redirect:/catalogo?compraExitosa=true&factura=" + codigoFactura;
    }
}
