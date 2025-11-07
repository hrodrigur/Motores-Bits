package es.unex.cum.mdai.motoresbits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import es.unex.cum.mdai.motoresbits.data.model.entity.Resena;
import es.unex.cum.mdai.motoresbits.data.model.entity.Usuario;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.repository.ResenaRepository;
import es.unex.cum.mdai.motoresbits.support.BaseJpaTest;
import es.unex.cum.mdai.motoresbits.support.TestDataFactory;
import jakarta.validation.ConstraintViolationException;

/**
 * Pruebas para validar el comportamiento del repositorio de `Resena`:
 * - creación y búsqueda por producto
 * - validación de rango de puntuación (1..5)
 * - agregación (media) de puntuaciones
 */
class ResenasRepositoryTest extends BaseJpaTest {

    @Autowired TestDataFactory f;
    @Autowired ResenaRepository resenaRepo;

    @Test
    void crearResena_y_buscarPorProducto() {
        var u = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Frenos");
        var p = f.newProductoPersisted(c, "PF", new BigDecimal("19.95"));

        var r = new Resena();
        r.setUsuario(u);
        r.setProducto(p);
        r.setPuntuacion(5);
        r.setComentario("Muy buenas");
        try { r.setCreadaEn(LocalDateTime.now()); } catch (Throwable ignored) {}
        resenaRepo.saveAndFlush(r);

        var resenas = resenaRepo.findByProductoId(p.getId());
        assertThat(resenas).hasSize(1);
        assertThat(resenas.get(0).getUsuario().getEmail()).isEqualTo(u.getEmail());
    }

    @Test
    void validacion_puntuacion_1a5_debeFallarSiSeExcede() {
        var u = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Frenos");
        var p = f.newProductoPersisted(c, "PF", new BigDecimal("19.95"));

        var rInv = new Resena();
        rInv.setUsuario(u);
        rInv.setProducto(p);
        rInv.setPuntuacion(6); // inválida
        rInv.setComentario("exceso");

        assertThatThrownBy(() -> resenaRepo.saveAndFlush(rInv))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void mediaDePuntuaciones_porProducto() {
        var u1 = f.newUsuarioPersisted();
        var u2 = f.newUsuarioPersisted();
        var u3 = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Frenos");
        var p = f.newProductoPersisted(c, "PF", new BigDecimal("10.00"));

        resenaRepo.save(nuevaResena(u1, p, 5));
        resenaRepo.save(nuevaResena(u2, p, 4));
        resenaRepo.saveAndFlush(nuevaResena(u3, p, 3));

        var media = resenaRepo.avgPuntuacionByProductoId(p.getId()).orElseThrow();
        assertThat(media).isCloseTo(4.0, org.assertj.core.data.Offset.offset(1e-4));
    }

    // helper
    private Resena nuevaResena(Usuario u, Producto p, int puntuacion) {
        var r = new Resena();
        r.setUsuario(u);
        r.setProducto(p);
        r.setPuntuacion(puntuacion);
        r.setComentario("ok");
        try { r.setCreadaEn(LocalDateTime.now()); } catch (Throwable ignored) {}
        return r;
    }
}
