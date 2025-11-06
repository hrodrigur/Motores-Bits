package es.unex.cum.mdai.motoresbits;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import es.unex.cum.mdai.motoresbits.data.model.entity.Resena;
import es.unex.cum.mdai.motoresbits.data.repository.ResenaRepository;
import es.unex.cum.mdai.motoresbits.support.BaseJpaTest;
import es.unex.cum.mdai.motoresbits.support.TestDataFactory;

class ResenaMediaPorProductoTest extends BaseJpaTest {

    @Autowired TestDataFactory f;
    @Autowired ResenaRepository resenaRepo;

    @Test
    void calculaMediaDePuntuaciones() {
        var u1 = f.newUsuarioPersisted();
        var u2 = f.newUsuarioPersisted();
        var u3 = f.newUsuarioPersisted();

        var c = f.newCategoriaPersisted("Frenos");
        var prod = f.newProductoPersisted(c, "PF", new BigDecimal("10.00"));

        resenaRepo.save(nuevaResena(u1, prod, 5));
        resenaRepo.save(nuevaResena(u2, prod, 4));
        resenaRepo.save(nuevaResena(u3, prod, 3));
        resenaRepo.flush();

        var media = resenaRepo.avgPuntuacionByProductoId(prod.getId()).orElseThrow();
        assertThat(media).isCloseTo(4.0, org.assertj.core.data.Offset.offset(0.0001));
    }

    private Resena nuevaResena(es.unex.cum.mdai.motoresbits.data.model.entity.Usuario u,
                               es.unex.cum.mdai.motoresbits.data.model.entity.Producto p,
                               int puntuacion) {
        var r = new Resena();
        r.setUsuario(u);
        r.setProducto(p);
        r.setPuntuacion(puntuacion);
        r.setComentario("ok");
        try { r.setCreadaEn(LocalDateTime.now()); } catch (Throwable ignored) {}
        return r;
    }
}
