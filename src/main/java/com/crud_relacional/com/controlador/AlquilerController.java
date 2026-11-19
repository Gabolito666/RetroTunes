package com.crud_relacional.com.controlador;

import com.crud_relacional.com.entidades.AlquilerEquipo;
import com.crud_relacional.com.entidades.ProductoMusica;
import com.crud_relacional.com.entidades.Usuario;
import com.crud_relacional.com.repositorio.AlquilerEquipoRepository;
import com.crud_relacional.com.repositorio.ProductoMusicaRepository;
import com.crud_relacional.com.repositorio.UsuarioRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/alquileres")
public class AlquilerController {

    private final AlquilerEquipoRepository alquilerEquipoRepository;
    private final ProductoMusicaRepository productoMusicaRepository;
    private final UsuarioRepository usuarioRepository;

    public AlquilerController(AlquilerEquipoRepository alquilerEquipoRepository,
                              ProductoMusicaRepository productoMusicaRepository,
                              UsuarioRepository usuarioRepository) {
        this.alquilerEquipoRepository = alquilerEquipoRepository;
        this.productoMusicaRepository = productoMusicaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Mapeo estricto para GET /alquileres y GET /alquileres/reserva
     * Filtra EXCLUSIVAMENTE productos de categoría equipo ('EQUIPO_SONIDO')
     */
    @GetMapping({"", "/reserva"})
    public String mostrarFormularioReserva(@RequestParam(value = "equipoId", required = false) Long equipoId,
                                           HttpSession session,
                                           Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            usuario = (Usuario) session.getAttribute("usuario");
        }

        boolean esAdmin = usuario != null && usuario.getRol() != null && 
                ("ADMIN".equalsIgnoreCase(usuario.getRol().getNombre_rol()) || "ADMIN".equalsIgnoreCase(usuario.getRol().getNombreRol()));

        // SEPARACIÓN ESTRICTA: Solo equipos de sonido, prohibido vinilos, cassettes o CDs
        List<ProductoMusica> equipos = productoMusicaRepository.findSoloEquipos();
        model.addAttribute("equipos", equipos);
        model.addAttribute("equipoSeleccionadoId", equipoId);
        model.addAttribute("fechaMinima", LocalDate.now());
        model.addAttribute("esAdmin", esAdmin);
        model.addAttribute("usuarioAutenticado", usuario != null);

        return "alquileres/reserva";
    }

    @PostMapping("/reserva")
    public String procesarReserva(@RequestParam("idEquipo") Long idEquipo,
                                  @RequestParam("fechaInicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                                  @RequestParam("fechaFin") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        Usuario usuarioEnSesion = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioEnSesion == null) {
            usuarioEnSesion = (Usuario) session.getAttribute("usuario");
        }
        if (usuarioEnSesion == null) {
            redirectAttributes.addFlashAttribute("errorAlquiler", "Inicia sesión para formalizar la reserva.");
            return "redirect:/login";
        }

        Usuario cliente = usuarioRepository.findById(usuarioEnSesion.getId_usuario()).orElse(null);
        if (cliente == null) {
            return "redirect:/login";
        }

        // Bloqueo estricto a nivel de backend para ADMIN: No puede formalizar reservas de alquiler
        if (cliente.getRol() != null && ("ADMIN".equalsIgnoreCase(cliente.getRol().getNombre_rol()) || "ADMIN".equalsIgnoreCase(cliente.getRol().getNombreRol()))) {
            redirectAttributes.addFlashAttribute("errorAlquiler", "Acción restringida: El perfil Administrador supervisa e inspecciona el inventario; las reservas de alquiler están destinadas a clientes.");
            return "redirect:/alquileres";
        }

        Optional<ProductoMusica> optEquipo = productoMusicaRepository.findById(idEquipo);
        if (optEquipo.isEmpty() || !"EQUIPO_SONIDO".equalsIgnoreCase(optEquipo.get().getFormato())) {
            redirectAttributes.addFlashAttribute("errorAlquiler", "El artículo seleccionado no corresponde a un equipo de sonido para alquiler.");
            return "redirect:/alquileres";
        }

        ProductoMusica equipo = optEquipo.get();

        if (fechaInicio == null || fechaFin == null) {
            redirectAttributes.addFlashAttribute("errorAlquiler", "Debes especificar las fechas de inicio y fin.");
            return "redirect:/alquileres/reserva?equipoId=" + idEquipo;
        }

        if (fechaInicio.isBefore(LocalDate.now())) {
            redirectAttributes.addFlashAttribute("errorAlquiler", "La fecha de inicio no puede ser anterior a hoy.");
            return "redirect:/alquileres/reserva?equipoId=" + idEquipo;
        }

        if (fechaFin.isBefore(fechaInicio)) {
            redirectAttributes.addFlashAttribute("errorAlquiler", "La fecha de fin debe ser igual o posterior a la fecha de inicio.");
            return "redirect:/alquileres/reserva?equipoId=" + idEquipo;
        }

        long dias = ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1;
        BigDecimal montoAlquiler = equipo.getPrecio_alquiler_dia().multiply(BigDecimal.valueOf(dias));

        // Depósito de garantía: 30% del valor comercial (precio_venta)
        BigDecimal depositoGarantia = equipo.getPrecio_venta()
                .multiply(new BigDecimal("0.30"))
                .setScale(2, RoundingMode.HALF_UP);

        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String codigoAlquiler = "ALQ-RETRO-" + timestamp + "-" + (int)(Math.random() * 900 + 100);

        AlquilerEquipo alquiler = new AlquilerEquipo(
                codigoAlquiler,
                fechaInicio,
                fechaFin,
                depositoGarantia,
                montoAlquiler,
                "RESERVADO",
                cliente,
                equipo
        );

        alquilerEquipoRepository.save(alquiler);
        redirectAttributes.addFlashAttribute("mensajeExito",
                "Reserva confirmada (" + codigoAlquiler + "). Depósito de garantía: $" + depositoGarantia);
        return "redirect:/alquileres/mis-reservas";
    }

