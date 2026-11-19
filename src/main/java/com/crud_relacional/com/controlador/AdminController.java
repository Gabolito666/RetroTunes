package com.crud_relacional.com.controlador;

import com.crud_relacional.com.entidades.*;
import com.crud_relacional.com.repositorio.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    private final VentaRepository ventaRepository;
    private final AlquilerEquipoRepository alquilerEquipoRepository;
    private final ProductoMusicaRepository productoMusicaRepository;
    private final UsuarioRepository usuarioRepository;

    public AdminController(VentaRepository ventaRepository,
                           AlquilerEquipoRepository alquilerEquipoRepository,
                           ProductoMusicaRepository productoMusicaRepository,
                           UsuarioRepository usuarioRepository) {
        this.ventaRepository = ventaRepository;
        this.alquilerEquipoRepository = alquilerEquipoRepository;
        this.productoMusicaRepository = productoMusicaRepository;
        this.usuarioRepository = usuarioRepository;
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

        model.addAttribute("totalVentas", ventas);
        model.addAttribute("totalAlquileres", alquileres);
        model.addAttribute("ingresosTotales", ingresosTotales);
        model.addAttribute("ventasPorFormato", porFormato);
        model.addAttribute("alquileresActivos", activos);
        model.addAttribute("depositosCustodia", depositos);
        model.addAttribute("numProductos", numProductos);
        model.addAttribute("numUsuarios", numUsuarios);
        model.addAttribute("ultimasVentas", ultimasVentas);

        return "admin/dashboard";
    }

    @GetMapping("/reportes")
    public String reportes(HttpSession session, Model model) {
        if (!esAdmin(session)) {
            return "redirect:/login";
        }

        List<VentaPorTipoDto> ventasPorTipo = obtenerReporteVentasPorTipo();
        List<VentaPorUsuarioDto> ventasPorUsuario = obtenerReporteVentasPorUsuario();
        List<AlquilerReporteDto> alquileresReporte = obtenerReporteAlquileres();

        model.addAttribute("ventasPorTipo", ventasPorTipo);
        model.addAttribute("ventasPorUsuario", ventasPorUsuario);
        model.addAttribute("alquileresReporte", alquileresReporte);

        model.addAttribute("numProductos", productoMusicaRepository.count());
        model.addAttribute("numVentas", ventaRepository.count());
        model.addAttribute("numAlquileres", alquilerEquipoRepository.count());

        return "admin/reportes";
    }

    public List<VentaPorTipoDto> obtenerReporteVentasPorTipo() {
        Map<String, Long> cantidades = new LinkedHashMap<>();
        Map<String, BigDecimal> totales = new LinkedHashMap<>();
        Map<String, Set<Long>> transacciones = new LinkedHashMap<>();

        // Inicializar categorías estándar
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

        // Ordenar por total gastado descendente
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
                    a.getEquipo() != null ? a.getEquipo().getTitulo() : "Equipo",
                    a.getEquipo() != null ? a.getEquipo().getSku() : "EQP-N/A",
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
