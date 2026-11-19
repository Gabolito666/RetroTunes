package com.crud_relacional.com.controlador;

import com.crud_relacional.com.entidades.*;
import com.crud_relacional.com.repositorio.ProductoMusicaRepository;
import com.crud_relacional.com.repositorio.UsuarioRepository;
import com.crud_relacional.com.repositorio.VentaRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /**
     * Endpoint oficial según requerimiento: GET '/compras/factura/pdf/{idVenta}'
     * Consulta la Venta y sus DetallesVenta en Supabase a través de 'VentaRepository'
     * y genera en memoria el PDF estructurado con OpenPDF.
     */
    @GetMapping(value = "/factura/pdf/{idVenta}", produces = MediaType.APPLICATION_PDF_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> descargarFacturaPdfPorIdVenta(@PathVariable("idVenta") Long idVenta) {
        Optional<Venta> optVenta = ventaRepository.findByIdConDetalles(idVenta);
        if (optVenta.isEmpty()) {
            optVenta = ventaRepository.findById(idVenta);
        }
        if (optVenta.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Venta venta = optVenta.get();
        try {
            byte[] pdfBytes = FacturaPdfHelper.generarFactura(venta);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.inline()
                    .filename("factura-" + venta.getCodigo_factura() + ".pdf")
                    .build());
            headers.setContentLength(pdfBytes.length);
            headers.setCacheControl(CacheControl.noCache().mustRevalidate());

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint complementario para descarga por ID: GET '/compras/factura/id/{id}/pdf'
     */
    @GetMapping(value = "/factura/id/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarFacturaPdfPorIdAlias(@PathVariable("id") Long id) {
        return descargarFacturaPdfPorIdVenta(id);
    }

    /**
     * Endpoint complementario para descarga por Código de Factura: GET '/compras/factura/{codigo}/pdf'
     */
    @GetMapping(value = "/factura/{codigo}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> descargarFacturaPdfPorCodigo(@PathVariable("codigo") String codigo) {
        Optional<Venta> optVenta = ventaRepository.findByCodigoFactura(codigo);
        if (optVenta.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Venta venta = optVenta.get();
        try {
            byte[] pdfBytes = FacturaPdfHelper.generarFactura(venta);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.inline()
                    .filename("factura-" + venta.getCodigo_factura() + ".pdf")
                    .build());
            headers.setContentLength(pdfBytes.length);
            headers.setCacheControl(CacheControl.noCache().mustRevalidate());

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Vista de Confirmación de Compra y Factura Generada
     */
    @GetMapping("/confirmacion/{idVenta}")
    @Transactional(readOnly = true)
    public String verConfirmacion(@PathVariable("idVenta") Long idVenta,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        Usuario usuarioLogueado = (Usuario) session.getAttribute("usuarioLogueado");
        Optional<Venta> optVenta = ventaRepository.findByIdConDetalles(idVenta);
        if (optVenta.isEmpty()) {
            optVenta = ventaRepository.findById(idVenta);
        }
        if (optVenta.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorStock", "La orden especificada no existe en la base de datos.");
            return "redirect:/catalogo";
        }

        Venta venta = optVenta.get();

        // Control de permisos si hay sesión activa
        if (usuarioLogueado != null && !"ADMIN".equalsIgnoreCase(usuarioLogueado.getRol().getNombre_rol())) {
            if (venta.getCliente() != null && !venta.getCliente().getId_usuario().equals(usuarioLogueado.getId_usuario())) {
                redirectAttributes.addFlashAttribute("errorStock", "No tienes permisos para visualizar esta orden de compra.");
                return "redirect:/catalogo";
            }
        }

        BigDecimal total = venta.getMonto_total() != null ? venta.getMonto_total() : BigDecimal.ZERO;
        BigDecimal base = total.divide(new BigDecimal("1.19"), 2, RoundingMode.HALF_UP);
        BigDecimal iva = total.subtract(base);

        model.addAttribute("venta", venta);
        model.addAttribute("detalles", venta.getDetalles());
        model.addAttribute("subtotalBase", base);
        model.addAttribute("iva", iva);
        model.addAttribute("total", total);

        return "compras/confirmacion";
    }

    /**
     * Vista de Historial de Compras del Cliente
     */
    @GetMapping({"/historial", "/mis-compras"})
    @Transactional(readOnly = true)
    public String verHistorialCompras(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Usuario usuarioEnSesion = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioEnSesion == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "Debes iniciar sesión para consultar tu historial de compras.");
            return "redirect:/login";
        }

        Usuario cliente = usuarioRepository.findById(usuarioEnSesion.getId_usuario()).orElse(null);
        if (cliente == null) {
            return "redirect:/login";
        }

        List<Venta> compras = ventaRepository.findByClienteConDetallesOrderByFechaHoraDesc(cliente);
        if (compras == null || compras.isEmpty()) {
            compras = ventaRepository.findByCliente(cliente);
            if (compras != null) {
                compras.sort((a, b) -> b.getFecha_hora().compareTo(a.getFecha_hora()));
            }
        }

        model.addAttribute("usuario", cliente);
        model.addAttribute("compras", compras != null ? compras : Collections.emptyList());
        return "compras/historial";
    }

    @GetMapping("/carrito")
    public String verCarrito(HttpSession session, Model model) {
        Map<Long, ItemCarrito> carrito = obtenerCarrito(session);
        model.addAttribute("items", carrito.values());
        model.addAttribute("total", calcularTotal(carrito));
        model.addAttribute("cantidadItems", carrito.values().stream().mapToInt(ItemCarrito::getCantidad).sum());

        List<ProductoMusica> sugeridos = productoMusicaRepository.findAll().stream()
                .filter(p -> p.getStock() != null && p.getStock() > 0 && !carrito.containsKey(p.getId_producto()))
                .limit(6)
                .toList();
        model.addAttribute("sugeridos", sugeridos);

        return "compras/carrito";
    }

    @PostMapping(value = "/carrito/agregar-ajax", produces = "application/json")
    @ResponseBody
    public Map<String, Object> agregarAlCarritoAjax(@RequestParam("idProducto") Long idProducto,
                                                   @RequestParam(value = "cantidad", defaultValue = "1") Integer cantidad,
                                                   HttpSession session) {
        Map<String, Object> resp = new HashMap<>();

        // Bloqueo de compras para Administradores
        Usuario usuarioEnSesion = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioEnSesion != null && usuarioEnSesion.getRol() != null && "ADMIN".equalsIgnoreCase(usuarioEnSesion.getRol().getNombre_rol())) {
            resp.put("success", false);
            resp.put("mensaje", "Acción restringida: El perfil Administrador no puede realizar compras.");
            return resp;
        }

        if (cantidad == null || cantidad <= 0) {
            resp.put("success", false);
            resp.put("mensaje", "La cantidad debe ser mayor a cero.");
            return resp;
        }

        Optional<ProductoMusica> optProducto = productoMusicaRepository.findById(idProducto);
        if (optProducto.isEmpty()) {
            resp.put("success", false);
            resp.put("mensaje", "El producto solicitado no existe.");
            return resp;
        }

        ProductoMusica producto = optProducto.get();
        Map<Long, ItemCarrito> carrito = obtenerCarrito(session);
        ItemCarrito existente = carrito.get(idProducto);
        int nuevaCantidad = (existente != null ? existente.getCantidad() : 0) + cantidad;

        if (producto.getStock() < nuevaCantidad) {
            resp.put("success", false);
            resp.put("mensaje", "Stock insuficiente para '" + producto.getTitulo() + "'. Disponibles: " + producto.getStock());
            return resp;
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

        int totalItems = carrito.values().stream().mapToInt(ItemCarrito::getCantidad).sum();
        BigDecimal subtotal = calcularTotal(carrito);

        resp.put("success", true);
        resp.put("mensaje", "¡" + producto.getTitulo() + " (" + producto.getFormato() + ") añadido al carrito!");
        resp.put("totalItems", totalItems);
        resp.put("subtotal", subtotal);
        resp.put("productoTitulo", producto.getTitulo());
        return resp;
    }

    @PostMapping("/carrito/incrementar")
    public String incrementarCantidad(@RequestParam("idProducto") Long idProducto,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        Map<Long, ItemCarrito> carrito = obtenerCarrito(session);
        ItemCarrito item = carrito.get(idProducto);
        if (item == null) {
            return "redirect:/compras/carrito";
        }

        Optional<ProductoMusica> optProducto = productoMusicaRepository.findById(idProducto);
        if (optProducto.isPresent()) {
            ProductoMusica producto = optProducto.get();
            if (producto.getStock() <= item.getCantidad()) {
                redirectAttributes.addFlashAttribute("errorStock",
                        "Stock límite alcanzado para '" + producto.getTitulo() + "'. Máximo disponible: " + producto.getStock());
            } else {
                item.setCantidad(item.getCantidad() + 1);
            }
        }
        return "redirect:/compras/carrito";
    }

    @PostMapping("/carrito/decrementar")
    public String decrementarCantidad(@RequestParam("idProducto") Long idProducto,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        Map<Long, ItemCarrito> carrito = obtenerCarrito(session);
        ItemCarrito item = carrito.get(idProducto);
        if (item == null) {
            return "redirect:/compras/carrito";
        }

        if (item.getCantidad() > 1) {
            item.setCantidad(item.getCantidad() - 1);
        } else {
            carrito.remove(idProducto);
            redirectAttributes.addFlashAttribute("mensajeExito", "Producto eliminado del carrito.");
        }
        return "redirect:/compras/carrito";
    }

    @PostMapping("/carrito/agregar")
    public String agregarAlCarrito(@RequestParam("idProducto") Long idProducto,
                                   @RequestParam(value = "cantidad", defaultValue = "1") Integer cantidad,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        Usuario usuarioEnSesion = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioEnSesion != null && usuarioEnSesion.getRol() != null && "ADMIN".equalsIgnoreCase(usuarioEnSesion.getRol().getNombre_rol())) {
            redirectAttributes.addFlashAttribute("errorStock", "Acción restringida: Los administradores no pueden agregar productos al carrito.");
            return "redirect:/catalogo/" + idProducto;
        }

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

        if (usuario.getRol() != null && "ADMIN".equalsIgnoreCase(usuario.getRol().getNombre_rol())) {
            redirectAttributes.addFlashAttribute("errorStock", "Acción restringida: Los administradores tienen bloqueada la función de compra.");
            return "redirect:/catalogo";
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

        if (cliente.getRol() != null && "ADMIN".equalsIgnoreCase(cliente.getRol().getNombre_rol())) {
            redirectAttributes.addFlashAttribute("errorStock", "Acción denegada: El perfil Administrador tiene bloqueada la operación de compra.");
            return "redirect:/catalogo";
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

        Venta ventaGuardada = ventaRepository.save(venta);
        carrito.clear();
        session.removeAttribute(SESSION_CARRITO);

        redirectAttributes.addFlashAttribute("compraExitosa", true);
        redirectAttributes.addFlashAttribute("facturaGenerada", codigoFactura);
        redirectAttributes.addFlashAttribute("montoTotal", montoTotal);
        
        // Redirección inmediata a la vista de confirmación oficial
        return "redirect:/compras/confirmacion/" + ventaGuardada.getId_venta();
    }
}
