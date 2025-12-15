package es.unex.cum.mdai.motoresbits.web.controller;

import es.unex.cum.mdai.motoresbits.data.model.entity.Categoria;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.service.CatalogoService;
import es.unex.cum.mdai.motoresbits.service.ResenaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

    // ✅ Mantener /categoria/{id} (numérico)
    @GetMapping("/categoria/{id:\\d+}")
    public String verCategoriaPorId(@PathVariable Long id, Model model) {
        Categoria categoria = catalogoService.obtenerCategoria(id);
        model.addAttribute("categoria", categoria);
        model.addAttribute("productos", catalogoService.listarPorCategoria(id));
        return "categoria";
    }

    // ✅ Nuevo /categoria/{nombre}
    @GetMapping("/categoria/{nombre}")
    public String verCategoriaPorNombre(@PathVariable String nombre, Model model) {
        Categoria categoria = catalogoService.obtenerCategoriaPorNombre(nombre);
        model.addAttribute("categoria", categoria);
        model.addAttribute("productos", catalogoService.listarPorCategoria(categoria.getId()));
        return "categoria";
    }

    // ✅ Mantener /producto/{id} (numérico)
    @GetMapping("/producto/{id:\\d+}")
    public String productoDetallePorId(@PathVariable Long id, Model model) {
        Producto p = catalogoService.obtenerProducto(id);
        model.addAttribute("producto", p);
        model.addAttribute("resenas", resenaService.listarPorProducto(id));
        model.addAttribute("mediaPuntuacion", resenaService.mediaPuntuacionPorProducto(id).orElse(null));
        return "producto-detalle";
    }

    // ✅ NUEVO: /producto/{referencia} (ej: MV8-001)
    @GetMapping("/producto/{referencia}")
    public String productoDetallePorReferencia(@PathVariable String referencia, Model model) {
        Producto p = catalogoService.obtenerProductoPorReferencia(referencia);

        Long id = p.getId();
        model.addAttribute("producto", p);
        model.addAttribute("resenas", resenaService.listarPorProducto(id));
        model.addAttribute("mediaPuntuacion", resenaService.mediaPuntuacionPorProducto(id).orElse(null));
        return "producto-detalle";
    }
}
