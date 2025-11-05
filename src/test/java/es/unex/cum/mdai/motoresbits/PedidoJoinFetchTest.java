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
import es.unex.cum.mdai.motoresbits.data.repository.PedidoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ProductoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.UsuarioRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PedidoJoinFetchTest {

    @Autowired UsuarioRepository usuarioRepository;
    @Autowired CategoriaRepository categoriaRepository;
    @Autowired ProductoRepository productoRepository;
    @Autowired PedidoRepository pedidoRepository;

    @Test
    void traerPedidoConLineasYProductos_enUnaSolaConsulta() {
        // ---------- GIVEN ----------
        // Usuario
        var u = new Usuario();
        u.setNombre("Laura");
        u.setEmail("laura@test.com");
        u.setContrasena("x");
        u.setRol(RolUsuario.CLIENTE);
        u = usuarioRepository.save(u);

        // Categoría + Productos
        var cat = new Categoria();
        cat.setNombre("Frenos");
        cat.setDescripcion("Pastillas y discos");
        cat = categoriaRepository.save(cat);

        var p1 = new Producto();
        p1.setNombre("Pastillas freno");
        p1.setReferencia("PF-001");
        p1.setPrecio(new BigDecimal("19.95"));
        p1.setStock(40);
        p1.setCategoria(cat);
        p1 = productoRepository.save(p1);

        var p2 = new Producto();
        p2.setNombre("Disco freno");
        p2.setReferencia("DF-010");
        p2.setPrecio(new BigDecimal("59.90"));
        p2.setStock(20);
        p2.setCategoria(cat);
        p2 = productoRepository.save(p2);

        // Pedido
        var pedido = new Pedido();
        pedido.setUsuario(u);
        pedido.setFechaPedido(LocalDate.now());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setTotal(new BigDecimal("99.85"));
        pedido = pedidoRepository.save(pedido);

        // Líneas (no creamos PK a mano)
        pedido.addLinea(p1, 1, p1.getPrecio());
        pedido.addLinea(p2, 1, p2.getPrecio());
        // Si no confías en cascade, guarda explícitamente:
        // detallePedidoRepository.saveAll(pedido.getDetalles());

        // ---------- WHEN ----------
        var pedidoCargado = pedidoRepository.findConLineasYProductos(pedido.getId())
                .orElseThrow();

        // ---------- THEN ----------
        assertThat(pedidoCargado.getId()).isEqualTo(pedido.getId());
        assertThat(pedidoCargado.getDetalles()).hasSize(2);
        // Aseguramos que los productos vienen cargados y accesibles
        assertThat(pedidoCargado.getDetalles())
                .allSatisfy(d -> {
                    assertThat(d.getProducto()).isNotNull();
                    assertThat(d.getProducto().getReferencia()).isNotBlank();
                });
    }
}
