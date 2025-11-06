package es.unex.cum.mdai.motoresbits;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.repository.ProductoRepository;
import es.unex.cum.mdai.motoresbits.support.BaseJpaTest;
import es.unex.cum.mdai.motoresbits.support.TestDataFactory;

class CategoriaProductoRepositoryTest extends BaseJpaTest {

    @Autowired TestDataFactory f;
    @Autowired ProductoRepository productoRepository;

    @Test
    void crearCategoriaConProductos_y_buscarPorCategoriaId() {
        var cat = f.newCategoriaPersisted("Aceites");

        var p1 = f.newProductoPersisted(cat, "OIL-5W30", new BigDecimal("29.90"));
        var p2 = f.newProductoPersisted(cat, "OIL-10W40", new BigDecimal("24.90"));

        var encontrados = productoRepository.findByCategoriaId(cat.getId());

        assertThat(encontrados).hasSize(2);
        assertThat(encontrados)
                .extracting(Producto::getReferencia)
                .containsExactlyInAnyOrder(p1.getReferencia(), p2.getReferencia());
    }
}
