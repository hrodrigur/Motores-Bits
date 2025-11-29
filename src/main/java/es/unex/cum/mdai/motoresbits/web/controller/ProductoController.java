package es.unex.cum.mdai.motoresbits.web.controller;

import es.unex.cum.mdai.motoresbits.service.ProductoService;
import es.unex.cum.mdai.motoresbits.service.ResenaService;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.model.entity.Resena;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/product")
public class ProductoController {

    private final ProductoService productoService;
    private final ResenaService resenaService;

    public ProductoController(ProductoService productoService, ResenaService resenaService) {
        this.productoService = productoService;
        this.resenaService = resenaService;
    }

    @GetMapping("/{id}")
    public String verProducto(@PathVariable Long id, Model model) {
        Producto producto = productoService.getById(id);
        if (producto == null) {
            return "error/404"; // o redirigir a una página de error
        }

        List<Resena> resenas = resenaService.listarPorProducto(id);
        model.addAttribute("producto", producto);
        model.addAttribute("resenas", resenas);
        return "producto"; // este es tu HTML
    }

    @PostMapping("/id/{reseña}")
    public String publicarReseña(@PathVariable Long id,
                                 @RequestParam int puntuacion,
                                 @RequestParam String comentario,
                                 HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }

        resenaService.crearResena(usuarioId, id, puntuacion, comentario);
        return "redirect:/producto/" + id;
    }

}
