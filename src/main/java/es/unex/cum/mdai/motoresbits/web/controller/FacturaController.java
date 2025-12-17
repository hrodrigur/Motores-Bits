package es.unex.cum.mdai.motoresbits.web.controller;

import es.unex.cum.mdai.motoresbits.service.PedidoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import es.unex.cum.mdai.motoresbits.util.PdfGenerator;
import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Genera facturas en PDF para pedidos y las devuelve en la respuesta HTTP
@Controller
public class FacturaController {

    private final PedidoService pedidoService;
    private final SpringTemplateEngine templateEngine;
    private final PdfGenerator pdfGenerator;

    public FacturaController(PedidoService pedidoService, SpringTemplateEngine templateEngine,
                             PdfGenerator pdfGenerator) {
        this.pedidoService = pedidoService;
        this.templateEngine = templateEngine;
        this.pdfGenerator = pdfGenerator;
    }

    @GetMapping(value = "/pedidos/{id}/factura", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarFactura(@PathVariable Long id, HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        Object rol = session.getAttribute("usuarioRol");
        if (usuarioId == null) {
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, "/login").build();
        }

        var pedido = pedidoService.obtenerPedido(id);
        boolean esAdmin = rol != null && "ADMIN".equals(rol.toString());
        if (!esAdmin && (pedido.getUsuario() == null || !usuarioId.equals(pedido.getUsuario().getId()))) {
            return ResponseEntity.status(403).build();
        }

        Context ctx = new Context();
        ctx.setVariable("pedido", pedido);
        if (pedido.getUsuario() != null) {
            ctx.setVariable("clienteNombre", pedido.getUsuario().getNombre());
            ctx.setVariable("clienteEmail", pedido.getUsuario().getEmail());
            ctx.setVariable("clienteDireccion", pedido.getUsuario().getDireccion());
        } else {
            ctx.setVariable("clienteNombre", "");
            ctx.setVariable("clienteEmail", "");
            ctx.setVariable("clienteDireccion", "");
        }

        List<Map<String, Object>> simpleDetalles = new ArrayList<>();
        if (pedido.getDetalles() != null) {
            for (var d : pedido.getDetalles()) {
                Map<String, Object> m = new HashMap<>();
                m.put("productoReferencia", d.getProducto() != null ? d.getProducto().getReferencia() : "");
                m.put("productoNombre", d.getProducto() != null ? d.getProducto().getNombre() : "");
                m.put("cantidad", d.getCantidad());
                m.put("precio", d.getPrecio());
                BigDecimal subtotal = d.getPrecio() == null ? BigDecimal.ZERO : d.getPrecio().multiply(BigDecimal.valueOf(d.getCantidad()));
                m.put("subtotal", subtotal);
                simpleDetalles.add(m);
            }
        }
        ctx.setVariable("detalles", simpleDetalles);

        String html = templateEngine.process("invoice", ctx);

        byte[] pdf = pdfGenerator.generatePdfFromHtml(html);

        String fileName = "factura-pedido-" + id + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(pdf);
    }
}
