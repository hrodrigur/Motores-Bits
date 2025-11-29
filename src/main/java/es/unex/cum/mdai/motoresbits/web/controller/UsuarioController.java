package es.unex.cum.mdai.motoresbits.web.controller;

import es.unex.cum.mdai.motoresbits.service.PedidoService;
import es.unex.cum.mdai.motoresbits.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

@Controller
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final PedidoService pedidoService;

    public UsuarioController(UsuarioService usuarioService, PedidoService pedidoService) {
        this.usuarioService = usuarioService;
        this.pedidoService = pedidoService;
    }

    @GetMapping("/perfil")
    public String perfil(HttpSession session, Model model, @RequestParam(required = false) Boolean firstTime, @RequestParam(required = false) String saved) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }
        model.addAttribute("usuario", usuarioService.getById(usuarioId));
        model.addAttribute("pedidos", pedidoService.listarPedidosUsuario(usuarioId));
        if (Boolean.TRUE.equals(firstTime)) {
            model.addAttribute("firstTime", true);
        }
        if (saved != null) {
            model.addAttribute("saved", true);
        }
        return "perfil";
    }

    @PostMapping("/perfil")
    public String actualizarPerfil(HttpSession session,
                                   @RequestParam(required = false) String direccion,
                                   @RequestParam(required = false) String telefono, Model model) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }
        // Validación del teléfono: debe tener exactamente 9 dígitos
        if (telefono != null && !telefono.matches("\\d{9}")) {
            model.addAttribute("usuario", usuarioService.getById(usuarioId));
            model.addAttribute("pedidos", pedidoService.listarPedidosUsuario(usuarioId));
            model.addAttribute("errorTelefono", "El teléfono debe contener exactamente 9 dígitos.");
            return "perfil";
        }
        usuarioService.actualizarPerfil(usuarioId, direccion, telefono);
        // Mantenerse en perfil y mostrar mensaje de guardado
        return "redirect:/perfil?saved=true";
    }

    @GetMapping("/mis-pedidos")
    public String misPedidos(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }
        model.addAttribute("pedidos", pedidoService.listarPedidosUsuario(usuarioId));
        return "mis-pedidos";
    }

}
