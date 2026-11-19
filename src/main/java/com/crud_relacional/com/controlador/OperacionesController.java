package com.crud_relacional.com.controlador;

import com.crud_relacional.com.entidades.AlquilerEquipo;
import com.crud_relacional.com.entidades.Usuario;
import com.crud_relacional.com.repositorio.AlquilerEquipoRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/operaciones")
public class OperacionesController {

    private final AlquilerEquipoRepository alquilerEquipoRepository;

    public OperacionesController(AlquilerEquipoRepository alquilerEquipoRepository) {
        this.alquilerEquipoRepository = alquilerEquipoRepository;
    }

    private boolean tienePermisoTecnico(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || usuario.getRol() == null) return false;
        String rol = usuario.getRol().getNombre_rol();
        return "TECNICO_OPERACIONES".equalsIgnoreCase(rol) || "ADMIN".equalsIgnoreCase(rol);
    }

    @GetMapping("/inspeccion")
    public String listarParaInspeccion(HttpSession session, Model model) {
        if (!tienePermisoTecnico(session)) {
            return "redirect:/login";
        }

        List<AlquilerEquipo> alquileres = alquilerEquipoRepository.findPendientesInspeccion();
        model.addAttribute("alquileres", alquileres);
        return "operaciones/inspeccion";
    }

    @PostMapping("/inspeccion/{id}")
    public String procesarInspeccion(@PathVariable("id") Long id,
                                     @RequestParam("accion") String accion,
                                     @RequestParam("novedadTecnica") String novedadTecnica,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        if (!tienePermisoTecnico(session)) {
            return "redirect:/login";
        }

        Optional<AlquilerEquipo> opt = alquilerEquipoRepository.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Alquiler no encontrado.");
            return "redirect:/operaciones/inspeccion";
        }

        AlquilerEquipo alq = opt.get();
        alq.setNovedad_tecnica(novedadTecnica != null ? novedadTecnica.trim() : "Inspección técnica sin observaciones.");

        if ("LIBERAR".equalsIgnoreCase(accion)) {
            alq.setEstado("DEVUELTO");
            alq.setDeposito_devuelto(true);
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Equipo " + alq.getCodigo_alquiler() + " inspeccionado: Estado DEVUELTO y garantía liberada.");
        } else {
            alq.setEstado("CON_NOVEDAD");
            alq.setDeposito_devuelto(false);
            redirectAttributes.addFlashAttribute("mensajeAlerta",
                    "Equipo " + alq.getCodigo_alquiler() + " marcado CON NOVEDAD: Garantía retenida.");
        }

        alquilerEquipoRepository.save(alq);
        return "redirect:/operaciones/inspeccion";
    }
}