    @GetMapping("/mis-reservas")
    public String misReservas(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Usuario usuarioEnSesion = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioEnSesion == null) {
            redirectAttributes.addFlashAttribute("mensajeError", "Inicia sesión para consultar tus alquileres.");
            return "redirect:/login";
        }

        Usuario cliente = usuarioRepository.findById(usuarioEnSesion.getId_usuario()).orElse(null);
        if (cliente == null) {
            return "redirect:/login";
        }

        List<AlquilerEquipo> reservas = alquilerEquipoRepository.findByCliente(cliente);
        model.addAttribute("reservas", reservas);
        return "alquileres/mis_reservas";
    }

    @GetMapping("/{id}/ical")
    public void descargarICal(@PathVariable("id") Long id,
                              HttpSession session,
                              HttpServletResponse response) throws IOException {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        Optional<AlquilerEquipo> opt = alquilerEquipoRepository.findById(id);
        if (opt.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        AlquilerEquipo alq = opt.get();
        String dtStart = alq.getFecha_inicio().format(DateTimeFormatter.BASIC_ISO_DATE);
        String dtEnd = alq.getFecha_fin().plusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE);

        response.setContentType("text/calendar; charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"alquiler-" + alq.getCodigo_alquiler() + ".ics\"");

        PrintWriter writer = response.getWriter();
        writer.println("BEGIN:VCALENDAR");
        writer.println("VERSION:2.0");
        writer.println("PRODID:-//RetroTunes//Alquiler Equipos v1.0//ES");
        writer.println("CALSCALE:GREGORIAN");
        writer.println("METHOD:PUBLISH");
        writer.println("BEGIN:VEVENT");
        writer.println("UID:retrotunes-" + alq.getCodigo_alquiler() + "@retrotunes.com");
        writer.println("DTSTAMP:" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "T120000Z");
        writer.println("DTSTART;VALUE=DATE:" + dtStart);
        writer.println("DTEND;VALUE=DATE:" + dtEnd);
        writer.println("SUMMARY:Alquiler RetroTunes: " + alq.getEquipo().getTitulo());
        writer.println("DESCRIPTION:Reserva de equipo " + alq.getEquipo().getTitulo() + " (" + alq.getCodigo_alquiler() + "). Depósito garantía: $" + alq.getDeposito_garantia() + " COP/USD.");
        writer.println("LOCATION:RetroTunes Store & Showroom - Carrera 7 # 45-10");
        writer.println("STATUS:CONFIRMED");
        writer.println("END:VEVENT");
        writer.println("END:VCALENDAR");
        writer.flush();
    }
}
