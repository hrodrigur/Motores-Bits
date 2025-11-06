package es.unex.cum.mdai.motoresbits;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.repository.ProductoRepository;
import es.unex.cum.mdai.motoresbits.support.BaseJpaTest;
import es.unex.cum.mdai.motoresbits.support.TestDataFactory;

class ProductoUnicidadReferenciaTest extends BaseJpaTest {

    @Autowired TestDataFactory f;
    @Autowired ProductoRepository productoRepo;

    @Test
    void noPermiteReferenciaDuplicada() {
        var cat = f.newCategoriaPersisted("Filtros");
        var p1 = f.newProductoPersisted(cat, "REFX", new BigDecimal("10.00"));

        var p2 = new Producto();
        p2.setNombre("Otro");
        p2.setReferencia(p1.getReferencia());  // duplicada
        p2.setPrecio(new BigDecimal("9.99"));
        p2.setStock(10);
        p2.setCategoria(cat);

        // La violación salta en save (o en saveAndFlush), así que la aserción envuelve esa llamada
        assertThatThrownBy(() -> productoRepo.saveAndFlush(p2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
