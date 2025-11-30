package es.unex.cum.mdai.motoresbits.web.controller;

import es.unex.cum.mdai.motoresbits.service.CatalogoService;
import es.unex.cum.mdai.motoresbits.service.PedidoService;
import es.unex.cum.mdai.motoresbits.service.ResenaService;
import es.unex.cum.mdai.motoresbits.service.UsuarioService;
import es.unex.cum.mdai.motoresbits.service.exception.EstadoPedidoInvalidoException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final CatalogoService catalogoService;
    private final PedidoService pedidoService;
    private final ResenaService resenaService;
    private final UsuarioService usuarioService;

    public AdminController(CatalogoService catalogoService, PedidoService pedidoService, ResenaService resenaService, UsuarioService usuarioService) {
        this.catalogoService = catalogoService;
        this.pedidoService = pedidoService;
        this.resenaService = resenaService;
        this.usuarioService = usuarioService;
    }

    private boolean isNotAdmin(HttpSession session) {
        Object r = session.getAttribute("usuarioRol");
        return r == null || !"ADMIN".equals(r.toString());
    }

    // ---------- CATEGORÍAS ----------
    @GetMapping("/categorias")
    public String listarCategorias(HttpSession session, Model model) {
        if (isNotAdmin(session)) return "redirect:/login";
        model.addAttribute("categorias", catalogoService.listarCategorias());
        return "admin/categorias";
    }

    @PostMapping("/categorias/crear")
    public String crearCategoria(HttpSession session, @RequestParam String nombre, @RequestParam String descripcion) {
        if (isNotAdmin(session)) return "redirect:/login";
        catalogoService.crearCategoria(nombre, descripcion);
        return "redirect:/admin/categorias";
    }

    @PostMapping("/categorias/editar")
    public String editarCategoria(HttpSession session, @RequestParam Long id, @RequestParam String nombre, @RequestParam String descripcion) {
        if (isNotAdmin(session)) return "redirect:/login";
        catalogoService.editarCategoria(id, nombre, descripcion);
        return "redirect:/admin/categorias";
    }

    @PostMapping("/categorias/eliminar")
    public String eliminarCategoria(HttpSession session, @RequestParam Long id, Model model) {
        if (isNotAdmin(session)) return "redirect:/login";
        try {
            catalogoService.eliminarCategoria(id);
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("categorias", catalogoService.listarCategorias());
            return "admin/categorias";
        }
        return "redirect:/admin/categorias";
    }

    // ---------- PRODUCTOS ----------
    @GetMapping("/productos")
    public String listarProductos(HttpSession session, Model model, @RequestParam(required = false) Long categoriaId) {
        if (isNotAdmin(session)) return "redirect:/login";
        if (categoriaId != null) {
            model.addAttribute("productos", catalogoService.listarPorCategoriaConCategoria(categoriaId));
            model.addAttribute("selectedCategoriaId", categoriaId);
        } else {
            model.addAttribute("productos", catalogoService.listarProductosConCategoria());
            model.addAttribute("selectedCategoriaId", null);
        }
        model.addAttribute("categorias", catalogoService.listarCategorias());
        return "admin/productos";
    }

    @PostMapping("/productos/crear")
    public String crearProducto(HttpSession session, @RequestParam Long idCategoria, @RequestParam String nombre,
                                @RequestParam String referencia, @RequestParam BigDecimal precio, @RequestParam Integer stock) {
        if (isNotAdmin(session)) return "redirect:/login";
        catalogoService.crearProducto(idCategoria, nombre, referencia, precio, stock);
        return "redirect:/admin/productos";
    }

    @PostMapping("/productos/editar")
    public String editarProducto(HttpSession session, @RequestParam Long id, @RequestParam Long idCategoria, @RequestParam String nombre,
                                 @RequestParam BigDecimal precio, @RequestParam Integer stock) {
        if (isNotAdmin(session)) return "redirect:/login";
        catalogoService.editarProducto(id, idCategoria, nombre, precio, stock);
        return "redirect:/admin/productos";
    }

    @PostMapping("/productos/eliminar")
    public String eliminarProducto(HttpSession session, @RequestParam Long id, Model model) {
        if (isNotAdmin(session)) return "redirect:/login";
        try {
            catalogoService.eliminarProducto(id);
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("productos", catalogoService.listarProductos());
            model.addAttribute("categorias", catalogoService.listarCategorias());
            return "admin/productos";
        }
        return "redirect:/admin/productos";
    }

    // ---------- PEDIDOS ----------
    @GetMapping("/pedidos")
    public String listarPedidos(HttpSession session, Model model) {
        if (isNotAdmin(session)) return "redirect:/login";
        model.addAttribute("pedidos", pedidoService.listarTodosPedidos());
        return "admin/pedidos";
    }

    @PostMapping("/pedidos/cambiar-estado")
    public String cambiarEstado(HttpSession session,
                                @RequestParam Long idPedido,
                                @RequestParam String nuevoEstado,
                                Model model) {
        if (isNotAdmin(session)) return "redirect:/login";

        try {
            pedidoService.cambiarEstado(
                    idPedido,
                    es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido.valueOf(nuevoEstado)
            );
            return "redirect:/admin/pedidos";
        } catch (EstadoPedidoInvalidoException ex) {

            // Recargamos la tabla de pedidos con el mensaje de error
            model.addAttribute("errorEstado", ex.getMessage());
            model.addAttribute("pedidos", pedidoService.listarTodosPedidos());

            return "admin/pedidos"; // vuelve a la misma vista
        }
    }


    // ---------- RESEÑAS (moderación) ----------
    @PostMapping("/resena/eliminar/admin")
    public String eliminarResenaAdmin(HttpSession session, @RequestParam Long idResena, jakarta.servlet.http.HttpServletRequest request) {
        if (isNotAdmin(session)) return "redirect:/login";
        resenaService.eliminarResena(idResena);
        // Intentar volver a la página previa (Referer) para mejor UX; fallback a /admin/pedidos
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;
        }
        return "redirect:/admin/pedidos";
    }

    // Admin root - panel principal (responde a /admin y /admin/)
    @GetMapping({"","/"})
    public String adminIndex(HttpSession session, Model model) {
        if (isNotAdmin(session)) return "redirect:/login";
        // pasar cuentas resumidas si se desea
        model.addAttribute("categoriasCount", catalogoService.listarCategorias().size());
        model.addAttribute("productosCount", catalogoService.listarProductos().size());
        model.addAttribute("pedidosCount", pedidoService.listarTodosPedidos().size());
        return "admin/index";
    }

    // ---------- Usuarios ----------
    @GetMapping("/usuario/{id}")
    public String verCliente(@PathVariable Long id, HttpSession session, Model model) {
        if (isNotAdmin(session)) return "redirect:/login";

        var usuario = usuarioService.getById(id);

        // Cargamos las colecciones con sus servicios, NO desde usuario.pedidos
        var pedidosUsuario = pedidoService.listarPedidosUsuario(id);

        var resenasUsuario = resenaService.obtenerResena(id); // o el que tengas

        model.addAttribute("usuario", usuario);
        model.addAttribute("pedidosUsuario", pedidosUsuario);
        model.addAttribute("resenasUsuario", resenasUsuario);

        return "admin/usuario-detalle";
    }

    @PostMapping("/pedidos/eliminar")
    public String eliminarPedido(HttpSession session,
                                 @RequestParam Long idPedido,
                                 Model model) {
        if (isNotAdmin(session)) return "redirect:/login";

        try {
            pedidoService.eliminarPedido(idPedido);
            return "redirect:/admin/pedidos";   // recarga la lista
        } catch (Exception ex) {
            // si quieres mostrar el error en la misma página:
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("pedidos", pedidoService.listarTodosPedidos());
            return "admin/pedidos";
        }
    }
}
