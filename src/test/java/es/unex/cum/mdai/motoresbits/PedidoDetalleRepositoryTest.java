package es.unex.cum.mdai.motoresbits;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import es.unex.cum.mdai.motoresbits.data.model.entity.Categoria;
import es.unex.cum.mdai.motoresbits.data.model.entity.Pedido;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.model.entity.Usuario;
import es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido;
import es.unex.cum.mdai.motoresbits.data.model.enums.RolUsuario;
import es.unex.cum.mdai.motoresbits.data.repository.CategoriaRepository;
import es.unex.cum.mdai.motoresbits.data.repository.DetallePedidoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.PedidoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ProductoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.UsuarioRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PedidoDetalleRepositoryTest {

    @Autowired UsuarioRepository usuarioRepository;
    @Autowired CategoriaRepository categoriaRepository;
    @Autowired ProductoRepository productoRepository;
    @Autowired PedidoRepository pedidoRepository;
    @Autowired DetallePedidoRepository detallePedidoRepository;

    @Test
    void crearPedidoConLineas_y_consultarPorUsuario_y_porPedidoYProducto() {
        // ---------- GIVEN ----------
        // Usuario
        Usuario u = new Usuario();
        u.setNombre("Carlos");
        u.setEmail("carlos@test.com");
        u.setContrasena("x");
        u.setRol(RolUsuario.CLIENTE);
        u = usuarioRepository.save(u);

        // Categoría
        Categoria cat = new Categoria();
        cat.setNombre("Filtros");
        cat.setDescripcion("Aceite/Aire");
        cat = categoriaRepository.save(cat);

        // Producto
        Producto prod = new Producto();
        prod.setNombre("Filtro aceite");
        prod.setReferencia("FA-123");
        prod.setPrecio(new BigDecimal("12.50"));
        prod.setStock(100);
        prod.setCategoria(cat);
        prod = productoRepository.save(prod);

        // Pedido (se guarda antes para que tenga ID y MapsId funcione)
        Pedido pedido = new Pedido();
        pedido.setUsuario(u);
        pedido.setFechaPedido(LocalDate.now());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setTotal(new BigDecimal("25.00"));
        pedido = pedidoRepository.save(pedido);

        // Línea usando el método de dominio (no construimos la PK a mano)
        pedido.addLinea(prod, 2, prod.getPrecio());

        // Si no confías en cascada PERSIST/MERGE, descomenta la siguiente línea:
        // detallePedidoRepository.saveAll(pedido.getDetalles());

        // ---------- WHEN ----------
        // 1) Consultar pedidos del usuario
        var pedidosUsuario = pedidoRepository.findByUsuarioId(u.getId());

        // 2) Consultar la línea por pedido y producto sin crear la PK compuesta
        var detalle = detallePedidoRepository.findByPedido_IdAndProducto_Id(
                pedido.getId(), prod.getId()
        );

        // ---------- THEN ----------
        assertThat(pedidosUsuario).hasSize(1);
        assertThat(detalle).isPresent();
        assertThat(detalle.get().getCantidad()).isEqualTo(2);
        assertThat(detalle.get().getPrecio()).isEqualByComparingTo("12.50");
    }
}
