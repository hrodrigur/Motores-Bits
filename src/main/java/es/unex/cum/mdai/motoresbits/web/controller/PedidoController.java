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
import java.util.HashMap;
import java.util.Map;

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
        @SuppressWarnings("unchecked")
        Map<Long,Integer> cart = (Map<Long,Integer>) session.getAttribute("cartItems");
        if (cart != null && !cart.isEmpty()) {
            Pedido temp = new Pedido();
            for (Map.Entry<Long,Integer> e : cart.entrySet()) {
                Long prodId = e.getKey();
                Integer qty = e.getValue();
                Producto p = catalogoService.obtenerProducto(prodId);
                DetallePedido d = new DetallePedido();
                d.setProducto(p);
                d.setCantidad(qty);
                d.setPrecio(p.getPrecio());
                d.setPedido(temp);
                temp.getDetalles().add(d);
            }
            temp.setTotal(temp.getDetalles().stream()
                    .map(d -> d.getPrecio().multiply(java.math.BigDecimal.valueOf(d.getCantidad())))
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
            model.addAttribute("pedido", temp);

            Object cartMsg = session.getAttribute("cartMsg");
            if (cartMsg != null) {
                model.addAttribute("cartMsg", cartMsg);
                session.removeAttribute("cartMsg");
            }

            return "carrito";
        }

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

        @SuppressWarnings("unchecked")
        Map<Long,Integer> cart = (Map<Long,Integer>) session.getAttribute("cartItems");
        if (cart == null) { cart = new HashMap<>(); }

        Producto prod = catalogoService.obtenerProducto(idProducto);
        int available = prod.getStock() == null ? 0 : prod.getStock();

        if (cantidad > 100) cantidad = 100;

        int current = cart.getOrDefault(idProducto, 0);
        int requestedTotal = current + cantidad;
        if (requestedTotal > 100) requestedTotal = 100;

        if (requestedTotal > available) {
            requestedTotal = available;
            session.setAttribute("cartMsg", "La cantidad solicitada se ha ajustado al stock disponible: " + available);
        }

        cart.put(idProducto, requestedTotal);
        session.setAttribute("cartItems", cart);

        int totalItems = cart.values().stream().mapToInt(Integer::intValue).sum();
        session.setAttribute("pedidoCantidad", totalItems);

        return "redirect:/carrito";
    }

    @PostMapping("/carrito/anadir")
    public String anadirAlCarrito(@RequestParam Long idProducto,
                                  @RequestParam int cantidad,
                                  HttpSession session) {

        @SuppressWarnings("unchecked")
        Map<Long,Integer> cart = (Map<Long,Integer>) session.getAttribute("cartItems");
        if (cart == null) { cart = new HashMap<>(); }

        Producto prod = catalogoService.obtenerProducto(idProducto);
        int available = prod.getStock() == null ? 0 : prod.getStock();

        if (cantidad > 100) cantidad = 100;

        int current = cart.getOrDefault(idProducto, 0);
        int requestedTotal = current + cantidad;
        if (requestedTotal > 100) requestedTotal = 100;

        if (requestedTotal > available) {
            requestedTotal = available;
            session.setAttribute("cartMsg", "La cantidad solicitada se ha ajustado al stock disponible: " + available);
        }

        cart.put(idProducto, requestedTotal);
        session.setAttribute("cartItems", cart);

        int totalItems = cart.values().stream().mapToInt(Integer::intValue).sum();
        session.setAttribute("pedidoCantidad", totalItems);

        return "redirect:/producto/" + idProducto;
    }

    @PostMapping("/carrito/eliminar")
    public String eliminarLinea(@RequestParam Long idProducto,
                                HttpSession session) {

        @SuppressWarnings("unchecked")
        Map<Long,Integer> cart = (Map<Long,Integer>) session.getAttribute("cartItems");
        if (cart != null) {
            cart.remove(idProducto);
            session.setAttribute("cartItems", cart);
            int totalItems = cart.values().stream().mapToInt(Integer::intValue).sum();
            session.setAttribute("pedidoCantidad", totalItems);
        } else {
            session.setAttribute("pedidoCantidad", 0);
        }
        return "redirect:/carrito";
    }

    @PostMapping("/pedido/confirmar")
    public String confirmarPedido(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }

        @SuppressWarnings("unchecked")
        Map<Long,Integer> cart = (Map<Long,Integer>) session.getAttribute("cartItems");
        if (cart == null || cart.isEmpty()) {
            model.addAttribute("error", "No hay artículos en el carrito");
            return "carrito";
        }

        Long nuevoId = null;
        try {
            var nuevo = pedidoService.crearPedido(usuarioId);
            nuevoId = nuevo.getId();

            for (Map.Entry<Long,Integer> e : cart.entrySet()) {
                pedidoService.agregarLinea(nuevoId, e.getKey(), e.getValue());
            }

            // ✅ confirmar (esto ya descuenta stock + saldo en el service)
            Pedido pedidoConfirmado = pedidoService.confirmarPedido(nuevoId);

            // ✅ actualizar saldo en sesión (para que se vea en el header)
            if (pedidoConfirmado.getUsuario() != null) {
                session.setAttribute("usuarioSaldo", pedidoConfirmado.getUsuario().getSaldo());
            }

            // limpiar sesión del carrito
            session.removeAttribute("cartItems");
            session.removeAttribute("pedidoCantidad");
            session.setAttribute("pedidoId", nuevoId);

            model.addAttribute("success", "Pedido confirmado correctamente");
            return "checkout";

        } catch (SaldoInsuficienteException ex) {

            try {
                if (nuevoId != null) pedidoService.eliminarPedido(nuevoId);
            } catch (Exception ignore) {}

            model.addAttribute("error", ex.getMessage());
            return "carrito";

        } catch (StockInsuficienteException ex) {

            try {
                if (nuevoId != null) pedidoService.eliminarPedido(nuevoId);
            } catch (Exception ignore) {}

            session.removeAttribute("cartItems");
            session.removeAttribute("pedidoCantidad");
            session.removeAttribute("pedidoId");

            model.addAttribute("error", ex.getMessage());
            return "carrito";

        } catch (Exception ex) {

            try {
                if (nuevoId != null) pedidoService.eliminarPedido(nuevoId);
            } catch (Exception ignore) {}

            model.addAttribute("error", ex.getMessage());
            return "carrito";
        }
    }
}
