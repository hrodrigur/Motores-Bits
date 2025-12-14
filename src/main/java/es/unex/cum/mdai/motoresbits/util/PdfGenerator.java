package es.unex.cum.mdai.motoresbits.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;

@Component
public class PdfGenerator {

    public byte[] generatePdfFromHtml(String html) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);

            // Registrar una fuente si existe en resources/static/fonts
            try {
                ClassPathResource fontRes = new ClassPathResource("static/fonts/NotoSans-Regular.ttf");
                if (fontRes.exists()) {
                    File f = File.createTempFile("font", ".ttf");
                    Files.copy(fontRes.getInputStream(), f.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    builder.useFont(f, "Noto Sans");
                }
            } catch (Exception ex) {
                // ignorar si no hay fuente personalizada
            }

            builder.toStream(baos);
            builder.run();

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF", e);
        }
    }
}
