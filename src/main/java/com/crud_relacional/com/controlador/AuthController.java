package com.crud_relacional.com.controlador;

import com.crud_relacional.com.entidades.Rol;
import com.crud_relacional.com.entidades.Usuario;
import com.crud_relacional.com.repositorio.RolRepository;
import com.crud_relacional.com.repositorio.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    // Cero capa Service: Inyección directa de repositorios por constructor
    public AuthController(UsuarioRepository usuarioRepository, RolRepository rolRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
    }

    @GetMapping("/")
    public String index(HttpSession session) {
        return "index";
    }

    @GetMapping("/login")
    public String mostrarLogin(HttpSession session,
                               @RequestParam(value = "error", required = false) String error,
                               @RequestParam(value = "logout", required = false) String logout,
                               @RequestParam(value = "registrado", required = false) String registrado,
                               Model model) {
        if (session.getAttribute("usuarioLogueado") != null) {
            return "redirect:/catalogo";
        }
        if (error != null) {
            model.addAttribute("mensajeError", "Credenciales incorrectas o usuario inactivo.");
        }
        if (logout != null) {
            model.addAttribute("mensajeExito", "Has cerrado sesión correctamente.");
        }
        if (registrado != null) {
            model.addAttribute("mensajeExito", "Registro exitoso. Ahora puedes iniciar sesión con tu cuenta.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam("correo") String correo,
                                @RequestParam("clave") String clave,
                                HttpSession session,
                                Model model) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreoAndClaveUsuario(correo.trim(), clave);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if (Boolean.FALSE.equals(usuario.getActivo())) {
                model.addAttribute("mensajeError", "Tu cuenta se encuentra inactiva. Contacta al administrador.");
                return "login";
            }

            // Almacenar usuario autenticado en la sesión HTTP (compatibilidad estándar y SpEL sessionScope.usuario)
            session.setAttribute("usuarioLogueado", usuario);
            session.setAttribute("usuario", usuario);

            // Redirección según rol
            String rol = usuario.getRol() != null ? usuario.getRol().getNombre_rol() : "CLIENTE";
            if ("ADMIN".equalsIgnoreCase(rol)) {
                return "redirect:/catalogo?admin=true";
            } else if ("DISCOGRAFICA".equalsIgnoreCase(rol) || "ARTISTA".equalsIgnoreCase(rol)) {
                return "redirect:/catalogo?creator=true";
            } else {
                return "redirect:/catalogo";
            }
        } else {
            model.addAttribute("mensajeError", "Correo o clave incorrectos.");
            model.addAttribute("correoIngresado", correo);
            return "login";
        }
    }

    @GetMapping("/registro")
    public String mostrarRegistro(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogueado") != null) {
            return "redirect:/catalogo";
        }
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@Valid @ModelAttribute("usuario") Usuario usuario,
                                   BindingResult bindingResult,
                                   Model model) {
        // Validar si el correo ya existe
        if (usuario.getCorreo() != null && usuarioRepository.existsByCorreo(usuario.getCorreo().trim())) {
            bindingResult.rejectValue("correo", "error.usuario", "El correo ya se encuentra registrado.");
        }

        if (bindingResult.hasErrors()) {
            return "registro";
        }

        // Asignación automática del rol CLIENTE
        Rol rolCliente = rolRepository.findByNombreRol("CLIENTE")
                .orElseGet(() -> rolRepository.save(new Rol("CLIENTE")));

        usuario.setCorreo(usuario.getCorreo().trim());
        usuario.setRol(rolCliente);
        usuario.setActivo(true);

        usuarioRepository.save(usuario);
        return "redirect:/login?registrado=true";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }
}
