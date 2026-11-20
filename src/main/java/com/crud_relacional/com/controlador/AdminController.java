package com.crud_relacional.com.controlador;

import com.crud_relacional.com.entidades.*;
import com.crud_relacional.com.repositorio.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    public record VentaPorTipoDto(String formato, long cantidadVendida, BigDecimal totalRecaudado, long totalTransacciones) {}

    public record VentaPorUsuarioDto(Long idUsuario, String nombreCliente, String correo, String telefono, 
                                     int totalFacturas, int totalItems, BigDecimal totalGastado, String ultimaCompra) {}

    public record AlquilerReporteDto(Long idAlquiler, String codigo, String equipoTitulo, String equipoSku, 
                                     String clienteNombre, String clienteCorreo, LocalDate fechaInicio, LocalDate fechaFin, 
                                     long dias, BigDecimal montoAlquiler, BigDecimal depositoGarantia, String estado) {}

    public record DetalleCompraItemDto(
            String sku,
            String titulo,
            String formato,
            int cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal
    ) {}

    public record CompraIndividualClienteDto(
            Long idVenta,
            String codigoFactura,
            LocalDateTime fechaHora,
            String fechaFormateada,
            String metodoPago,
            BigDecimal total,
            int totalItems,
            List<DetalleCompraItemDto> items
    ) {}

    public record ClienteReporteComprasDto(
            Long idUsuario,
            String nombreCliente,
            String correo,
            String telefono,
            int totalFacturas,
            int totalItems,
            BigDecimal totalGastado,
            String ultimaCompra,
            List<CompraIndividualClienteDto> compras
    ) {}

    public record ProductoAdquiridoDto(
            String sku,
            String titulo,
            String formato,
            int cantidadTotal,
            BigDecimal montoTotal
    ) {}

    public record TopClienteReporteDto(
            int posicion,
            Long idUsuario,
            String nombreCliente,
            String correo,
            String telefono,
            int totalFacturas,
            int totalItems,
            BigDecimal totalGastado,
            String formatoFavorito,
            Map<String, Integer> formatosDesglose,
            List<ProductoAdquiridoDto> productosTop
    ) {}

    private final VentaRepository ventaRepository;
    private final AlquilerEquipoRepository alquilerEquipoRepository;
    private final ProductoMusicaRepository productoMusicaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ArtistaEmpresaRepository artistaEmpresaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(VentaRepository ventaRepository,
                           AlquilerEquipoRepository alquilerEquipoRepository,
                           ProductoMusicaRepository productoMusicaRepository,
                           UsuarioRepository usuarioRepository,
                           RolRepository rolRepository,
                           ArtistaEmpresaRepository artistaEmpresaRepository,
                           DetalleVentaRepository detalleVentaRepository,
                           PasswordEncoder passwordEncoder) {
        this.ventaRepository = ventaRepository;
        this.alquilerEquipoRepository = alquilerEquipoRepository;
        this.productoMusicaRepository = productoMusicaRepository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.artistaEmpresaRepository = artistaEmpresaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private boolean esAdmin(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            usuario = (Usuario) session.getAttribute("usuario");
        }
        return usuario != null && usuario.getRol() != null && "ADMIN".equalsIgnoreCase(usuario.getRol().getNombre_rol());
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        BigDecimal ventas = ventaRepository.sumTotalVentas();
        BigDecimal alquileres = alquilerEquipoRepository.sumTotalAlquileres();
        BigDecimal ingresosTotales = ventas.add(alquileres);

        List<Object[]> porFormato = ventaRepository.findVentasPorFormato();
        long activos = alquilerEquipoRepository.countAlquileresActivos();
        BigDecimal depositos = alquilerEquipoRepository.sumTotalDepositosCustodia();

        long numProductos = productoMusicaRepository.count();
        long numUsuarios = usuarioRepository.count();
        List<Venta> ultimasVentas = ventaRepository.findAll();

        String formatoMasVendido = "N/A";
        long formatoMasVendidoUds = 0;
        if (porFormato != null && !porFormato.isEmpty()) {
            for (Object[] row : porFormato) {
                String f = (String) row[0];
                if (!"EQUIPO_SONIDO".equalsIgnoreCase(f)) {
                    formatoMasVendido = f;
                    formatoMasVendidoUds = ((Number) row[1]).longValue();
                    break;
                }
            }
        }

        model.addAttribute("totalVentas", ventas);
        model.addAttribute("totalAlquileres", alquileres);
        model.addAttribute("ingresosTotales", ingresosTotales);
        model.addAttribute("ventasPorFormato", porFormato);
        model.addAttribute("formatoMasVendido", formatoMasVendido);
        model.addAttribute("formatoMasVendidoUds", formatoMasVendidoUds);
        model.addAttribute("alquileresActivos", activos);
        model.addAttribute("depositosCustodia", depositos);
        model.addAttribute("numProductos", numProductos);
        model.addAttribute("numUsuarios", numUsuarios);
        model.addAttribute("ultimasVentas", ultimasVentas);

        return "admin/dashboard";
    }

    @GetMapping("/usuarios")
    public String listarUsuariosAdmin(@RequestParam(value = "q", required = false) String busqueda,
                                      @RequestParam(value = "rol", required = false) String rolFiltro,
                                      HttpSession session,
                                      Model model) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        List<Usuario> usuarios = usuarioRepository.findAll();
        usuarios.sort(Comparator.comparing(Usuario::getId_usuario));

        if (busqueda != null && !busqueda.isBlank()) {
            String q = busqueda.trim().toLowerCase();
            usuarios = usuarios.stream().filter(u ->
                    (u.getNombre_completo() != null && u.getNombre_completo().toLowerCase().contains(q)) ||
                    (u.getCorreo() != null && u.getCorreo().toLowerCase().contains(q)) ||
                    (u.getTelefono() != null && u.getTelefono().toLowerCase().contains(q)) ||
                    (u.getRazon_social() != null && u.getRazon_social().toLowerCase().contains(q)) ||
                    (u.getNit() != null && u.getNit().toLowerCase().contains(q)) ||
                    (u.getCodigo_empleado() != null && u.getCodigo_empleado().toLowerCase().contains(q))
            ).toList();
            model.addAttribute("busqueda", busqueda.trim());
        }

        if (rolFiltro != null && !rolFiltro.isBlank() && !"TODOS".equalsIgnoreCase(rolFiltro)) {
            usuarios = usuarios.stream().filter(u ->
                    u.getRol() != null && rolFiltro.equalsIgnoreCase(u.getRol().getNombre_rol())
            ).toList();
            model.addAttribute("rolFiltro", rolFiltro.trim());
        }

        List<Rol> roles = rolRepository.findAll();
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("roles", roles);
        model.addAttribute("totalUsuarios", usuarios.size());

        return "admin/usuarios";
    }

    /**
     * Creación exclusiva de usuarios corporativos por Administrador (Opción 1).
     * Asigna directamente el rol 'DISCOGRAFICA' (con razonSocial y nit) o 'TECNICO_OPERACIONES' (con codigoEmpleado).
     * Cifra automáticamente la clave con BCryptPasswordEncoder.
     */
    @PostMapping("/usuarios/corporativo")
    public String crearUsuarioCorporativo(@RequestParam("nombre_completo") String nombreCompleto,
                                          @RequestParam("correo") String correo,
                                          @RequestParam("clave") String clave,
                                          @RequestParam(value = "telefono", required = false) String telefono,
                                          @RequestParam("rol") String rolNombre,
                                          @RequestParam(value = "razonSocial", required = false) String razonSocial,
                                          @RequestParam(value = "nit", required = false) String nit,
                                          @RequestParam(value = "codigoEmpleado", required = false) String codigoEmpleado,
                                          HttpSession session,
                                          RedirectAttributes redirectAttributes) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        if (nombreCompleto == null || nombreCompleto.trim().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "El nombre completo o contacto corporativo es obligatorio.");
            return "redirect:/admin/usuarios";
        }

        if (correo == null || correo.trim().isBlank() || !correo.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            redirectAttributes.addFlashAttribute("error", "El correo corporativo especificado no es válido.");
            return "redirect:/admin/usuarios";
        }

        if (usuarioRepository.existsByCorreo(correo.trim())) {
            redirectAttributes.addFlashAttribute("error", "El correo corporativo ya se encuentra registrado en el sistema.");
            return "redirect:/admin/usuarios";
        }

        if (clave == null || clave.trim().length() < 6) {
            redirectAttributes.addFlashAttribute("error", "La contraseña asignada debe tener al menos 6 caracteres.");
            return "redirect:/admin/usuarios";
        }

        String rolNorm = rolNombre != null ? rolNombre.trim().toUpperCase() : "";
        if (!"DISCOGRAFICA".equalsIgnoreCase(rolNorm) && !"TECNICO_OPERACIONES".equalsIgnoreCase(rolNorm)) {
            redirectAttributes.addFlashAttribute("error", "Rol corporativo inválido. Solo se permite DISCOGRAFICA o TECNICO_OPERACIONES.");
            return "redirect:/admin/usuarios";
        }

        if ("DISCOGRAFICA".equalsIgnoreCase(rolNorm)) {
            if (razonSocial == null || razonSocial.trim().isBlank()) {
                redirectAttributes.addFlashAttribute("error", "La Razón Social es requerida para el perfil Discográfica.");
                return "redirect:/admin/usuarios";
            }
            if (nit == null || nit.trim().isBlank()) {
                redirectAttributes.addFlashAttribute("error", "El NIT / Identificación Tributaria es requerido para el perfil Discográfica.");
                return "redirect:/admin/usuarios";
            }
        } else if ("TECNICO_OPERACIONES".equalsIgnoreCase(rolNorm)) {
            if (codigoEmpleado == null || codigoEmpleado.trim().isBlank()) {
                redirectAttributes.addFlashAttribute("error", "El Código de Empleado es requerido para el perfil Técnico de Operaciones.");
                return "redirect:/admin/usuarios";
            }
        }

        Rol rol = rolRepository.findByNombre_rol(rolNorm)
                .orElseGet(() -> rolRepository.save(new Rol(rolNorm)));

        Usuario usuario = new Usuario();
        usuario.setNombre_completo(nombreCompleto.trim());
        usuario.setCorreo(correo.trim());
        usuario.setClave_usuario(passwordEncoder.encode(clave.trim()));
        usuario.setTelefono(telefono != null ? telefono.trim() : null);
        usuario.setActivo(true);
        usuario.setRol(rol);

        if ("DISCOGRAFICA".equalsIgnoreCase(rolNorm)) {
            usuario.setRazon_social(razonSocial.trim());
            usuario.setNit(nit.trim());
        } else {
            usuario.setCodigo_empleado(codigoEmpleado.trim());
        }

        Usuario guardado = usuarioRepository.save(usuario);

        // Si es DISCOGRAFICA, sincronizar entidad ArtistaEmpresa para catálogo y regalías
        if ("DISCOGRAFICA".equalsIgnoreCase(rolNorm)) {
            ArtistaEmpresa discografica = new ArtistaEmpresa(
                    razonSocial.trim(),
                    "DISCOGRAFICA",
                    "Razón Social: " + razonSocial.trim() + " | NIT: " + nit.trim(),
                    null,
                    guardado
            );
            artistaEmpresaRepository.save(discografica);
        }

        redirectAttributes.addFlashAttribute("mensajeExito",
                "Usuario corporativo '" + guardado.getNombre_completo() + "' registrado exitosamente con rol " + rolNorm + " (Contraseña cifrada con BCrypt).");
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/editar/{id}")
    public String modificarUsuarioAdmin(@PathVariable("id") Long id,
                                        @RequestParam("nombre_completo") String nombreCompleto,
                                        @RequestParam("correo") String correo,
                                        @RequestParam(value = "telefono", required = false) String telefono,
                                        @RequestParam("id_rol") Long idRol,
                                        @RequestParam(value = "activo", defaultValue = "false") boolean activo,
                                        @RequestParam(value = "razon_social", required = false) String razonSocial,
                                        @RequestParam(value = "nit", required = false) String nit,
                                        @RequestParam(value = "codigo_empleado", required = false) String codigoEmpleado,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        Optional<Usuario> opt = usuarioRepository.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado.");
            return "redirect:/admin/usuarios";
        }

        Usuario usuario = opt.get();

        if (nombreCompleto == null || nombreCompleto.trim().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "El nombre completo es obligatorio.");
            return "redirect:/admin/usuarios";
        }

        if (correo == null || correo.trim().isBlank() || !correo.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            redirectAttributes.addFlashAttribute("error", "El formato de correo electrónico no es válido.");
            return "redirect:/admin/usuarios";
        }

        Optional<Usuario> usuarioConMismoCorreo = usuarioRepository.findByCorreo(correo.trim());
        if (usuarioConMismoCorreo.isPresent() && !usuarioConMismoCorreo.get().getId_usuario().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "El correo ya se encuentra registrado por otro usuario.");
            return "redirect:/admin/usuarios";
        }

        Optional<Rol> rolOpt = rolRepository.findById(idRol);
        if (rolOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El rol seleccionado no es válido.");
            return "redirect:/admin/usuarios";
        }

        usuario.setNombre_completo(nombreCompleto.trim());
        usuario.setCorreo(correo.trim());
        usuario.setTelefono(telefono != null ? telefono.trim() : null);
        usuario.setActivo(activo);
        usuario.setRol(rolOpt.get());

        if (razonSocial != null) usuario.setRazon_social(razonSocial.trim());
        if (nit != null) usuario.setNit(nit.trim());
        if (codigoEmpleado != null) usuario.setCodigo_empleado(codigoEmpleado.trim());

        usuarioRepository.save(usuario);

        redirectAttributes.addFlashAttribute("mensajeExito",
                "Usuario " + usuario.getCorreo() + " actualizado correctamente.");
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/toggle-estado/{id}")
    public String cambiarEstadoUsuarioAdmin(@PathVariable("id") Long id,
                                           HttpSession session,
                                           RedirectAttributes redirectAttributes) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        Optional<Usuario> opt = usuarioRepository.findById(id);
        if (opt.isPresent()) {
            Usuario usuario = opt.get();
            boolean nuevoEstado = usuario.getActivo() == null || !usuario.getActivo();
            usuario.setActivo(nuevoEstado);
            usuarioRepository.save(usuario);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Estado de " + usuario.getCorreo() + " cambiado a " + (nuevoEstado ? "ACTIVO" : "INACTIVO") + ".");
        }
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/facturas")
    public String facturasAdmin(@RequestParam(value = "q", required = false) String busqueda,
                                HttpSession session, 
                                Model model) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        List<Venta> todas = ventaRepository.findAll();
        todas.sort((a, b) -> b.getFecha_hora().compareTo(a.getFecha_hora()));

        if (busqueda != null && !busqueda.isBlank()) {
            String q = busqueda.trim().toLowerCase();
            todas = todas.stream().filter(v -> 
                (v.getCodigo_factura() != null && v.getCodigo_factura().toLowerCase().contains(q)) ||
                (v.getCliente() != null && v.getCliente().getNombre_completo() != null && v.getCliente().getNombre_completo().toLowerCase().contains(q)) ||
                (v.getCliente() != null && v.getCliente().getCorreo() != null && v.getCliente().getCorreo().toLowerCase().contains(q)) ||
                (v.getMetodo_pago() != null && v.getMetodo_pago().toLowerCase().contains(q))
            ).toList();
            model.addAttribute("busqueda", busqueda.trim());
        }

        BigDecimal sumaTotalFacturado = todas.stream()
                .map(v -> v.getMonto_total() != null ? v.getMonto_total() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItemsVendidos = 0;
        for (Venta v : todas) {
            if (v.getDetalles() != null) {
                totalItemsVendidos += v.getDetalles().stream().mapToInt(DetalleVenta::getCantidad).sum();
            }
        }

        model.addAttribute("facturas", todas);
        model.addAttribute("totalFacturas", todas.size());
        model.addAttribute("montoFacturado", sumaTotalFacturado);
        model.addAttribute("totalItemsVendidos", totalItemsVendidos);

        return "admin/facturas";
    }

    // =========================================================================
    // REESTRUCTURACIÓN COMPLETA DEL PANEL DE REPORTES ADMIN (4 SUBMENÚS/PESTAÑAS)
    // =========================================================================

    /**
     * Redirección por defecto a la primera pestaña de reportes: Calendario
     */
    @GetMapping("/reportes")
    public String reportes(HttpSession session) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }
        return "redirect:/admin/reportes/calendario";
    }

    /**
     * SUBMENÚ 1: CALENDARIO DE PRÉSTAMOS Y DEVOLUCIONES ('/admin/reportes/calendario')
     * Vista con minicalendario / cronograma visual que registra fechas de inicio, entrega y devolución
     * de los alquileres activos (equipos y productos musicales).
     * Muestra estados: RESERVADO, EN_USO, DEVUELTO y CON_NOVEDAD.
     */
    @GetMapping("/reportes/calendario")
    public String reporteCalendario(@RequestParam(value = "estado", required = false) String estadoFiltro,
                                    @RequestParam(value = "q", required = false) String busqueda,
                                    HttpSession session,
                                    Model model) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        List<AlquilerEquipo> alquileres = alquilerEquipoRepository.findAll();
        alquileres.sort((a, b) -> b.getFecha_inicio().compareTo(a.getFecha_inicio()));

        long totalAlquileres = alquileres.size();
        long reservados = alquileres.stream().filter(a -> "RESERVADO".equalsIgnoreCase(a.getEstado())).count();
        long enUso = alquileres.stream().filter(a -> "EN_USO".equalsIgnoreCase(a.getEstado())).count();
        long devueltos = alquileres.stream().filter(a -> "DEVUELTO".equalsIgnoreCase(a.getEstado())).count();
        long conNovedad = alquileres.stream().filter(a -> "CON_NOVEDAD".equalsIgnoreCase(a.getEstado())).count();
        BigDecimal depositosCustodia = alquileres.stream()
                .filter(a -> "RESERVADO".equalsIgnoreCase(a.getEstado()) || "EN_USO".equalsIgnoreCase(a.getEstado()))
                .map(a -> a.getDeposito_garantia() != null ? a.getDeposito_garantia() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (estadoFiltro != null && !estadoFiltro.isBlank() && !"TODOS".equalsIgnoreCase(estadoFiltro)) {
            alquileres = alquileres.stream().filter(a -> estadoFiltro.equalsIgnoreCase(a.getEstado())).toList();
            model.addAttribute("estadoFiltro", estadoFiltro.trim().toUpperCase());
        }

        if (busqueda != null && !busqueda.isBlank()) {
            String q = busqueda.trim().toLowerCase();
            alquileres = alquileres.stream().filter(a ->
                    (a.getCodigo_alquiler() != null && a.getCodigo_alquiler().toLowerCase().contains(q)) ||
                    (a.getEquipo() != null && a.getEquipo().getTitulo() != null && a.getEquipo().getTitulo().toLowerCase().contains(q)) ||
                    (a.getEquipo() != null && a.getEquipo().getSku() != null && a.getEquipo().getSku().toLowerCase().contains(q)) ||
                    (a.getCliente() != null && a.getCliente().getNombre_completo() != null && a.getCliente().getNombre_completo().toLowerCase().contains(q)) ||
                    (a.getCliente() != null && a.getCliente().getCorreo() != null && a.getCliente().getCorreo().toLowerCase().contains(q))
            ).toList();
            model.addAttribute("busqueda", busqueda.trim());
        }

        model.addAttribute("alquileres", alquileres);
        model.addAttribute("totalAlquileres", totalAlquileres);
        model.addAttribute("reservados", reservados);
        model.addAttribute("enUso", enUso);
        model.addAttribute("devueltos", devueltos);
        model.addAttribute("conNovedad", conNovedad);
        model.addAttribute("depositosCustodia", depositosCustodia);
        model.addAttribute("pestanaActiva", "calendario");

        return "admin/reportes/calendario";
    }

    /**
     * SUBMENÚ 2: CONTROL DE INVENTARIO CON FILTROS MULTICRITERIO ('/admin/reportes/inventario')
     * Tabla de stock dinámico con filtros JPQL por:
     * - Tipo de producto (Vinilo, Cassette, CD, Equipo).
     * - Artista / Grupo.
     * - Discográfica.
     * - Rango de cantidad/stock disponible.
     */
    @GetMapping("/reportes/inventario")
    public String reporteInventario(@RequestParam(value = "formato", required = false) String formato,
                                    @RequestParam(value = "artista", required = false) String artista,
                                    @RequestParam(value = "discografica", required = false) String discografica,
                                    @RequestParam(value = "stockMin", required = false) Integer stockMin,
                                    @RequestParam(value = "stockMax", required = false) Integer stockMax,
                                    HttpSession session,
                                    Model model) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        List<ProductoMusica> productos = productoMusicaRepository.filtrarInventarioReporte(
                formato, artista, discografica, stockMin, stockMax
        );

        long totalPiezas = productos.stream().mapToLong(p -> p.getStock() != null ? p.getStock() : 0).sum();
        BigDecimal valorizacionTotal = productos.stream()
                .map(p -> (p.getPrecio_venta() != null && p.getStock() != null)
                        ? p.getPrecio_venta().multiply(BigDecimal.valueOf(p.getStock()))
                        : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long bajoStock = productos.stream().filter(p -> p.getStock() != null && p.getStock() < 5).count();

        model.addAttribute("productos", productos);
        model.addAttribute("totalProductos", productos.size());
        model.addAttribute("totalPiezas", totalPiezas);
        model.addAttribute("valorizacionTotal", valorizacionTotal);
        model.addAttribute("bajoStock", bajoStock);

        model.addAttribute("formatoFiltro", formato);
        model.addAttribute("artistaFiltro", artista);
        model.addAttribute("discograficaFiltro", discografica);
        model.addAttribute("stockMinFiltro", stockMin);
        model.addAttribute("stockMaxFiltro", stockMax);
        model.addAttribute("pestanaActiva", "inventario");

        return "admin/reportes/inventario";
    }

    /**
     * SUBMENÚ 3: TOP CLIENTES COMPRADORES ('/admin/reportes/top-clientes')
     * Ranking JPQL con los usuarios con mayor volumen de compras e ingresos generados.
     * Desglose detallado de los productos y formatos más adquiridos por cada cliente.
     */
    @GetMapping("/reportes/top-clientes")
    public String reporteTopClientes(HttpSession session, Model model) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        List<Object[]> rawRanking = ventaRepository.rankingTopClientes();
        List<Venta> todasLasVentas = ventaRepository.findAll();

        Map<Long, List<Venta>> ventasPorCliente = new HashMap<>();
        for (Venta v : todasLasVentas) {
            if (v.getCliente() != null) {
                ventasPorCliente.computeIfAbsent(v.getCliente().getId_usuario(), k -> new ArrayList<>()).add(v);
            }
        }

        List<TopClienteReporteDto> ranking = new ArrayList<>();
        int pos = 1;

        for (Object[] row : rawRanking) {
            Long idUsuario = ((Number) row[0]).longValue();
            String nombre = (String) row[1];
            String correo = (String) row[2];
            String telefono = (String) row[3];
            int totalFacturas = ((Number) row[4]).intValue();
            BigDecimal totalGastado = row[5] != null ? new BigDecimal(row[5].toString()) : BigDecimal.ZERO;

            List<Venta> clienteVentas = ventasPorCliente.getOrDefault(idUsuario, Collections.emptyList());
            Map<String, Integer> desgloseFormatos = new LinkedHashMap<>();
            desgloseFormatos.put("VINILO", 0);
            desgloseFormatos.put("CASSETTE", 0);
            desgloseFormatos.put("CD", 0);
            desgloseFormatos.put("EQUIPO_SONIDO", 0);

            Map<String, ProductoAdquiridoDto> productosMap = new HashMap<>();
            int totalItems = 0;

            for (Venta v : clienteVentas) {
                if (v.getDetalles() == null) continue;
                for (DetalleVenta d : v.getDetalles()) {
                    totalItems += d.getCantidad();
                    String formato = (d.getProducto() != null && d.getProducto().getFormato() != null)
                            ? d.getProducto().getFormato().toUpperCase() : "OTROS";
                    desgloseFormatos.put(formato, desgloseFormatos.getOrDefault(formato, 0) + d.getCantidad());

                    String sku = (d.getProducto() != null && d.getProducto().getSku() != null) ? d.getProducto().getSku() : "N/A";
                    String titulo = (d.getProducto() != null && d.getProducto().getTitulo() != null) ? d.getProducto().getTitulo() : "Producto";
                    BigDecimal subtotal = d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO;

                    if (productosMap.containsKey(sku)) {
                        ProductoAdquiridoDto exist = productosMap.get(sku);
                        productosMap.put(sku, new ProductoAdquiridoDto(
                                sku,
                                titulo,
                                formato,
                                exist.cantidadTotal() + d.getCantidad(),
                                exist.montoTotal().add(subtotal)
                        ));
                    } else {
                        productosMap.put(sku, new ProductoAdquiridoDto(sku, titulo, formato, d.getCantidad(), subtotal));
                    }
                }
            }

            // Determinar formato favorito
            String formatoFav = "N/A";
            int maxCant = 0;
            for (Map.Entry<String, Integer> entry : desgloseFormatos.entrySet()) {
                if (entry.getValue() > maxCant) {
                    maxCant = entry.getValue();
                    formatoFav = entry.getKey() + " (" + maxCant + " uds)";
                }
            }

            List<ProductoAdquiridoDto> productosTop = new ArrayList<>(productosMap.values());
            productosTop.sort((a, b) -> Integer.compare(b.cantidadTotal(), a.cantidadTotal()));

            ranking.add(new TopClienteReporteDto(
                    pos++,
                    idUsuario,
                    nombre,
                    correo,
                    telefono != null ? telefono : "N/A",
                    totalFacturas,
                    totalItems,
                    totalGastado,
                    formatoFav,
                    desgloseFormatos,
                    productosTop
            ));
        }

        BigDecimal volumenTotalGenerado = ranking.stream()
                .map(TopClienteReporteDto::totalGastado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItemsVendidos = ranking.stream().mapToInt(TopClienteReporteDto::totalItems).sum();

        model.addAttribute("ranking", ranking);
        model.addAttribute("totalClientesCompradores", ranking.size());
        model.addAttribute("volumenTotalGenerado", volumenTotalGenerado);
        model.addAttribute("totalItemsVendidos", totalItemsVendidos);
        model.addAttribute("pestanaActiva", "top-clientes");

        return "admin/reportes/top-clientes";
    }

    /**
     * SUBMENÚ 4: HISTORIAL DETALLADO DE COMPRAS ('/admin/reportes/compras')
     * Registro de ventas individuales con filtros por:
     * - Usuario comprador.
     * - Producto vendido.
     * - Discográfica.
     * - Artista.
     * - Rango de cantidad de unidades.
     */
    @GetMapping("/reportes/compras")
    public String reporteComprasIndividuales(@RequestParam(value = "usuario", required = false) String usuario,
                                             @RequestParam(value = "producto", required = false) String producto,
                                             @RequestParam(value = "discografica", required = false) String discografica,
                                             @RequestParam(value = "artista", required = false) String artista,
                                             @RequestParam(value = "cantMin", required = false) Integer cantMin,
                                             @RequestParam(value = "cantMax", required = false) Integer cantMax,
                                             HttpSession session,
                                             Model model) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        List<DetalleVenta> detalles = detalleVentaRepository.filtrarComprasDetalladas(
                usuario, producto, discografica, artista, cantMin, cantMax
        );

        long totalUnidades = detalles.stream().mapToLong(d -> d.getCantidad() != null ? d.getCantidad() : 0).sum();
        BigDecimal totalFacturado = detalles.stream()
                .map(d -> d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("detalles", detalles);
        model.addAttribute("totalTransacciones", detalles.size());
        model.addAttribute("totalUnidades", totalUnidades);
        model.addAttribute("totalFacturado", totalFacturado);

        model.addAttribute("usuarioFiltro", usuario);
        model.addAttribute("productoFiltro", producto);
        model.addAttribute("discograficaFiltro", discografica);
        model.addAttribute("artistaFiltro", artista);
        model.addAttribute("cantMinFiltro", cantMin);
        model.addAttribute("cantMaxFiltro", cantMax);
        model.addAttribute("pestanaActiva", "compras");

        return "admin/reportes/compras";
    }

    // =========================================================================
    // EXPORTACIÓN EXCEL / PDF / WORD
    // =========================================================================

    public List<VentaPorTipoDto> obtenerReporteVentasPorTipo() {
        Map<String, Long> cantidades = new LinkedHashMap<>();
        Map<String, BigDecimal> totales = new LinkedHashMap<>();
        Map<String, Set<Long>> transacciones = new LinkedHashMap<>();

        List<String> formatos = List.of("VINILO", "CASSETTE", "CD", "EQUIPO_SONIDO");
        for (String f : formatos) {
            cantidades.put(f, 0L);
            totales.put(f, BigDecimal.ZERO);
            transacciones.put(f, new HashSet<>());
        }

        List<Venta> ventas = ventaRepository.findAll();
        for (Venta v : ventas) {
            if (v.getDetalles() == null) continue;
            for (DetalleVenta d : v.getDetalles()) {
                String fmt = (d.getProducto() != null && d.getProducto().getFormato() != null) 
                        ? d.getProducto().getFormato().toUpperCase() 
                        : "OTROS";

                cantidades.put(fmt, cantidades.getOrDefault(fmt, 0L) + d.getCantidad());
                totales.put(fmt, totales.getOrDefault(fmt, BigDecimal.ZERO).add(d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO));
                transacciones.computeIfAbsent(fmt, k -> new HashSet<>()).add(v.getId_venta());
            }
        }

        List<VentaPorTipoDto> resultado = new ArrayList<>();
        for (Map.Entry<String, Long> entry : cantidades.entrySet()) {
            resultado.add(new VentaPorTipoDto(
                    entry.getKey(),
                    entry.getValue(),
                    totales.getOrDefault(entry.getKey(), BigDecimal.ZERO),
                    transacciones.getOrDefault(entry.getKey(), Collections.emptySet()).size()
            ));
        }
        return resultado;
    }

    public List<VentaPorUsuarioDto> obtenerReporteVentasPorUsuario() {
        List<Venta> ventas = ventaRepository.findAll();
        Map<Long, List<Venta>> ventasPorCliente = new HashMap<>();

        for (Venta v : ventas) {
            if (v.getCliente() != null) {
                ventasPorCliente.computeIfAbsent(v.getCliente().getId_usuario(), k -> new ArrayList<>()).add(v);
            }
        }

        List<Usuario> usuarios = usuarioRepository.findAll();
        List<VentaPorUsuarioDto> resultado = new ArrayList<>();

        for (Usuario u : usuarios) {
            List<Venta> lista = ventasPorCliente.getOrDefault(u.getId_usuario(), Collections.emptyList());
            int numFacturas = lista.size();
            int totalItems = 0;
            BigDecimal totalGastado = BigDecimal.ZERO;
            String ultima = "Sin compras";

            if (!lista.isEmpty()) {
                lista.sort((a, b) -> b.getFecha_hora().compareTo(a.getFecha_hora()));
                ultima = lista.get(0).getFecha_hora().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                for (Venta v : lista) {
                    totalGastado = totalGastado.add(v.getMonto_total() != null ? v.getMonto_total() : BigDecimal.ZERO);
                    if (v.getDetalles() != null) {
                        for (DetalleVenta d : v.getDetalles()) {
                            totalItems += d.getCantidad();
                        }
                    }
                }
            }

            resultado.add(new VentaPorUsuarioDto(
                    u.getId_usuario(),
                    u.getNombre_completo(),
                    u.getCorreo(),
                    u.getTelefono() != null ? u.getTelefono() : "N/A",
                    numFacturas,
                    totalItems,
                    totalGastado,
                    ultima
            ));
        }

        resultado.sort((a, b) -> b.totalGastado().compareTo(a.totalGastado()));
        return resultado;
    }

    public List<AlquilerReporteDto> obtenerReporteAlquileres() {
        List<AlquilerEquipo> alquileres = alquilerEquipoRepository.findAll();
        alquileres.sort((a, b) -> b.getFecha_inicio().compareTo(a.getFecha_inicio()));

        List<AlquilerReporteDto> resultado = new ArrayList<>();
        for (AlquilerEquipo a : alquileres) {
            long dias = ChronoUnit.DAYS.between(a.getFecha_inicio(), a.getFecha_fin()) + 1;
            resultado.add(new AlquilerReporteDto(
                    a.getId_alquiler(),
                    a.getCodigo_alquiler(),
                    a.getEquipo() != null ? a.getEquipo().getTitulo() : "Equipo / Pieza Musical",
                    a.getEquipo() != null ? a.getEquipo().getSku() : "SKU-N/A",
                    a.getCliente() != null ? a.getCliente().getNombre_completo() : "Cliente General",
                    a.getCliente() != null ? a.getCliente().getCorreo() : "N/A",
                    a.getFecha_inicio(),
                    a.getFecha_fin(),
                    dias,
                    a.getMonto_alquiler() != null ? a.getMonto_alquiler() : BigDecimal.ZERO,
                    a.getDeposito_garantia() != null ? a.getDeposito_garantia() : BigDecimal.ZERO,
                    a.getEstado()
            ));
        }
        return resultado;
    }

    public List<ClienteReporteComprasDto> obtenerReporteComprasDetalladasPorCliente() {
        List<Venta> todasLasVentas = ventaRepository.findAll();
        Map<Long, List<Venta>> ventasPorCliente = new HashMap<>();

        for (Venta v : todasLasVentas) {
            if (v.getCliente() != null) {
                ventasPorCliente.computeIfAbsent(v.getCliente().getId_usuario(), k -> new ArrayList<>()).add(v);
            }
        }

        List<Usuario> usuarios = usuarioRepository.findAll();
        List<ClienteReporteComprasDto> resultado = new ArrayList<>();

        for (Usuario u : usuarios) {
            List<Venta> ventasCliente = ventasPorCliente.getOrDefault(u.getId_usuario(), new ArrayList<>());
            ventasCliente.sort((a, b) -> b.getFecha_hora().compareTo(a.getFecha_hora()));

            List<CompraIndividualClienteDto> comprasDto = new ArrayList<>();
            int totalItemsCliente = 0;
            BigDecimal totalGastadoCliente = BigDecimal.ZERO;
            String ultimaCompraStr = "Sin compras";

            if (!ventasCliente.isEmpty()) {
                ultimaCompraStr = ventasCliente.get(0).getFecha_hora().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }

            for (Venta v : ventasCliente) {
                totalGastadoCliente = totalGastadoCliente.add(v.getMonto_total() != null ? v.getMonto_total() : BigDecimal.ZERO);
                List<DetalleCompraItemDto> itemsDto = new ArrayList<>();
                int itemsVenta = 0;

                if (v.getDetalles() != null) {
                    for (DetalleVenta d : v.getDetalles()) {
                        String sku = (d.getProducto() != null && d.getProducto().getSku() != null) ? d.getProducto().getSku() : "N/A";
                        String titulo = (d.getProducto() != null && d.getProducto().getTitulo() != null) ? d.getProducto().getTitulo() : "Producto";
                        String formato = (d.getProducto() != null && d.getProducto().getFormato() != null) ? d.getProducto().getFormato() : "VINILO";
                        BigDecimal precio = d.getPrecio_unitario() != null ? d.getPrecio_unitario() : BigDecimal.ZERO;
                        BigDecimal subtotal = d.getSubtotal() != null ? d.getSubtotal() : BigDecimal.ZERO;

                        itemsVenta += d.getCantidad();
                        itemsDto.add(new DetalleCompraItemDto(sku, titulo, formato, d.getCantidad(), precio, subtotal));
                    }
                }

                totalItemsCliente += itemsVenta;
                String fechaFormateada = v.getFecha_hora().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                comprasDto.add(new CompraIndividualClienteDto(
                        v.getId_venta(),
                        v.getCodigo_factura(),
                        v.getFecha_hora(),
                        fechaFormateada,
                        v.getMetodo_pago() != null ? v.getMetodo_pago() : "TARJETA",
                        v.getMonto_total() != null ? v.getMonto_total() : BigDecimal.ZERO,
                        itemsVenta,
                        itemsDto
                ));
            }

            resultado.add(new ClienteReporteComprasDto(
                    u.getId_usuario(),
                    u.getNombre_completo(),
                    u.getCorreo(),
                    u.getTelefono() != null ? u.getTelefono() : "N/A",
                    ventasCliente.size(),
                    totalItemsCliente,
                    totalGastadoCliente,
                    ultimaCompraStr,
                    comprasDto
            ));
        }

        resultado.sort((a, b) -> b.totalGastado().compareTo(a.totalGastado()));
        return resultado;
    }

    @GetMapping("/reportes/excel")
    public void exportarExcel(@RequestParam(value = "tipo", required = false, defaultValue = "completo") String tipo,
                              HttpSession session, 
                              HttpServletResponse response) throws Exception {
        if (!esAdmin(session)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        byte[] excelBytes;
        String filename;

        switch (tipo.toLowerCase().trim()) {
            case "ventas-individuales":
                excelBytes = ReporteExcelHelper.generarReporteComprasIndividualesPorClienteExcel(obtenerReporteComprasDetalladasPorCliente());
                filename = "reporte-retrotunes-compras-individuales-clientes.xlsx";
                break;
            case "ventas-tipo":
                excelBytes = ReporteExcelHelper.generarReporteVentasPorTipoExcel(obtenerReporteVentasPorTipo());
                filename = "reporte-retrotunes-ventas-por-tipo.xlsx";
                break;
            case "ventas-usuario":
                excelBytes = ReporteExcelHelper.generarReporteVentasPorUsuarioExcel(obtenerReporteVentasPorUsuario());
                filename = "reporte-retrotunes-ventas-por-usuario.xlsx";
                break;
            case "alquileres":
                excelBytes = ReporteExcelHelper.generarReporteAlquileresExcel(obtenerReporteAlquileres());
                filename = "reporte-retrotunes-alquileres.xlsx";
                break;
            case "completo":
            default:
                excelBytes = ReporteExcelHelper.generarReporteConsolidadoCompleto(
                        obtenerReporteVentasPorTipo(),
                        obtenerReporteVentasPorUsuario(),
                        obtenerReporteAlquileres(),
                        productoMusicaRepository.findAll()
                );
                filename = "reporte-retrotunes-consolidado-general.xlsx";
                break;
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setContentLength(excelBytes.length);
        response.getOutputStream().write(excelBytes);
        response.getOutputStream().flush();
    }

    @GetMapping("/reportes/pdf")
    public void exportarPdf(@RequestParam(value = "tipo", required = false, defaultValue = "ventas-tipo") String tipo,
                            HttpSession session, 
                            HttpServletResponse response) throws Exception {
        if (!esAdmin(session)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        byte[] pdfBytes;
        String filename;

        switch (tipo.toLowerCase().trim()) {
            case "ventas-individuales":
                pdfBytes = ReportePdfHelper.generarReporteComprasDetalladasPorCliente(obtenerReporteComprasDetalladasPorCliente());
                filename = "reporte-compras-individuales-clientes.pdf";
                break;
            case "ventas-usuario":
                pdfBytes = ReportePdfHelper.generarReporteVentasPorUsuario(obtenerReporteVentasPorUsuario());
                filename = "reporte-ventas-por-usuario.pdf";
                break;
            case "alquileres":
                pdfBytes = ReportePdfHelper.generarReporteAlquileres(obtenerReporteAlquileres());
                filename = "reporte-alquileres-equipos.pdf";
                break;
            case "ventas-tipo":
            default:
                pdfBytes = ReportePdfHelper.generarReporteVentasPorTipo(obtenerReporteVentasPorTipo());
                filename = "reporte-ventas-por-tipo-producto.pdf";
                break;
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
        response.setContentLength(pdfBytes.length);
        response.getOutputStream().write(pdfBytes);
        response.getOutputStream().flush();
    }

    @GetMapping("/contrato-alquiler/{id}/docx")
    public void descargarContratoWord(@PathVariable("id") Long id,
                                      HttpSession session,
                                      HttpServletResponse response) throws Exception {
        if (!esAdmin(session)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        Optional<AlquilerEquipo> opt = alquilerEquipoRepository.findById(id);
        if (opt.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        byte[] wordBytes = DocumentoWordHelper.generarContratoAlquiler(opt.get());

        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=\"contrato-alquiler-" + opt.get().getCodigo_alquiler() + ".docx\"");
        response.setContentLength(wordBytes.length);
        response.getOutputStream().write(wordBytes);
        response.getOutputStream().flush();
    }
}
