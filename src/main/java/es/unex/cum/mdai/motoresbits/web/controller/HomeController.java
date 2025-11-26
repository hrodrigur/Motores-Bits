package es.unex.cum.mdai.motoresbits.web.controller;

import es.unex.cum.mdai.motoresbits.service.CatalogoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    private final CatalogoService catalogoService;

    public HomeController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        // Si no hay usuario en sesion, redirigimos al login
        Object usuarioId = session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }

        model.addAttribute("categorias", catalogoService.listarCategorias());
        model.addAttribute("productos", catalogoService.listarProductos());
        return "index";
    }
}
