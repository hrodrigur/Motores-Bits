package es.unex.cum.mdai.motoresbits.web.controller;

import es.unex.cum.mdai.motoresbits.service.CatalogoService;
import es.unex.cum.mdai.motoresbits.service.PedidoService;
import es.unex.cum.mdai.motoresbits.service.ResenaService;
import es.unex.cum.mdai.motoresbits.service.UsuarioService;
import es.unex.cum.mdai.motoresbits.service.exception.EstadoPedidoInvalidoException;
import es.unex.cum.mdai.motoresbits.service.exception.ReferenciaProductoDuplicadaException;
import es.unex.cum.mdai.motoresbits.service.exception.DatosProductoInvalidosException;
import es.unex.cum.mdai.motoresbits.service.exception.SaldoInsuficienteException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import es.unex.cum.mdai.motoresbits.data.model.enums.RolUsuario;

// Controlador de administración: gestión de categorías, productos, pedidos, reseñas y usuarios.
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

    @GetMapping("/categorias")
    public String listarCategorias(HttpSession session, Model model) {
        if (isNotAdmin(session)) return "redirect:/login";
        model.addAttribute("categorias", catalogoService.listarCategorias());
        return "admin/categorias";
    }

    @PostMapping("/categorias/crear")
    public String crearCategoria(HttpSession session, @RequestParam String nombre, @RequestParam String descripcion, Model model) {
        if (isNotAdmin(session)) return "redirect:/login";
        try {
            catalogoService.crearCategoria(nombre, descripcion);
            return "redirect:/admin/categorias";
        } catch (DatosProductoInvalidosException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("categorias", catalogoService.listarCategorias());
            return "admin/categorias";
        }
    }

    @PostMapping("/categorias/editar")
    public String editarCategoria(HttpSession session, @RequestParam Long id, @RequestParam String nombre, @RequestParam String descripcion, Model model) {
        if (isNotAdmin(session)) return "redirect:/login";
        try {
            catalogoService.editarCategoria(id, nombre, descripcion);
            return "redirect:/admin/categorias";
        } catch (DatosProductoInvalidosException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("categorias", catalogoService.listarCategorias());
            return "admin/categorias";
        }
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

        Object adminError = session.getAttribute("adminError");
        if (adminError != null) {
            model.addAttribute("error", adminError.toString());
            session.removeAttribute("adminError");
        }

        return "admin/productos";
    }

    @PostMapping("/productos/crear")
    public String crearProducto(HttpSession session,
                                @RequestParam Long idCategoria,
                                @RequestParam String nombre,
                                @RequestParam String referencia,
                                @RequestParam BigDecimal precio,
                                @RequestParam Integer stock,
                                @RequestParam(required = false) String imagenUrl,
                                Model model) {
        if (isNotAdmin(session)) return "redirect:/login";
        try {

            catalogoService.crearProducto(idCategoria, nombre, referencia, precio, stock, imagenUrl);
            return "redirect:/admin/productos";
        } catch (ReferenciaProductoDuplicadaException | DatosProductoInvalidosException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("categorias", catalogoService.listarCategorias());
            model.addAttribute("productos", catalogoService.listarProductosConCategoria());
            return "admin/productos";
        }
    }


    @PostMapping("/productos/editar")
    public String editarProducto(HttpSession session,
                                 @RequestParam Long id,
                                 @RequestParam Long idCategoria,
                                 @RequestParam String nombre,
                                 @RequestParam BigDecimal precio,
                                 @RequestParam Integer stock,
                                 @RequestParam(required = false) String imagenUrl) {
        if (isNotAdmin(session)) return "redirect:/login";
        try {

            catalogoService.editarProducto(id, idCategoria, nombre, precio, stock, imagenUrl);
            return "redirect:/admin/productos";
        } catch (DatosProductoInvalidosException ex) {
            session.setAttribute("adminError", ex.getMessage());
            return "redirect:/admin/productos";
        }
    }

    @PostMapping("/productos/eliminar")
    public String eliminarProducto(HttpSession session, @RequestParam Long id) {
        if (isNotAdmin(session)) return "redirect:/login";
        catalogoService.eliminarProducto(id);
        return "redirect:/admin/productos";
    }

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

        } catch (SaldoInsuficienteException | EstadoPedidoInvalidoException ex) {
            model.addAttribute("errorEstado", ex.getMessage());
            model.addAttribute("pedidos", pedidoService.listarTodosPedidos());
            return "admin/pedidos";

        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorEstado", "Estado inválido: " + nuevoEstado);
            model.addAttribute("pedidos", pedidoService.listarTodosPedidos());
            return "admin/pedidos";

        } catch (Exception ex) {
            model.addAttribute("errorEstado", "No se pudo cambiar el estado: " + ex.getMessage());
            model.addAttribute("pedidos", pedidoService.listarTodosPedidos());
            return "admin/pedidos";
        }
    }

    @PostMapping("/resena/eliminar/admin")
    public String eliminarResenaAdmin(HttpSession session, @RequestParam Long idResena, jakarta.servlet.http.HttpServletRequest request) {
        if (isNotAdmin(session)) return "redirect:/login";
        resenaService.eliminarResena(idResena);

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;
        }
        return "redirect:/admin/pedidos";
    }

    @GetMapping({"","/"})
    public String adminIndex(HttpSession session, Model model) {
        if (isNotAdmin(session)) return "redirect:/login";
        model.addAttribute("categoriasCount", catalogoService.listarCategorias().size());
        model.addAttribute("productosCount", catalogoService.listarProductos().size());
        model.addAttribute("pedidosCount", pedidoService.listarTodosPedidos().size());
        return "admin/index";
    }

    @GetMapping("/usuario/{id}")
    public String verCliente(@PathVariable Long id, HttpSession session, Model model) {
        if (isNotAdmin(session)) return "redirect:/login";

        var usuario = usuarioService.getById(id);
        var pedidosUsuario = pedidoService.listarPedidosUsuario(id);
        var resenasUsuario = resenaService.listarResenasUsuario(id);

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
            return "redirect:/admin/pedidos";
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("pedidos", pedidoService.listarTodosPedidos());
            return "admin/pedidos";
        }
    }

    @GetMapping("/usuarios")
    public String listarUsuarios(HttpSession session, Model model) {
        if (isNotAdmin(session)) return "redirect:/login";
        model.addAttribute("usuarios", usuarioService.listarTodos());
        return "admin/usuarios";
    }

    @PostMapping("/usuarios/eliminar")
    public String eliminarUsuarioAdmin(HttpSession session, @RequestParam Long id, Model model) {
        if (isNotAdmin(session)) return "redirect:/login";
        try {
            var usuario = usuarioService.getById(id);
            if (usuario.getRol() == RolUsuario.ADMIN) {
                model.addAttribute("error", "No está permitido eliminar usuarios con rol ADMIN.");
                model.addAttribute("usuarios", usuarioService.listarTodos());
                return "admin/usuarios";
            }

            usuarioService.eliminarUsuario(id);
            return "redirect:/admin/usuarios";
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("usuarios", usuarioService.listarTodos());
            return "admin/usuarios";
        }
    }

    @GetMapping("/usuarios/saldo")
    public String adminUsuariosSaldo(HttpSession session, Model model) {
        if (isNotAdmin(session)) return "redirect:/";
        model.addAttribute("usuarios", usuarioService.listarTodos());
        return "admin-usuarios-saldo";
    }

    @GetMapping("/usuarios/{id}/saldo")
    public String adminFormSaldo(@PathVariable Long id,
                                 @RequestParam(defaultValue = "add") String op,
                                 HttpSession session,
                                 Model model) {

        if (isNotAdmin(session)) return "redirect:/";

        model.addAttribute("usuario", usuarioService.getById(id));
        model.addAttribute("op", op);
        return "admin-usuario-saldo-form";
    }

    @PostMapping("/usuarios/{id}/saldo")
    public String adminAjustarSaldo(@PathVariable Long id,
                                    @RequestParam BigDecimal cantidad,
                                    @RequestParam(defaultValue = "add") String op,
                                    HttpSession session,
                                    Model model) {

        if (isNotAdmin(session)) return "redirect:/";

        try {
            BigDecimal delta = op.equals("sub") ? cantidad.negate() : cantidad;
            usuarioService.ajustarSaldo(id, delta);
            return "redirect:/admin/usuarios/saldo";

        } catch (Exception ex) {
            model.addAttribute("usuario", usuarioService.getById(id));
            model.addAttribute("op", op);
            model.addAttribute("error", ex.getMessage());
            return "admin-usuario-saldo-form";
        }
    }
}
