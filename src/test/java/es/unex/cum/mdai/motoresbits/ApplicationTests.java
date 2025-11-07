package es.unex.cum.mdai.motoresbits;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test mínimo que arranca el contexto de Spring Boot con el perfil de pruebas.
 * Útil para detectar errores de wiring, configuración o beans faltantes al inicio.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {
    @Test void contextLoads() {}
}
