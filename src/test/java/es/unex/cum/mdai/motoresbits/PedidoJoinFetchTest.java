package es.unex.cum.mdai.motoresbits;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import es.unex.cum.mdai.motoresbits.data.repository.DetallePedidoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido;
import es.unex.cum.mdai.motoresbits.data.repository.PedidoRepository;
import es.unex.cum.mdai.motoresbits.support.BaseJpaTest;
import es.unex.cum.mdai.motoresbits.support.TestDataFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PedidoJoinFetchTest {

    @Autowired TestDataFactory f;
    @Autowired PedidoRepository pedidoRepository;
    @Autowired DetallePedidoRepository detalleRepo;

    @Test
    void traerPedidoConLineasYProductos_enUnaSolaConsulta() {
        var u = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Frenos");
        var p1 = f.newProductoPersisted(c, "PF", new BigDecimal("19.95"));
        var p2 = f.newProductoPersisted(c, "DF", new BigDecimal("59.90"));

        var pedido = f.newPedidoPersisted(u, new BigDecimal("79.85"), EstadoPedido.PENDIENTE);
        f.addLineaPersisted(pedido, p1, 1, p1.getPrecio());
        f.addLineaPersisted(pedido, p2, 1, p2.getPrecio());

        // Diagnóstico
        assertThat(detalleRepo.count()).as("detalles en BD").isGreaterThanOrEqualTo(2);
        assertThat(pedido.getDetalles()).as("colección en memoria").hasSize(2);
        pedidoRepository.flush();

        var pedidoCargado = pedidoRepository.findConLineasYProductos(pedido.getId()).orElseThrow();

        assertThat(pedidoCargado.getDetalles()).hasSize(2);
        assertThat(pedidoCargado.getDetalles())
                .allSatisfy(d -> {
                    assertThat(d.getProducto()).isNotNull();
                    assertThat(d.getProducto().getReferencia()).isNotBlank();
                });
    }
}