package es.unex.cum.mdai.motoresbits.web.controller;

import es.unex.cum.mdai.motoresbits.data.model.entity.DetallePedido;
import es.unex.cum.mdai.motoresbits.data.model.entity.Pedido;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.service.CatalogoService;
import es.unex.cum.mdai.motoresbits.service.PedidoService;
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
        // Permitimos ver el carrito sin iniciar sesión: si hay cartItems en sesión los mostramos.
        // Priorizar carrito en sesión (cartItems). Mostramos un Pedido temporal si existe cartItems.
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
            return "carrito";
        }

        // Si no hay carrito en sesión, mostrar pedido persistente si existe
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
        // Permitir añadir al carrito sin login: manipulamos carrito en sesión (no persistir aún)
        @SuppressWarnings("unchecked")
        Map<Long,Integer> cart = (Map<Long,Integer>) session.getAttribute("cartItems");
        if (cart == null) { cart = new HashMap<>(); }
        cart.put(idProducto, cart.getOrDefault(idProducto, 0) + cantidad);
        session.setAttribute("cartItems", cart);

        // actualizar cantidad total
        int totalItems = cart.values().stream().mapToInt(Integer::intValue).sum();
        session.setAttribute("pedidoCantidad", totalItems);

        return "redirect:/carrito";
    }

    @PostMapping("/carrito/anadir")
    public String anadirAlCarrito(@RequestParam Long idProducto,
                                  @RequestParam int cantidad,
                                  HttpSession session) {
        // Permitir añadir al carrito sin login: manejamos cartItems en sesión y redirigimos al detalle
        @SuppressWarnings("unchecked")
        Map<Long,Integer> cart = (Map<Long,Integer>) session.getAttribute("cartItems");
        if (cart == null) { cart = new HashMap<>(); }
        cart.put(idProducto, cart.getOrDefault(idProducto, 0) + cantidad);
        session.setAttribute("cartItems", cart);

        int totalItems = cart.values().stream().mapToInt(Integer::intValue).sum();
        session.setAttribute("pedidoCantidad", totalItems);

        return "redirect:/producto/" + idProducto;
    }

    @PostMapping("/carrito/eliminar")
    public String eliminarLinea(@RequestParam Long idProducto,
                                HttpSession session) {
        // Permitir eliminar del carrito sin login: manipulamos cartItems en sesión
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
            return "redirect:/login"; // pedir login antes de confirmar
        }

        @SuppressWarnings("unchecked")
        Map<Long,Integer> cart = (Map<Long,Integer>) session.getAttribute("cartItems");
        if (cart == null || cart.isEmpty()) {
            model.addAttribute("error", "No hay artículos en el carrito");
            return "carrito";
        }

        Long nuevoId = null;
        try {
            // crear pedido persistente y añadir líneas
            var nuevo = pedidoService.crearPedido(usuarioId);
            nuevoId = nuevo.getId();
            for (Map.Entry<Long,Integer> e : cart.entrySet()) {
                pedidoService.agregarLinea(nuevoId, e.getKey(), e.getValue());
            }
            // confirmar (descontar stock y cambiar estado)
            pedidoService.confirmarPedido(nuevoId);

            // limpiar sesión
            session.removeAttribute("cartItems");
            session.removeAttribute("pedidoCantidad");
            // opcional: guardar pedidoId si quieres referencia futura
            session.setAttribute("pedidoId", nuevoId);

            model.addAttribute("success", "Pedido confirmado correctamente");
            return "checkout";
        } catch (StockInsuficienteException ex) {
            // Si falla por falta de stock, vaciamos el carrito en sesión y mostramos el error
            // además eliminamos el pedido creado para no dejar registros CREADO en BD
            try {
                if (nuevoId != null) {
                    pedidoService.eliminarPedido(nuevoId);
                }
            } catch (Exception ignore) {
                // si no se puede eliminar, no interrumpimos el flujo; seguimos limpiando sesión
            }
            session.removeAttribute("cartItems");
            session.removeAttribute("pedidoCantidad");
            session.removeAttribute("pedidoId");
            model.addAttribute("error", ex.getMessage());
            return "carrito";
        } catch (Exception ex) {
            // en cualquier otra excepción intentamos limpiar el pedido temporal también
            try {
                if (nuevoId != null) {
                    pedidoService.eliminarPedido(nuevoId);
                }
            } catch (Exception ignore) {}
            model.addAttribute("error", ex.getMessage());
            return "carrito";
        }
    }
}
