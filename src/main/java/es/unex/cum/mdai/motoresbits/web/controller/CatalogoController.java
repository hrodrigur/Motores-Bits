package es.unex.cum.mdai.motoresbits.web.controller;

import es.unex.cum.mdai.motoresbits.data.model.entity.Categoria;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.service.CatalogoService;
import es.unex.cum.mdai.motoresbits.service.ResenaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CatalogoController {

    private final CatalogoService catalogoService;
    private final ResenaService resenaService;

    public CatalogoController(CatalogoService catalogoService, ResenaService resenaService) {
        this.catalogoService = catalogoService;
        this.resenaService = resenaService;
    }

    @GetMapping("/catalogo")
    public String catalogo(Model model) {
        model.addAttribute("categorias", catalogoService.listarCategorias());
        model.addAttribute("productos", catalogoService.listarProductos());
        return "catalogo";
    }

    @GetMapping("/categoria/{id}")
    public String categoria(@PathVariable Long id, Model model) {
        Categoria c = catalogoService.obtenerCategoria(id);
        model.addAttribute("categoria", c);
        model.addAttribute("productos", catalogoService.listarPorCategoria(id));
        return "categoria";
    }

    @GetMapping("/producto/{id}")
    public String productoDetalle(@PathVariable Long id, Model model) {
        Producto p = catalogoService.obtenerProducto(id);
        if (p == null) {
            return "error/404";
        }

        model.addAttribute("producto", p);
        model.addAttribute("resenas", resenaService.listarPorProducto(id));
        model.addAttribute("mediaPuntuacion",
                resenaService.mediaPuntuacionPorProducto(id).orElse(null));

        return "producto-detalle";
    }

}
