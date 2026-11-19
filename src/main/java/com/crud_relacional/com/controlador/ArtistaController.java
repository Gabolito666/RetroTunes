package com.crud_relacional.com.controlador;

import com.crud_relacional.com.entidades.Usuario;
import com.crud_relacional.com.repositorio.VentaRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Controller
@RequestMapping("/artista")
public class ArtistaController {

    private final VentaRepository ventaRepository;

    public ArtistaController(VentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;
    }

    private boolean tienePermisoArtista(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null || usuario.getRol() == null) return false;
        String rol = usuario.getRol().getNombre_rol();
        return "ARTISTA".equalsIgnoreCase(rol) || "DISCOGRAFICA".equalsIgnoreCase(rol) || "ADMIN".equalsIgnoreCase(rol);
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
}
