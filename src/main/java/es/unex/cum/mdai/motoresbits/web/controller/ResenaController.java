package es.unex.cum.mdai.motoresbits.web.controller;

import es.unex.cum.mdai.motoresbits.service.ResenaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Controlador que gestiona la creación y eliminación de reseñas de productos.
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

        return "redirect:/producto/" + id;
    }

    @PostMapping("/resena/eliminar")
    public String eliminarResena(@RequestParam Long idResena, HttpSession session, RedirectAttributes redirectAttributes) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }

        try {
            var r = resenaService.obtenerResena(idResena);
            Long productoId = r.getProducto() != null ? r.getProducto().getId() : null;
            Long autorId = r.getUsuario() != null ? r.getUsuario().getId() : null;
            if (!usuarioId.equals(autorId)) {
                redirectAttributes.addFlashAttribute("resenaError", "No tienes permiso para eliminar esta reseña.");
                return productoId != null ? "redirect:/producto/" + productoId : "redirect:/";
            }

            resenaService.eliminarResena(idResena);
            redirectAttributes.addFlashAttribute("resenaOk", "Reseña eliminada correctamente.");
            return productoId != null ? "redirect:/producto/" + productoId : "redirect:/";
         } catch (Exception ex) {
             redirectAttributes.addFlashAttribute("resenaError", ex.getMessage());
             try { var r2 = resenaService.obtenerResena(idResena); return r2.getProducto() != null ? "redirect:/producto/" + r2.getProducto().getId() : "redirect:/"; } catch (Exception e) { return "redirect:/"; }
         }
    }
}
