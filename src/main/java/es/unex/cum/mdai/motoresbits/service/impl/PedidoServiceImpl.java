package es.unex.cum.mdai.motoresbits.service.impl;

import es.unex.cum.mdai.motoresbits.data.model.entity.DetallePedido;
import es.unex.cum.mdai.motoresbits.data.model.entity.Pedido;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.model.entity.Usuario;
import es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido;
import es.unex.cum.mdai.motoresbits.data.repository.DetallePedidoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.PedidoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ProductoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.UsuarioRepository;
import es.unex.cum.mdai.motoresbits.service.PedidoService;
import es.unex.cum.mdai.motoresbits.service.exception.*;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final DetallePedidoRepository detallePedidoRepository;

    public PedidoServiceImpl(PedidoRepository pedidoRepository,
                             UsuarioRepository usuarioRepository,
                             ProductoRepository productoRepository,
                             DetallePedidoRepository detallePedidoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
    }

    // -------------------- CREAR / OBTENER / LISTAR --------------------

    @Override
    public Pedido crearPedido(Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException(idUsuario));

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setFechaPedido(LocalDate.now());
        pedido.setEstado(EstadoPedido.CREADO);
        pedido.setTotal(BigDecimal.ZERO);

        return pedidoRepository.save(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public Pedido obtenerPedido(Long idPedido) {
        Pedido pedido = pedidoRepository.findConLineasYProductos(idPedido)
                .orElseThrow(() -> new PedidoNoEncontradoException(idPedido));

        if (pedido.getUsuario() != null) {
            pedido.getUsuario().getNombre();
        }

        return pedido;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> listarPedidosUsuario(Long idUsuario) {
        return pedidoRepository.findByUsuarioIdAndEstadoNot(idUsuario, EstadoPedido.CREADO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> listarTodosPedidos() {
        return pedidoRepository.findAllConUsuarioYLineasYProductos();
    }

    // -------------------- LÍNEAS DE PEDIDO --------------------

    @Override
    public Pedido agregarLinea(Long idPedido, Long idProducto, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }

        Pedido pedido = obtenerPedidoParaModificacion(idPedido);
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new ProductoNoEncontradoException(idProducto));

        DetallePedido detalleExistente = buscarDetalleEnPedido(pedido, idProducto);

        if (detalleExistente != null) {
            detalleExistente.setCantidad(detalleExistente.getCantidad() + cantidad);
        } else {
            BigDecimal precioUnitario = producto.getPrecio();
            pedido.addLinea(producto, cantidad, precioUnitario);
        }

        recalcularTotal(pedido);
        return pedidoRepository.save(pedido);
    }

    @Override
    public Pedido cambiarCantidadLinea(Long idPedido, Long idProducto, int nuevaCantidad) {
        if (nuevaCantidad < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa");
        }

        if (nuevaCantidad == 0) {
            return eliminarLinea(idPedido, idProducto);
        }

        Pedido pedido = obtenerPedidoParaModificacion(idPedido);

        DetallePedido detalle = buscarDetalleEnPedido(pedido, idProducto);
        if (detalle == null) {
            throw new LineaPedidoNoEncontradaException(idPedido, idProducto);
        }

        detalle.setCantidad(nuevaCantidad);
        recalcularTotal(pedido);

        return pedidoRepository.save(pedido);
    }

    @Override
    public Pedido eliminarLinea(Long idPedido, Long idProducto) {
        Pedido pedidoActual = obtenerPedidoParaModificacion(idPedido);
        DetallePedido detalle = buscarDetalleEnPedido(pedidoActual, idProducto);
        if (detalle == null) {
            throw new LineaPedidoNoEncontradaException(idPedido, idProducto);
        }

        pedidoActual.removeLinea(detalle);
        recalcularTotal(pedidoActual);

        return pedidoRepository.save(pedidoActual);
    }

    // -------------------- ESTADO Y ELIMINACIÓN --------------------

    @Override
    public Pedido cambiarEstado(Long idPedido, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new PedidoNoEncontradoException(idPedido));

        EstadoPedido actual = pedido.getEstado();
        validarTransicionEstado(actual, nuevoEstado);

        if (nuevoEstado == EstadoPedido.CANCELADO && actual == EstadoPedido.PENDIENTE) {
            if (pedido.getDetalles() != null) {
                pedido.getDetalles().forEach(d ->
                        productoRepository.incrementarStock(d.getProducto().getId(), d.getCantidad())
                );
            }
        }

        pedido.setEstado(nuevoEstado);
        return pedidoRepository.save(pedido);
    }

    @Override
    public void eliminarPedido(Long idPedido) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new PedidoNoEncontradoException(idPedido));

        pedidoRepository.delete(pedido);
    }

    @Override
    public Pedido confirmarPedido(Long idPedido) {
        final int MAX_RETRIES = 3;
        int attempt = 0;

        while (true) {
            attempt++;

            Pedido pedido = pedidoRepository.findConLineasYProductos(idPedido)
                    .orElseThrow(() -> new PedidoNoEncontradoException(idPedido));

            // ✅ Asegurar que el total está bien calculado (por si acaso)
            recalcularTotal(pedido);
            BigDecimal total = pedido.getTotal() != null ? pedido.getTotal() : BigDecimal.ZERO;

            // ✅ Cargar usuario y comprobar saldo
            Long usuarioId = (pedido.getUsuario() != null) ? pedido.getUsuario().getId() : null;
            if (usuarioId == null) {
                throw new RuntimeException("El pedido no tiene usuario asociado");
            }

            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new UsuarioNoEncontradoException(usuarioId));

            BigDecimal saldo = usuario.getSaldo() != null ? usuario.getSaldo() : BigDecimal.ZERO;

            if (saldo.compareTo(total) < 0) {
                throw new SaldoInsuficienteException(saldo, total);
            }

            // ✅ Verificar stock para cada línea (antes de descontar)
            if (pedido.getDetalles() != null) {
                for (DetallePedido d : pedido.getDetalles()) {
                    Producto prod = productoRepository.findById(d.getProducto().getId())
                            .orElseThrow(() -> new ProductoNoEncontradoException(d.getProducto().getId()));
                    int disponible = prod.getStock();
                    if (disponible < d.getCantidad()) {
                        throw new StockInsuficienteException(prod.getId(), d.getCantidad(), disponible);
                    }
                }

                try {
                    // ✅ Descontar stock de forma atómica en BD
                    for (DetallePedido d : pedido.getDetalles()) {
                        int updated = productoRepository.descontarStock(d.getProducto().getId(), d.getCantidad());
                        if (updated == 0) {
                            throw new StockInsuficienteException(
                                    d.getProducto().getId(),
                                    d.getCantidad(),
                                    productoRepository.findById(d.getProducto().getId())
                                            .map(Producto::getStock)
                                            .orElse(0)
                            );
                        }
                    }

                    // ✅ Si stock ok -> descontar saldo y marcar pedido PENDIENTE
                    usuario.setSaldo(saldo.subtract(total));
                    usuarioRepository.save(usuario);

                    pedido.setEstado(EstadoPedido.PENDIENTE);
                    pedido.setTotal(total);
                    return pedidoRepository.save(pedido);

                } catch (OptimisticLockingFailureException ex) {
                    if (attempt >= MAX_RETRIES) {
                        // revalidar stock y lanzar si procede
                        for (DetallePedido d : pedido.getDetalles()) {
                            Producto prod = productoRepository.findById(d.getProducto().getId()).orElseThrow();
                            int disponible = prod.getStock();
                            if (disponible < d.getCantidad()) {
                                throw new StockInsuficienteException(prod.getId(), d.getCantidad(), disponible);
                            }
                        }
                        throw ex;
                    }
                    try { TimeUnit.MILLISECONDS.sleep(50L * attempt); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); }

                } catch (StockInsuficienteException sie) {
                    if (attempt >= MAX_RETRIES) throw sie;
                    try { TimeUnit.MILLISECONDS.sleep(50L * attempt); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            } else {
                // no hay detalles; marcamos PENDIENTE igualmente (y no descontamos saldo)
                pedido.setEstado(EstadoPedido.PENDIENTE);
                pedido.setTotal(total);
                return pedidoRepository.save(pedido);
            }
        }
    }

    // -------------------- MÉTODOS PRIVADOS AUXILIARES --------------------

    private Pedido obtenerPedidoParaModificacion(Long idPedido) {
        return pedidoRepository.findConLineasYProductos(idPedido)
                .orElseThrow(() -> new PedidoNoEncontradoException(idPedido));
    }

    private DetallePedido buscarDetalleEnPedido(Pedido pedido, Long idProducto) {
        if (pedido.getDetalles() == null) return null;

        return pedido.getDetalles()
                .stream()
                .filter(d -> d.getProducto() != null && idProducto.equals(d.getProducto().getId()))
                .findFirst()
                .orElse(null);
    }

    private void recalcularTotal(Pedido pedido) {
        if (pedido.getDetalles() == null || pedido.getDetalles().isEmpty()) {
            pedido.setTotal(BigDecimal.ZERO);
            return;
        }

        BigDecimal total = pedido.getDetalles().stream()
                .map(d -> d.getPrecio().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        pedido.setTotal(total);
    }

    private void validarTransicionEstado(EstadoPedido actual, EstadoPedido nuevo) {
        if (actual == nuevo) return;

        switch (actual) {
            case CREADO -> {
                if (nuevo != EstadoPedido.PENDIENTE && nuevo != EstadoPedido.CANCELADO) {
                    throw new EstadoPedidoInvalidoException(actual, nuevo);
                }
            }
            case PENDIENTE -> {
                if (nuevo != EstadoPedido.PAGADO && nuevo != EstadoPedido.CANCELADO) {
                    throw new EstadoPedidoInvalidoException(actual, nuevo);
                }
            }
            case PAGADO -> {
                if (nuevo != EstadoPedido.ENVIADO && nuevo != EstadoPedido.CANCELADO) {
                    throw new EstadoPedidoInvalidoException(actual, nuevo);
                }
            }
            case ENVIADO -> {
                if (nuevo != EstadoPedido.ENTREGADO) {
                    throw new EstadoPedidoInvalidoException(actual, nuevo);
                }
            }
            case ENTREGADO, CANCELADO ->
                    throw new EstadoPedidoInvalidoException(actual, nuevo);
        }
    }
}
