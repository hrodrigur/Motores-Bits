package es.unex.cum.mdai.motoresbits.web.controller;

import es.unex.cum.mdai.motoresbits.service.ResenaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ResenaController {

    private final ResenaService resenaService;

    public ResenaController(ResenaService resenaService) {
        this.resenaService = resenaService;
    }

    @PostMapping("/producto/{id}/resena")
    public String crearResena(@PathVariable Long id,
                              @RequestParam Integer puntuacion,
                              @RequestParam String comentario,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }

        try {
            resenaService.crearResena(usuarioId, id, puntuacion, comentario);
            redirectAttributes.addFlashAttribute("resenaOk",
                    "Reseña guardada correctamente.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("resenaError", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("resenaError",
                    "No se ha podido guardar la reseña.");
        }

        // Volver al detalle del producto
        return "redirect:/producto/" + id;
    }
}
