package es.unex.cum.mdai.motoresbits.web.controller;

import es.unex.cum.mdai.motoresbits.data.model.entity.Usuario;
import es.unex.cum.mdai.motoresbits.service.PedidoService;
import es.unex.cum.mdai.motoresbits.service.UsuarioService;
import es.unex.cum.mdai.motoresbits.service.ResenaService;
import es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;

// Controlador para operaciones del usuario: perfil, saldo, pedidos y reseñas
@Controller
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final PedidoService pedidoService;
    private final ResenaService resenaService;

    public UsuarioController(UsuarioService usuarioService,
                             PedidoService pedidoService,
                             ResenaService resenaService) {
        this.usuarioService = usuarioService;
        this.pedidoService = pedidoService;
        this.resenaService = resenaService;
    }

    private boolean isAdmin(HttpSession session) {
        String rol = (String) session.getAttribute("usuarioRol");
        return rol != null && rol.equals("ADMIN");
    }

    @GetMapping("/perfil")
    public String perfil(HttpSession session,
                         Model model,
                         @RequestParam(required = false) Boolean firstTime,
                         @RequestParam(required = false) String saved) {

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) return "redirect:/login";

        model.addAttribute("usuario", usuarioService.getById(usuarioId));
        model.addAttribute("pedidos", pedidoService.listarPedidosUsuario(usuarioId));
        model.addAttribute("resenas", resenaService.listarResenasUsuario(usuarioId));

        if (Boolean.TRUE.equals(firstTime)) model.addAttribute("firstTime", true);
        if (saved != null) model.addAttribute("saved", true);

        return "perfil";
    }

    @PostMapping("/perfil")
    public String actualizarPerfil(HttpSession session,
                                   @RequestParam(required = false) String direccion,
                                   @RequestParam(required = false) String telefono,
                                   Model model) {

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) return "redirect:/login";

        if (telefono != null && !telefono.matches("\\d{9}")) {
            model.addAttribute("usuario", usuarioService.getById(usuarioId));
            model.addAttribute("pedidos", pedidoService.listarPedidosUsuario(usuarioId));
            model.addAttribute("resenas", resenaService.listarResenasUsuario(usuarioId));
            model.addAttribute("errorTelefono", "El teléfono debe contener exactamente 9 dígitos.");
            return "perfil";
        }

        usuarioService.actualizarPerfil(usuarioId, direccion, telefono);
        return "redirect:/perfil?saved=true";
    }

    @GetMapping("/perfil/saldo")
    public String mostrarFormularioSaldo(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) return "redirect:/login";

        model.addAttribute("usuario", usuarioService.getById(usuarioId));
        return "perfil-saldo";
    }

    @PostMapping("/perfil/saldo")
    public String anadirSaldo(@RequestParam BigDecimal cantidad,
                              HttpSession session,
                              Model model) {

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) return "redirect:/login";

        if (isAdmin(session)) return "redirect:/perfil";

        try {
            Usuario usuario = usuarioService.ajustarSaldo(usuarioId, cantidad);
            session.setAttribute("usuarioSaldo", usuario.getSaldo());
            return "redirect:/perfil";
        } catch (Exception ex) {
            model.addAttribute("usuario", usuarioService.getById(usuarioId));
            model.addAttribute("error", ex.getMessage());
            return "perfil-saldo";
        }
    }

    @GetMapping("/mis-pedidos")
    public String misPedidos(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) return "redirect:/login";

        model.addAttribute("pedidos", pedidoService.listarPedidosUsuario(usuarioId));
        return "mis-pedidos";
    }

    @GetMapping("/perfil/pedido/{id}")
    public String verDetallePedidoPerfil(@PathVariable Long id,
                                         HttpSession session,
                                         Model model) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) return "redirect:/login";

        try {
            var pedido = pedidoService.obtenerPedido(id);

            if (pedido.getUsuario() == null || !usuarioId.equals(pedido.getUsuario().getId())) {
                model.addAttribute("error", "No tienes permiso para ver este pedido.");
                model.addAttribute("usuario", usuarioService.getById(usuarioId));
                model.addAttribute("pedidos", pedidoService.listarPedidosUsuario(usuarioId));
                model.addAttribute("resenas", resenaService.listarResenasUsuario(usuarioId));
                return "perfil";
            }

            model.addAttribute("pedido", pedido);
            return "pedido-detalle-usuario";

        } catch (Exception ex) {
            model.addAttribute("error", "No se ha podido cargar el pedido.");
            model.addAttribute("usuario", usuarioService.getById(usuarioId));
            model.addAttribute("pedidos", pedidoService.listarPedidosUsuario(usuarioId));
            model.addAttribute("resenas", resenaService.listarResenasUsuario(usuarioId));
            return "perfil";
        }
    }

    @PostMapping("/perfil/pedido/eliminar")
    public String eliminarPedidoPerfil(HttpSession session,
                                       @RequestParam Long idPedido,
                                       Model model) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) return "redirect:/login";

        try {
            var pedido = pedidoService.obtenerPedido(idPedido);

            if (pedido.getUsuario() == null || !usuarioId.equals(pedido.getUsuario().getId())) {
                model.addAttribute("error", "No tienes permiso para eliminar este pedido.");
            } else if (pedido.getEstado() == EstadoPedido.PAGADO
                    || pedido.getEstado() == EstadoPedido.ENTREGADO) {
                model.addAttribute("error", "No puedes eliminar un pedido pagado o entregado.");
            } else {
                pedidoService.eliminarPedido(idPedido);
                return "redirect:/perfil?deleted=true";
            }

            model.addAttribute("usuario", usuarioService.getById(usuarioId));
            model.addAttribute("pedidos", pedidoService.listarPedidosUsuario(usuarioId));
            model.addAttribute("resenas", resenaService.listarResenasUsuario(usuarioId));
            return "perfil";

        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("usuario", usuarioService.getById(usuarioId));
            model.addAttribute("pedidos", pedidoService.listarPedidosUsuario(usuarioId));
            model.addAttribute("resenas", resenaService.listarResenasUsuario(usuarioId));
            return "perfil";
        }
    }

    @PostMapping("/perfil/resena/eliminar")
    public String eliminarResenaPerfil(HttpSession session,
                                       @RequestParam Long idResena,
                                       Model model) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) return "redirect:/login";

        try {
            var resena = resenaService.obtenerResena(idResena);

            if (!resena.getUsuario().getId().equals(usuarioId)) {
                model.addAttribute("error", "No puedes eliminar reseñas de otro usuario.");
                model.addAttribute("usuario", usuarioService.getById(usuarioId));
                model.addAttribute("pedidos", pedidoService.listarPedidosUsuario(usuarioId));
                model.addAttribute("resenas", resenaService.listarResenasUsuario(usuarioId));
                return "perfil";
            }

            resenaService.eliminarResena(idResena);
            return "redirect:/perfil?resenaEliminada=true";

        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("usuario", usuarioService.getById(usuarioId));
            model.addAttribute("pedidos", pedidoService.listarPedidosUsuario(usuarioId));
            model.addAttribute("resenas", resenaService.listarResenasUsuario(usuarioId));
            return "perfil";
        }
    }



}
