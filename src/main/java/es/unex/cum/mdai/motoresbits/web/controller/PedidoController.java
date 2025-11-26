package es.unex.cum.mdai.motoresbits.web.controller;

import es.unex.cum.mdai.motoresbits.data.model.entity.Pedido;
import es.unex.cum.mdai.motoresbits.service.PedidoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

@Controller
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping("/carrito")
    public String verCarrito(HttpSession session, Model model) {
        Object pedidoIdObj = session.getAttribute("pedidoId");
        if (pedidoIdObj == null) {
            model.addAttribute("pedido", null);
            return "carrito";
        }
        Long pedidoId = (Long) pedidoIdObj;
        Pedido pedido = pedidoService.obtenerPedido(pedidoId);
        model.addAttribute("pedido", pedido);
        return "carrito";
    }

    @PostMapping("/carrito/agregar")
    public String agregarAlCarrito(@RequestParam Long idProducto,
                                   @RequestParam int cantidad,
                                   HttpSession session) {
        // buscamos o creamos pedido en sesión
        Long pedidoId = (Long) session.getAttribute("pedidoId");
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (pedidoId == null) {
            if (usuarioId == null) {
                // usuario no logueado: redirigir a login
                return "redirect:/login";
            }
            Pedido nuevo = pedidoService.crearPedido(usuarioId);
            session.setAttribute("pedidoId", nuevo.getId());
            pedidoId = nuevo.getId();
        }

        pedidoService.agregarLinea(pedidoId, idProducto, cantidad);
        return "redirect:/carrito";
    }

    @PostMapping("/carrito/eliminar")
    public String eliminarLinea(@RequestParam Long idProducto,
                                HttpSession session) {
        Long pedidoId = (Long) session.getAttribute("pedidoId");
        if (pedidoId != null) {
            pedidoService.eliminarLinea(pedidoId, idProducto);
        }
        return "redirect:/carrito";
    }

    @PostMapping("/pedido/confirmar")
    public String confirmarPedido(HttpSession session, Model model) {
        Long pedidoId = (Long) session.getAttribute("pedidoId");
        if (pedidoId == null) {
            model.addAttribute("error", "No hay un pedido en curso");
            return "carrito";
        }
        try {
            pedidoService.confirmarPedido(pedidoId);
            // limpiar pedido en sesión
            session.removeAttribute("pedidoId");
            model.addAttribute("success", "Pedido confirmado correctamente");
            return "checkout";
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            return "carrito";
        }
    }
}

