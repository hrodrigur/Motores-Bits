package es.unex.cum.mdai.motoresbits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import es.unex.cum.mdai.motoresbits.data.model.entity.Categoria;
import es.unex.cum.mdai.motoresbits.data.model.entity.Pedido;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.model.entity.Resena;
import es.unex.cum.mdai.motoresbits.data.model.entity.Usuario;
import es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido;
import es.unex.cum.mdai.motoresbits.data.model.enums.RolUsuario;
import es.unex.cum.mdai.motoresbits.data.repository.CategoriaRepository;
import es.unex.cum.mdai.motoresbits.data.repository.DetallePedidoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.PedidoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ProductoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ResenaRepository;
import es.unex.cum.mdai.motoresbits.data.repository.UsuarioRepository;
import es.unex.cum.mdai.motoresbits.support.BaseJpaTest;
import es.unex.cum.mdai.motoresbits.support.TestDataFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ConstraintViolationException;
import org.assertj.core.data.Offset;

class GeneralSmokeTests extends BaseJpaTest {

    @Autowired TestDataFactory f;

    @Autowired UsuarioRepository usuarioRepo;
    @Autowired CategoriaRepository categoriaRepo;
    @Autowired ProductoRepository productoRepo;
    @Autowired PedidoRepository pedidoRepo;
    @Autowired DetallePedidoRepository detalleRepo;
    @Autowired ResenaRepository resenaRepo;

    @PersistenceContext
    private EntityManager em;

    // 1) Registro + login básico + unicidad email
    @Test
    void usuario_registroLogin_y_unicidadEmail() {
        String email = "smoke@test.com";

        var nuevo = new Usuario();
        nuevo.setNombre("Smoke");
        nuevo.setEmail(email);
        nuevo.setContrasena("secreto");
        nuevo.setRol(RolUsuario.CLIENTE);
        var saved = usuarioRepo.saveAndFlush(nuevo);

        assertThat(saved.getId()).isNotNull();
        var reloaded = usuarioRepo.findByEmail(email).orElseThrow();
        assertThat(reloaded.getContrasena()).isEqualTo("secreto");

        var duplicado = new Usuario();
        duplicado.setNombre("Dup");
        duplicado.setEmail(email); // mismísimo email
        duplicado.setContrasena("x");
        duplicado.setRol(RolUsuario.CLIENTE);
        assertThatThrownBy(() -> usuarioRepo.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // 2) Categorías/Productos: crear, listar por categoría, unicidad referencia
    @Test
    void productos_porCategoria_y_unicidadReferencia() {
        var cat = new Categoria();
        cat.setNombre("Aceites");
        cat.setDescripcion("Lubricantes");
        cat = categoriaRepo.saveAndFlush(cat);

        var p1 = new Producto();
        p1.setNombre("Aceite 5W30");
        p1.setReferencia("OIL-5W30-SMOKE");
        p1.setPrecio(new BigDecimal("29.90"));
        p1.setStock(50);
        p1.setCategoria(cat);
        p1 = productoRepo.saveAndFlush(p1);

        var p2 = f.newProductoPersisted(cat, "OIL-10W40", new BigDecimal("24.90"));

        var lista = productoRepo.findByCategoriaId(cat.getId());
        assertThat(lista).extracting(Producto::getReferencia)
                .contains(p1.getReferencia(), p2.getReferencia());

        var dup = new Producto();
        dup.setNombre("RefDup");
        dup.setReferencia(p1.getReferencia()); // duplicada
        dup.setPrecio(new BigDecimal("1.00"));
        dup.setStock(1);
        dup.setCategoria(cat);
        assertThatThrownBy(() -> productoRepo.saveAndFlush(dup))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // 3) Reseñas: crear/listar, validación rango, media por producto
    @Test
    void resenas_crear_listar_validarYMedia() {
        var u1 = f.newUsuarioPersisted();
        var u2 = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Frenos");
        var prod = f.newProductoPersisted(c, "PF", new BigDecimal("19.95"));

        // válida
        var r1 = new Resena();
        r1.setUsuario(u1);
        r1.setProducto(prod);
        r1.setPuntuacion(5);
        r1.setComentario("Muy buena");
        try { r1.setCreadaEn(LocalDateTime.now()); } catch (Throwable ignored) {}
        resenaRepo.saveAndFlush(r1);

        // inválida (6) -> esperamos ConstraintViolationException
        var rInv = new Resena();
        rInv.setUsuario(u2);
        rInv.setProducto(prod);
        rInv.setPuntuacion(6);
        rInv.setComentario("exceso");

        assertThatThrownBy(() -> resenaRepo.saveAndFlush(rInv))
                .isInstanceOf(ConstraintViolationException.class);

        // 🔧 Importante: limpiar el contexto tras una excepción de validación
        em.clear();

        // otra válida para calcular media
        var r2 = new Resena();
        r2.setUsuario(u2);
        r2.setProducto(prod);
        r2.setPuntuacion(3);
        r2.setComentario("ok");
        resenaRepo.saveAndFlush(r2);

        var list = resenaRepo.findByProductoId(prod.getId());
        assertThat(list).hasSize(2);

        var media = resenaRepo.avgPuntuacionByProductoId(prod.getId()).orElseThrow();
        assertThat(media).isCloseTo(4.0, Offset.offset(1e-4));
    }

    // 4) Checkout mínimo: pedido + detalle por cascada + join fetch + orphanRemoval
    @Test
    void pedidos_checkout_joinFetch_y_orphanRemoval() {
        var u = f.newUsuarioPersisted();
        var c = f.newCategoriaPersisted("Filtros");
        var pr1 = f.newProductoPersisted(c, "FA", new BigDecimal("12.50"));
        var pr2 = f.newProductoPersisted(c, "FB", new BigDecimal("9.90"));

        // crear pedido
        var pedido = new Pedido();
        pedido.setUsuario(u);
        pedido.setFechaPedido(LocalDate.now());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setTotal(new BigDecimal("22.40"));
        pedido = pedidoRepo.save(pedido);

        // añadir líneas (PK compuesta por MapsId) y persistir por cascada
        var l1 = pedido.addLinea(pr1, 1, pr1.getPrecio());
        var l2 = pedido.addLinea(pr2, 1, pr2.getPrecio());
        pedidoRepo.saveAndFlush(pedido);

        // buscar detalle con método derivado
        var d1 = detalleRepo.findByPedido_IdAndProducto_Id(pedido.getId(), pr1.getId());
        assertThat(d1).isPresent();
        assertThat(d1.get().getCantidad()).isEqualTo(1);

        // join fetch (pedido con líneas y productos en una query)
        var cargado = pedidoRepo.findConLineasYProductos(pedido.getId()).orElseThrow();
        assertThat(cargado.getDetalles()).hasSize(2);
        assertThat(cargado.getDetalles()).allSatisfy(d -> {
            assertThat(d.getProducto()).isNotNull();
            assertThat(d.getPrecio()).isNotNull();
        });

        // orphanRemoval: quitar una línea y confirmar borrado en BD
        pedido.removeLinea(l2);
        pedidoRepo.saveAndFlush(pedido);
        assertThat(detalleRepo.findByPedido_IdAndProducto_Id(pedido.getId(), pr2.getId()))
                .isEmpty();
    }
}
