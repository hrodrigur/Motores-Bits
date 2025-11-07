package es.unex.cum.mdai.motoresbits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import es.unex.cum.mdai.motoresbits.data.model.entity.Usuario;
import es.unex.cum.mdai.motoresbits.data.model.enums.RolUsuario;
import es.unex.cum.mdai.motoresbits.data.repository.UsuarioRepository;
import es.unex.cum.mdai.motoresbits.support.BaseJpaTest;
import es.unex.cum.mdai.motoresbits.support.TestDataFactory;

/**
 * Casos de uso relacionados con usuarios: registro y login (simulado sin encoder).
 * Los tests validan tanto el flujo exitoso como fallos esperados (unicidad, credenciales).
 */
class UsuarioUseCasesTest extends BaseJpaTest {

    @Autowired TestDataFactory f;
    @Autowired UsuarioRepository usuarioRepo;

    // Registro: éxito
    @Test
    void registro_creaUsuarioCliente_y_sePuedeLeerPorEmail() {
        String email = "nuevo@test.com";
        var u = new Usuario();
        u.setNombre("Nuevo");
        u.setEmail(email);
        u.setContrasena("secreto");
        u.setRol(RolUsuario.CLIENTE);

        var saved = usuarioRepo.saveAndFlush(u);

        assertThat(saved.getId()).isNotNull();
        var reloaded = usuarioRepo.findByEmail(email).orElseThrow();
        assertThat(reloaded.getNombre()).isEqualTo("Nuevo");
        assertThat(reloaded.getRol()).isEqualTo(RolUsuario.CLIENTE);
    }

    // Registro: fallo por email duplicado
    @Test
    void registro_falla_siEmailDuplicado() {
        var existente = f.newUsuarioPersisted();

        var u2 = new Usuario();
        u2.setNombre("Repetido");
        u2.setEmail(existente.getEmail());
        u2.setContrasena("x");
        u2.setRol(RolUsuario.CLIENTE);

        assertThatThrownBy(() -> usuarioRepo.saveAndFlush(u2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Login: éxito (simulado)
    @Test
    void login_correcto_conEmailYContrasenaValidos() {
        var u = f.newUsuarioPersisted();
        var encontrado = usuarioRepo.findByEmail(u.getEmail()).orElseThrow();

        assertThat(encontrado.getContrasena()).isEqualTo(u.getContrasena());
    }

    // CU-02 Login: fallo por email inexistente
    @Test
    void login_falla_siEmailNoExiste() {
        var inexistente = usuarioRepo.findByEmail("no@existe.test");
        assertThat(inexistente).isEmpty();
    }

    // CU-02 Login: fallo por contraseña incorrecta (simulado sin encoder)
    @Test
    void login_falla_siContrasenaIncorrecta() {
        var u = f.newUsuarioPersisted();          // contrasena "x"
        var encontrado = usuarioRepo.findByEmail(u.getEmail()).orElseThrow();

        // Simulación: aquí comprobamos que una contraseña incorrecta concreta no coincide
        // con la almacenada. Si en el proyecto se añade un PasswordEncoder, este test
        // deberá adaptarse para usar encoder.matches(raw, hashed).
        assertThat(encontrado.getContrasena()).isNotEqualTo("otra");
    }
}
