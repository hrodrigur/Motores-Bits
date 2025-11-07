package es.unex.cum.mdai.motoresbits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.repository.ProductoRepository;
import es.unex.cum.mdai.motoresbits.support.BaseJpaTest;
import es.unex.cum.mdai.motoresbits.support.TestDataFactory;

/**
 * Tests para repositorios `Producto` y `Categoria`.
 *
 * Objetivo:
 * - Verificar la creación de categorías y el listado de productos por categoría.
 * - Validar la restricción de unicidad en la referencia de producto.
 *
 * Cada test es autocontenido y persistente en la BBDD de pruebas (profil `test`).
 */
class ProductosCategoriasRepositoryTest extends BaseJpaTest {

    @Autowired TestDataFactory f;
    @Autowired ProductoRepository productoRepo;

    @Test
    void categoria_crear_y_listarProductosPorCategoria() {
        var cat = f.newCategoriaPersisted("Aceites");
        var p1 = f.newProductoPersisted(cat, "OIL-5W30", new BigDecimal("29.90"));
        var p2 = f.newProductoPersisted(cat, "OIL-10W40", new BigDecimal("24.90"));

        var encontrados = productoRepo.findByCategoriaId(cat.getId());
        assertThat(encontrados).hasSize(2);
        assertThat(encontrados).extracting(Producto::getReferencia)
                .containsExactlyInAnyOrder(p1.getReferencia(), p2.getReferencia());
    }

    @Test
    void producto_unicidadReferencia_debeLanzarExcepcion() {
        var cat = f.newCategoriaPersisted("Filtros");
        var p1 = f.newProductoPersisted(cat, "REFX", new BigDecimal("10.00"));

        var p2 = new Producto();
        p2.setNombre("Otro");
        p2.setReferencia(p1.getReferencia()); // duplicada intencionadamente
        p2.setPrecio(new BigDecimal("9.99"));
        p2.setStock(10);
        p2.setCategoria(cat);

        assertThatThrownBy(() -> productoRepo.saveAndFlush(p2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
