package es.unex.cum.mdai.motoresbits.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import es.unex.cum.mdai.motoresbits.data.model.entity.*;
import es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido;
import es.unex.cum.mdai.motoresbits.data.model.enums.RolUsuario;
import es.unex.cum.mdai.motoresbits.data.repository.*;

@Component
public class TestDataFactory {

    private static final AtomicInteger SEQ = new AtomicInteger(1);

    @Autowired UsuarioRepository usuarioRepo;
    @Autowired CategoriaRepository categoriaRepo;
    @Autowired ProductoRepository productoRepo;
    @Autowired PedidoRepository pedidoRepo;
    @Autowired DetallePedidoRepository detalleRepo;

    public Usuario newUsuarioPersisted() {
        int n = SEQ.getAndIncrement();
        Usuario u = new Usuario();
        u.setNombre("User " + n);
        u.setEmail("user"+n+"@test.com");
        u.setContrasena("x");
        u.setRol(RolUsuario.CLIENTE);
        return usuarioRepo.save(u);
    }

    public Categoria newCategoriaPersisted(String nombreBase) {
        int n = SEQ.getAndIncrement();
        Categoria c = new Categoria();
        c.setNombre(nombreBase + " " + n);
        c.setDescripcion("Desc " + n);
        return categoriaRepo.save(c);
    }

    public Producto newProductoPersisted(Categoria cat, String refBase, BigDecimal precio) {
        int n = SEQ.getAndIncrement();
        Producto p = new Producto();
        p.setNombre("Prod " + n);
        p.setReferencia(refBase + "-" + n);
        p.setPrecio(precio);
        p.setStock(100);
        p.setCategoria(cat);
        return productoRepo.save(p);
    }

    public Pedido newPedidoPersisted(Usuario u, BigDecimal total, EstadoPedido estado) {
        Pedido p = new Pedido();
        p.setUsuario(u);
        p.setFechaPedido(LocalDate.now());
        p.setEstado(estado);
        p.setTotal(total);
        return pedidoRepo.save(p);
    }

    /** Crea una línea en el pedido y la persiste (útil si no usas cascade PERSIST en Pedido.detalles) */
    public DetallePedido addLineaPersisted(Pedido pedido, Producto producto, int c, BigDecimal precio) {
        var linea = pedido.addLinea(producto, c, precio);
        return linea;
    }
}
