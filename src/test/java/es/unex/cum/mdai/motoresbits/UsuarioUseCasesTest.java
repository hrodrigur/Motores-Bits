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

// Pruebas de casos de uso de usuario: registro y login básicos.
class UsuarioUseCasesTest extends BaseJpaTest {

    @Autowired TestDataFactory f;
    @Autowired UsuarioRepository usuarioRepo;

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

    @Test
    void login_correcto_conEmailYContrasenaValidos() {
        var u = f.newUsuarioPersisted();
        var encontrado = usuarioRepo.findByEmail(u.getEmail()).orElseThrow();

        assertThat(encontrado.getContrasena()).isEqualTo(u.getContrasena());
    }

    @Test
    void login_falla_siEmailNoExiste() {
        var inexistente = usuarioRepo.findByEmail("no@existe.test");
        assertThat(inexistente).isEmpty();
    }

    @Test
    void login_falla_siContrasenaIncorrecta() {
        var u = f.newUsuarioPersisted();
        var encontrado = usuarioRepo.findByEmail(u.getEmail()).orElseThrow();

        assertThat(encontrado.getContrasena()).isNotEqualTo("otra");
    }
}
