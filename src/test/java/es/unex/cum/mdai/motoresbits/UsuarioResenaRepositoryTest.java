package es.unex.cum.mdai.motoresbits;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import es.unex.cum.mdai.motoresbits.data.model.entity.Categoria;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.model.entity.Resena;
import es.unex.cum.mdai.motoresbits.data.repository.CategoriaRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ProductoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ResenaRepository;
import es.unex.cum.mdai.motoresbits.data.repository.UsuarioRepository;
import es.unex.cum.mdai.motoresbits.support.BaseJpaTest;
import es.unex.cum.mdai.motoresbits.support.TestDataFactory;

class UsuarioResenaRepositoryTest extends BaseJpaTest {

    @Autowired TestDataFactory f;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired CategoriaRepository categoriaRepository;
    @Autowired ProductoRepository productoRepository;
    @Autowired ResenaRepository resenaRepository;

    @Test
    void crearUsuarioProductoYResena_y_buscarResenasPorProducto() {
        var u = f.newUsuarioPersisted();

        Categoria cat = f.newCategoriaPersisted("Frenos");
        Producto p = f.newProductoPersisted(cat, "PF", new BigDecimal("19.95"));

        Resena r = new Resena();
        r.setUsuario(u);
        r.setProducto(p);
        r.setPuntuacion(5);
        r.setComentario("Muy buenas");
        try { r.setCreadaEn(LocalDateTime.now()); } catch (Throwable ignored) {}
        resenaRepository.save(r);

        var resenas = resenaRepository.findByProductoId(p.getId());

        assertThat(resenas).hasSize(1);
        assertThat(resenas.get(0).getUsuario().getEmail()).isEqualTo(u.getEmail());
    }
}
