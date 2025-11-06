package es.unex.cum.mdai.motoresbits;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import es.unex.cum.mdai.motoresbits.data.model.entity.DetallePedido;
import es.unex.cum.mdai.motoresbits.data.repository.DetallePedidoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.PedidoRepository;
import es.unex.cum.mdai.motoresbits.support.BaseJpaTest;
import es.unex.cum.mdai.motoresbits.support.TestDataFactory;
import es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido;

class PedidoOrphanRemovalTest extends BaseJpaTest {

    @Autowired TestDataFactory f;
    @Autowired PedidoRepository pedidoRepo;
    @Autowired DetallePedidoRepository detalleRepo;

    @Test
    void quitarLinea_laBorraDeBD() {
        var u = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Aceites");
        var p1 = f.newProductoPersisted(c, "OIL", new BigDecimal("25.00"));

        var pedido = f.newPedidoPersisted(u, new BigDecimal("25.00"), EstadoPedido.PENDIENTE);
        var linea = pedido.addLinea(p1, 1, p1.getPrecio());

        // cascada al guardar el pedido
        pedidoRepo.saveAndFlush(pedido);
        assertThat(detalleRepo.count()).isEqualTo(1);

        // eliminar y flushear
        pedido.removeLinea(linea);
        pedidoRepo.saveAndFlush(pedido);

        assertThat(detalleRepo.count()).isZero();
    }
}
