package es.unex.cum.mdai.motoresbits;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import es.unex.cum.mdai.motoresbits.data.model.entity.Usuario;
import es.unex.cum.mdai.motoresbits.data.repository.UsuarioRepository;
import es.unex.cum.mdai.motoresbits.support.BaseJpaTest;
import es.unex.cum.mdai.motoresbits.support.TestDataFactory;

class UsuarioUnicidadEmailTest extends BaseJpaTest {

    @Autowired TestDataFactory f;
    @Autowired UsuarioRepository usuarioRepo;

    @Test
    void noPermiteEmailDuplicado() {
        var u1 = f.newUsuarioPersisted();

        var u2 = new Usuario();
        u2.setNombre("Otro");
        u2.setEmail(u1.getEmail());   // mismo email
        u2.setContrasena("x");
        u2.setRol(u1.getRol());

        // La violación salta en save (o en saveAndFlush), así que la aserción envuelve esa llamada
        assertThatThrownBy(() -> usuarioRepo.saveAndFlush(u2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
