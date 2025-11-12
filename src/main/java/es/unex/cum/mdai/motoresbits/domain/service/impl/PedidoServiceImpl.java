package es.unex.cum.mdai.motoresbits.domain.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.unex.cum.mdai.motoresbits.data.model.entity.DetallePedido;
import es.unex.cum.mdai.motoresbits.data.model.entity.DetallePedidoId;
import es.unex.cum.mdai.motoresbits.data.model.entity.Pedido;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.model.entity.Usuario;
import es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido;
import es.unex.cum.mdai.motoresbits.data.repository.DetallePedidoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.PedidoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ProductoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.UsuarioRepository;
import es.unex.cum.mdai.motoresbits.domain.exception.NotFoundException;
import es.unex.cum.mdai.motoresbits.domain.service.PedidoService;

@Service
@Transactional
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepo;
    private final UsuarioRepository usuarioRepo;
    private final ProductoRepository productoRepo;
    private final DetallePedidoRepository detalleRepo;

    public PedidoServiceImpl(PedidoRepository pedidoRepo,
                             UsuarioRepository usuarioRepo,
                             ProductoRepository productoRepo,
                             DetallePedidoRepository detalleRepo) {
        this.pedidoRepo = pedidoRepo;
        this.usuarioRepo = usuarioRepo;
        this.productoRepo = productoRepo;
        this.detalleRepo = detalleRepo;
    }

    @Override
    public Pedido crearPedido(Long usuarioId) {
        Usuario u = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        Pedido p = new Pedido();
        p.setUsuario(u);
        p.setFechaPedido(LocalDate.now());
        p.setEstado(EstadoPedido.CREADO);
        p.setTotal(BigDecimal.ZERO);
        return pedidoRepo.save(p);
    }

    @Override
    public DetallePedido agregarProducto(Long pedidoId, Long productoId, int cantidad) {
        Pedido pedido = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado"));
        Producto producto = productoRepo.findById(productoId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));

        // Si ya hay línea, acumula cantidad; si no, crea nueva
        DetallePedido detalle = detalleRepo
                .findByPedido_IdAndProducto_Id(pedidoId, productoId) // existe en tu repo. :contentReference[oaicite:4]{index=4}
                .orElseGet(() -> pedido.addLinea(producto, 0, producto.getPrecio()));

        detalle.setCantidad((detalle.getCantidad() == null ? 0 : detalle.getCantidad()) + cantidad);
        detalle.setPrecio(producto.getPrecio()); // precio actual
        // attach ya lo hace addLinea; si venía de repo, aseguramos consistencia:
        detalle.setPedido(pedido);
        detalle.setProducto(producto);

        detalleRepo.save(detalle);
        recalcularTotal(pedidoId);
        return detalle;
    }

    @Override
    public void eliminarProducto(Long pedidoId, Long productoId) {
        DetallePedidoId id = new DetallePedidoId();
        id.setPedidoId(pedidoId);
        id.setProductoId(productoId);
        detalleRepo.findById(id).ifPresent(d -> {
            Pedido p = d.getPedido();
            p.removeLinea(d);
            detalleRepo.delete(d);
            recalcularTotal(pedidoId);
        });
    }

    @Override
    public BigDecimal recalcularTotal(Long pedidoId) {
        // Cargamos pedido con líneas + productos en un único fetch
        Pedido p = pedidoRepo.findConLineasYProductos(pedidoId)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado")); // método tuyo. :contentReference[oaicite:5]{index=5}

        BigDecimal total = p.getDetalles().stream()
                .map(d -> d.getPrecio().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        p.setTotal(total);
        return total;
    }

    @Override
    @Transactional(readOnly = true)
    public Pedido buscarPorId(Long id) {
        return pedidoRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Pedido no encontrado"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> listarPorUsuario(Long usuarioId) {
        return pedidoRepo.findByUsuarioId(usuarioId); // ya existe en tu repo. :contentReference[oaicite:6]{index=6}
    }
}
