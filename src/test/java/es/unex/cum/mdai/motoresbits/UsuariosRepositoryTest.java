package es.unex.cum.mdai.motoresbits;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import es.unex.cum.mdai.motoresbits.data.model.entity.Usuario;
import es.unex.cum.mdai.motoresbits.data.model.enums.RolUsuario;
import es.unex.cum.mdai.motoresbits.data.repository.UsuarioRepository;
import es.unex.cum.mdai.motoresbits.support.BaseJpaTest;
import es.unex.cum.mdai.motoresbits.support.TestDataFactory;

class UsuariosRepositoryTest extends BaseJpaTest {

    @Autowired TestDataFactory f;
    @Autowired UsuarioRepository usuarioRepo;

    @Test
    void unicidadEmail_debeLanzarExcepcion() {
        var u1 = f.newUsuarioPersisted();

        var u2 = new Usuario();
        u2.setNombre("Otro");
        u2.setEmail(u1.getEmail()); // duplicado
        u2.setContrasena("x");
        u2.setRol(RolUsuario.CLIENTE);

        assertThatThrownBy(() -> usuarioRepo.saveAndFlush(u2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
