package es.unex.cum.mdai.motoresbits;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import es.unex.cum.mdai.motoresbits.data.model.entity.Categoria;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.repository.CategoriaRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ProductoRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CategoriaProductoRepositoryTest {

    @Autowired CategoriaRepository categoriaRepository;
    @Autowired ProductoRepository productoRepository;

    @Test
    void crearCategoriaConProductos_y_buscarPorCategoriaId() {
        // given
        var cat = new Categoria();
        cat.setNombre("Aceites");
        cat.setDescripcion("Lubricantes");
        cat = categoriaRepository.save(cat);

        var p1 = new Producto();
        p1.setNombre("5W30");
        p1.setReferencia("OIL-5W30");
        p1.setPrecio(new BigDecimal("29.90"));
        p1.setStock(20);
        p1.setCategoria(cat);
        productoRepository.save(p1);

        var p2 = new Producto();
        p2.setNombre("10W40");
        p2.setReferencia("OIL-10W40");
        p2.setPrecio(new BigDecimal("24.90"));
        p2.setStock(35);
        p2.setCategoria(cat);
        productoRepository.save(p2);

        // when
        var encontrados = productoRepository.findByCategoriaId(cat.getId());

        // then
        assertThat(encontrados).hasSize(2);
        assertThat(encontrados)
                .extracting(Producto::getReferencia)
                .containsExactlyInAnyOrder("OIL-5W30", "OIL-10W40");
    }
}
