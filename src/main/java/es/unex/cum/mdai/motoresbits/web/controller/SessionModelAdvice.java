package es.unex.cum.mdai.motoresbits.web.controller;

import es.unex.cum.mdai.motoresbits.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

// Añade atributos de sesión al modelo para ser usados por las vistas (usuario, carrito, saldo).
@ControllerAdvice
public class SessionModelAdvice {

    private final UsuarioService usuarioService;

    public SessionModelAdvice(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @ModelAttribute
    public void addSessionAttributes(Model model, HttpSession session) {

        Object uid = session.getAttribute("usuarioId");
        Object uname = session.getAttribute("usuarioNombre");
        Object urole = session.getAttribute("usuarioRol");

        if (uid != null && (urole == null || !"ADMIN".equals(urole.toString()))) {
            Long userId = (uid instanceof Long) ? (Long) uid : Long.valueOf(uid.toString());
            try {
                var usuario = usuarioService.getById(userId);
                session.setAttribute("usuarioSaldo", usuario.getSaldo());
            } catch (Exception ignored) {
            }
        }

        Object usaldo = session.getAttribute("usuarioSaldo");
        Object pedidoCantidad = session.getAttribute("pedidoCantidad");
        Object pedidoId = session.getAttribute("pedidoId");

        model.addAttribute("usuarioId", uid);
        model.addAttribute("usuarioNombre", uname);
        model.addAttribute("usuarioRol", urole);
        model.addAttribute("usuarioSaldo", usaldo);

        model.addAttribute("pedidoCantidad", pedidoCantidad != null ? pedidoCantidad : 0);
        model.addAttribute("pedidoId", pedidoId);

        @SuppressWarnings("unchecked")
        Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttribute("cartItems");
        model.addAttribute("cartItems", cart);
    }
}
