package es.unex.cum.mdai.motoresbits.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

@Component
public class PdfGenerator {

    private static final Logger logger = LoggerFactory.getLogger(PdfGenerator.class);

    public byte[] generatePdfFromHtml(String html) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);

            // Registrar una fuente si existe en resources/static/fonts
            File tmpFontFile = null;
            try {
                ClassPathResource fontRes = new ClassPathResource("static/fonts/NotoSans-Regular.ttf");
                if (fontRes.exists()) {
                    tmpFontFile = File.createTempFile("font", ".ttf");
                    try (InputStream is = fontRes.getInputStream()) {
                        Files.copy(is, tmpFontFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        builder.useFont(tmpFontFile, "Noto Sans");
                        logger.info("Fuente embebida registrada para PDF: {}", tmpFontFile.getAbsolutePath());
                    }
                } else {
                    logger.info("No se encontró fuente embebida en classpath static/fonts/NotoSans-Regular.ttf — se usará el mapeo de fuentes del sistema si es necesario.");
                }
            } catch (Exception ex) {
                logger.warn("No se pudo registrar la fuente embebida para PDF (se ignorará y probablemente se usen fuentes del sistema)", ex);
                // no rethrow; dejamos que el render siga y use fuentes del sistema o las builtin
            }

            builder.toStream(baos);
            builder.run();

            // intentar eliminar el archivo temporal si se creó
            if (tmpFontFile != null && tmpFontFile.exists()) {
                try {
                    Files.deleteIfExists(tmpFontFile.toPath());
                } catch (Exception ex) {
                    // no crítico
                    logger.debug("No se pudo borrar archivo temporal de fuente: {}", tmpFontFile.getAbsolutePath(), ex);
                }
            }

            return baos.toByteArray();
        } catch (Exception e) {
            logger.error("Error generando PDF", e);
            throw new RuntimeException("Error generando PDF", e);
        }
    }
}
