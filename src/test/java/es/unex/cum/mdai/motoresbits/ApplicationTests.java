package es.unex.cum.mdai.motoresbits;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test mínimo que arranca el contexto de Spring Boot con el perfil de pruebas.
 * Mantener este test ayuda a detectar problemas de wiring o configuración.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {
    @Test void contextLoads() {}
}
