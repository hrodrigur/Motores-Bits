package es.unex.cum.mdai.motoresbits;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Arranca el contexto de Spring con el perfil de test para detectar problemas de wiring.
@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {
    @Test void contextLoads() {}
}
