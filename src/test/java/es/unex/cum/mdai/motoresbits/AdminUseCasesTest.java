package es.unex.cum.mdai.motoresbits;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import es.unex.cum.mdai.motoresbits.data.model.entity.Categoria;
import es.unex.cum.mdai.motoresbits.data.model.entity.Pedido;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.model.entity.Resena;
import es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido;
import es.unex.cum.mdai.motoresbits.data.repository.CategoriaRepository;
import es.unex.cum.mdai.motoresbits.data.repository.PedidoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ProductoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ResenaRepository;
import es.unex.cum.mdai.motoresbits.data.repository.UsuarioRepository;
import es.unex.cum.mdai.motoresbits.support.BaseJpaTest;
import es.unex.cum.mdai.motoresbits.support.TestDataFactory;

class AdminUseCasesTest extends BaseJpaTest {

    @Autowired TestDataFactory f;
    @Autowired CategoriaRepository categoriaRepo;
    @Autowired ProductoRepository productoRepo;
    @Autowired PedidoRepository pedidoRepo;
    @Autowired UsuarioRepository usuarioRepo;
    @Autowired ResenaRepository resenaRepo;

    @Test
    void categoria_crud_basico() {
        var cat = new Categoria();
        cat.setNombre("Frenado");
        cat.setDescripcion("Sistema de frenos");
        cat = categoriaRepo.saveAndFlush(cat);

        assertThat(categoriaRepo.findAll()).extracting(Categoria::getNombre).contains("Frenado");

        cat.setNombre("Frenos");
        categoriaRepo.saveAndFlush(cat);
        var reloaded = categoriaRepo.findById(cat.getId()).orElseThrow();
        assertThat(reloaded.getNombre()).isEqualTo("Frenos");

        categoriaRepo.delete(reloaded);
        categoriaRepo.flush();
        assertThat(categoriaRepo.findById(cat.getId())).isEmpty();
    }

    @Test
    void producto_crud_y_listado_por_categoria() {
        var cat = f.newCategoriaPersisted("Aceites");

        var p = new Producto();
        p.setNombre("Aceite 5W30");
        p.setReferencia("OIL-5W30-ADMIN");
        p.setPrecio(new BigDecimal("29.90"));
        p.setStock(50);
        p.setCategoria(cat);
        p = productoRepo.saveAndFlush(p);

        p.setPrecio(new BigDecimal("27.50"));
        p.setStock(60);
        productoRepo.saveAndFlush(p);

        var reloaded = productoRepo.findById(p.getId()).orElseThrow();
        assertThat(reloaded.getPrecio()).isEqualByComparingTo("27.50");
        assertThat(reloaded.getStock()).isEqualTo(60);

        var lista = productoRepo.findByCategoriaId(cat.getId());
        assertThat(lista).extracting(Producto::getReferencia).contains("OIL-5W30-ADMIN");

        productoRepo.delete(reloaded);
        productoRepo.flush();
        assertThat(productoRepo.findById(p.getId())).isEmpty();
    }

    @Test
    void pedido_maestro_detalle_con_lineas_y_productos() {
        var u = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Frenos");
        var pr1 = f.newProductoPersisted(c, "PF", new BigDecimal("19.95"));
        var pr2 = f.newProductoPersisted(c, "DF", new BigDecimal("59.90"));

        var pedido = new Pedido();
        pedido.setUsuario(u);
        pedido.setFechaPedido(LocalDate.now());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setTotal(new BigDecimal("79.85"));
        pedido = pedidoRepo.save(pedido);

        pedido.addLinea(pr1, 1, pr1.getPrecio());
        pedido.addLinea(pr2, 1, pr2.getPrecio());
        pedidoRepo.saveAndFlush(pedido);

        var cargado = pedidoRepo.findConLineasYProductos(pedido.getId()).orElseThrow();
        assertThat(cargado.getDetalles()).hasSize(2);
        assertThat(cargado.getDetalles()).allSatisfy(d -> {
            assertThat(d.getProducto()).isNotNull();
            assertThat(d.getPrecio()).isNotNull();
        });
    }

    @Test
    void pedido_actualizar_estado() {
        var u = f.newUsuarioPersisted();
        var pedido = new Pedido();
        pedido.setUsuario(u);
        pedido.setFechaPedido(LocalDate.now());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setTotal(new BigDecimal("0.00"));
        pedido = pedidoRepo.saveAndFlush(pedido);

        pedido.setEstado(EstadoPedido.PAGADO);
        pedidoRepo.saveAndFlush(pedido);
        assertThat(pedidoRepo.findById(pedido.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.PAGADO);

        pedido.setEstado(EstadoPedido.ENVIADO);
        pedidoRepo.saveAndFlush(pedido);
        assertThat(pedidoRepo.findById(pedido.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.ENVIADO);

        pedido.setEstado(EstadoPedido.ENTREGADO);
        pedidoRepo.saveAndFlush(pedido);
        assertThat(pedidoRepo.findById(pedido.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.ENTREGADO);
    }

    @Test
    void moderar_resenas_editar_y_eliminar() {
        var u = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Filtros");
        var prod = f.newProductoPersisted(c, "FA", new BigDecimal("12.50"));

        var r = new Resena();
        r.setUsuario(u);
        r.setProducto(prod);
        r.setPuntuacion(4);
        r.setComentario("Buena");
        r = resenaRepo.saveAndFlush(r);

        r.setPuntuacion(5);
        r.setComentario("Excelente");
        resenaRepo.saveAndFlush(r);

        var reloaded = resenaRepo.findById(r.getId()).orElseThrow();
        assertThat(reloaded.getPuntuacion()).isEqualTo(5);
        assertThat(reloaded.getComentario()).isEqualTo("Excelente");

        resenaRepo.delete(reloaded);
        resenaRepo.flush();
        assertThat(resenaRepo.findById(r.getId())).isEmpty();
    }
}
