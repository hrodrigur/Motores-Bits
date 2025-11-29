package es.unex.cum.mdai.motoresbits.web.controller;

import es.unex.cum.mdai.motoresbits.service.CatalogoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final CatalogoService catalogoService;

    public HomeController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/")
    public String index(Model model) {
        // Mostrar la página de inicio (index) independientemente de la sesión
        model.addAttribute("categorias", catalogoService.listarCategorias());
        model.addAttribute("productos", catalogoService.listarProductos());
        return "index";
    }
}
