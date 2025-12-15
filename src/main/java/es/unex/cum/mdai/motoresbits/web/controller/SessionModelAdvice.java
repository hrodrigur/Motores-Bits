package es.unex.cum.mdai.motoresbits.web.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

import java.util.Map;

@ControllerAdvice
public class SessionModelAdvice {

    @ModelAttribute
    public void addSessionAttributes(Model model, HttpSession session) {
        Object uid = session.getAttribute("usuarioId");
        Object uname = session.getAttribute("usuarioNombre");
        Object urole = session.getAttribute("usuarioRol");
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
        Map<Long,Integer> cart = (Map<Long,Integer>) session.getAttribute("cartItems");
        model.addAttribute("cartItems", cart == null ? null : cart);
    }
}
