package es.unex.cum.mdai.motoresbits.web.controller;

import es.unex.cum.mdai.motoresbits.data.model.entity.DetallePedido;
import es.unex.cum.mdai.motoresbits.data.model.entity.Pedido;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.service.CatalogoService;
import es.unex.cum.mdai.motoresbits.service.PedidoService;
import es.unex.cum.mdai.motoresbits.service.exception.SaldoInsuficienteException;
import es.unex.cum.mdai.motoresbits.service.exception.StockInsuficienteException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

// Controlador para operaciones de carrito y pedidos: ver, agregar, eliminar y confirmar pedidos.
@Controller
public class PedidoController {

    private final PedidoService pedidoService;
    private final CatalogoService catalogoService;

    public PedidoController(PedidoService pedidoService, CatalogoService catalogoService) {
        this.pedidoService = pedidoService;
        this.catalogoService = catalogoService;
    }

    @GetMapping("/carrito")
    public String verCarrito(HttpSession session, Model model) {

        Object cartMsg = session.getAttribute("cartMsg");
        if (cartMsg != null) {
            model.addAttribute("cartMsg", cartMsg.toString());
            session.removeAttribute("cartMsg");
        }

        Object ok = session.getAttribute("carritoOk");
        if (ok != null) {
            model.addAttribute("success", ok.toString());
            session.removeAttribute("carritoOk");
        }

        Object err = session.getAttribute("carritoError");
        if (err != null) {
            model.addAttribute("error", err.toString());
            session.removeAttribute("carritoError");
        }

        @SuppressWarnings("unchecked")
        Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttribute("cartItems");

        if (cart == null || cart.isEmpty()) {
            model.addAttribute("pedido", null);
            return "carrito";
        }

        Pedido temp = new Pedido();
        for (Map.Entry<Long, Integer> e : cart.entrySet()) {
            Producto p = catalogoService.obtenerProducto(e.getKey());

            DetallePedido d = new DetallePedido();
            d.setProducto(p);
            d.setCantidad(e.getValue());
            d.setPrecio(p.getPrecio());
            d.setPedido(temp);
            temp.getDetalles().add(d);
        }

        BigDecimal total = temp.getDetalles().stream()
                .map(d -> d.getPrecio().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        temp.setTotal(total);

        model.addAttribute("pedido", temp);
        return "carrito";
    }

    @PostMapping("/carrito/agregar")
    public String agregarAlCarrito(@RequestParam Long idProducto,
                                   @RequestParam int cantidad,
                                   HttpSession session) {
        return agregarInterno(idProducto, cantidad, session, "redirect:/carrito");
    }

    @PostMapping("/carrito/anadir")
    public String anadirDesdeDetalle(@RequestParam Long idProducto,
                                     @RequestParam int cantidad,
                                     HttpSession session) {
        return agregarInterno(idProducto, cantidad, session, "redirect:/producto/" + idProducto);
    }

    private String agregarInterno(Long idProducto, int cantidad, HttpSession session, String redirectTo) {
        @SuppressWarnings("unchecked")
        Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttribute("cartItems");
        if (cart == null) cart = new HashMap<>();

        Producto prod = catalogoService.obtenerProducto(idProducto);
        int available = prod.getStock() == null ? 0 : prod.getStock();

        if (cantidad > 100) cantidad = 100;
        if (cantidad < 1) cantidad = 1;

        int current = cart.getOrDefault(idProducto, 0);
        int requestedTotal = current + cantidad;
        if (requestedTotal > 100) requestedTotal = 100;

        if (requestedTotal > available) {
            requestedTotal = available;
            session.setAttribute("cartMsg", "La cantidad solicitada se ha ajustado al stock disponible: " + available);
        }

        if (requestedTotal <= 0) {
            cart.remove(idProducto);
        } else {
            cart.put(idProducto, requestedTotal);
        }

        session.setAttribute("cartItems", cart);

        int totalItems = cart.values().stream().mapToInt(Integer::intValue).sum();
        session.setAttribute("pedidoCantidad", totalItems);

        return redirectTo;
    }

    @PostMapping("/carrito/eliminar")
    public String eliminarLinea(@RequestParam Long idProducto, HttpSession session) {
        @SuppressWarnings("unchecked")
        Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttribute("cartItems");

        if (cart != null) {
            cart.remove(idProducto);

            if (cart.isEmpty()) {
                session.removeAttribute("cartItems");
                session.setAttribute("pedidoCantidad", 0);
            } else {
                session.setAttribute("cartItems", cart);
                int totalItems = cart.values().stream().mapToInt(Integer::intValue).sum();
                session.setAttribute("pedidoCantidad", totalItems);
            }
        } else {
            session.setAttribute("pedidoCantidad", 0);
        }

        return "redirect:/carrito";
    }

    @PostMapping("/pedido/confirmar")
    public String confirmarPedido(HttpSession session) {

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) return "redirect:/login";

        @SuppressWarnings("unchecked")
        Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttribute("cartItems");
        if (cart == null || cart.isEmpty()) {
            session.setAttribute("carritoError", "No hay artículos en el carrito");
            return "redirect:/carrito";
        }

        Long nuevoId = null;
        try {
            var nuevo = pedidoService.crearPedido(usuarioId);
            nuevoId = nuevo.getId();

            for (Map.Entry<Long, Integer> e : cart.entrySet()) {
                pedidoService.agregarLinea(nuevoId, e.getKey(), e.getValue());
            }

            Pedido pedidoConfirmado = pedidoService.confirmarPedido(nuevoId);

            if (pedidoConfirmado.getUsuario() != null && pedidoConfirmado.getUsuario().getSaldo() != null) {
                session.setAttribute("usuarioSaldo", pedidoConfirmado.getUsuario().getSaldo());
            }

            session.removeAttribute("cartItems");
            session.setAttribute("pedidoCantidad", 0);

            session.setAttribute("carritoOk", "Pedido confirmado y pagado correctamente");
            return "redirect:/carrito";

        } catch (SaldoInsuficienteException | StockInsuficienteException ex) {
            try { if (nuevoId != null) pedidoService.eliminarPedido(nuevoId); } catch (Exception ignore) {}

            session.setAttribute("carritoError", ex.getMessage());
            return "redirect:/carrito";

        } catch (Exception ex) {
            try { if (nuevoId != null) pedidoService.eliminarPedido(nuevoId); } catch (Exception ignore) {}

            session.setAttribute("carritoError", "Error al confirmar el pedido: " + ex.getMessage());
            return "redirect:/carrito";
        }
    }
}
