package es.unex.cum.mdai.motoresbits.web.controller;

import es.unex.cum.mdai.motoresbits.data.model.entity.Usuario;
import es.unex.cum.mdai.motoresbits.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String loginForm(Model model, @RequestParam(required = false) String registered) {
        if (registered != null) {
            model.addAttribute("info", "Cuenta creada correctamente. Por favor, inicia sesión.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String loginSubmit(@RequestParam String email,
                              @RequestParam String contrasena,
                              Model model,
                              HttpSession session) {
        try {
            Usuario u = usuarioService.login(email, contrasena);

            session.setAttribute("usuarioId", u.getId());
            session.setAttribute("usuarioNombre", u.getNombre());

            // guardar rol en sesión para mostrar acciones de admin
            if (u.getRol() != null) {
                session.setAttribute("usuarioRol", u.getRol().name());
            }

            // ✅ guardar saldo en sesión para mostrarlo en el header
            session.setAttribute("usuarioSaldo", u.getSaldo());

            return "redirect:/";
        } catch (Exception ex) {
            model.addAttribute("error", "Credenciales inválidas");
            return "login";
        }
    }

    @GetMapping("/registro")
    public String registroForm() {
        return "register";
    }

    @PostMapping("/registro")
    public String registroSubmit(@RequestParam String nombre,
                                 @RequestParam String email,
                                 @RequestParam String contrasena,
                                 Model model,
                                 HttpSession session) {
        try {
            usuarioService.registrarCliente(nombre, email, contrasena);
            return "redirect:/login?registered=true";
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            return "register";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
