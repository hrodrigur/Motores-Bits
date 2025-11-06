package es.unex.cum.mdai.motoresbits;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import es.unex.cum.mdai.motoresbits.data.model.entity.Resena;
import es.unex.cum.mdai.motoresbits.data.repository.ResenaRepository;
import es.unex.cum.mdai.motoresbits.support.BaseJpaTest;
import es.unex.cum.mdai.motoresbits.support.TestDataFactory;
import jakarta.validation.ConstraintViolationException;

class ResenaValidacionPuntuacionTest extends BaseJpaTest {

    @Autowired TestDataFactory f;
    @Autowired ResenaRepository resenaRepo;

    @Test
    void puntuacionDebeEstarEntre1y5() {
        var u = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Frenos");
        var p = f.newProductoPersisted(c, "PF", new BigDecimal("19.95"));

        var r = new Resena();
        r.setUsuario(u);
        r.setProducto(p);
        r.setPuntuacion(6); // inválida (Max(5))
        r.setComentario("exceso");
        try { r.setCreadaEn(LocalDateTime.now()); } catch (Throwable ignored) {}

        // La validación salta en la operación que ejecuta el INSERT
        assertThatThrownBy(() -> resenaRepo.saveAndFlush(r))
                .isInstanceOf(ConstraintViolationException.class);
    }
}
