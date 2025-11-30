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
        // usamos la query que trae líneas y productos
        return pedidoRepository.findConLineasYProductos(idPedido)
                .orElseThrow(() -> new PedidoNoEncontradoException(idPedido));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> listarPedidosUsuario(Long idUsuario) {
        return pedidoRepository.findByUsuarioIdAndEstadoNot(idUsuario, EstadoPedido.CREADO);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<Pedido> listarTodosPedidos() {
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

        // ¿Ya existe una línea para este producto?
        DetallePedido detalleExistente = buscarDetalleEnPedido(pedido, idProducto);

        if (detalleExistente != null) {
            // sumamos cantidades
            detalleExistente.setCantidad(detalleExistente.getCantidad() + cantidad);
        } else {
            // precio de la línea = precio actual del producto
            BigDecimal precioUnitario = producto.getPrecio();

            // usamos el método de dominio del Pedido
            pedido.addLinea(producto, cantidad, precioUnitario);
        }

        recalcularTotal(pedido);

        // save y devolver con líneas actualizadas
        return pedidoRepository.save(pedido);
    }

    @Override
    public Pedido cambiarCantidadLinea(Long idPedido, Long idProducto, int nuevaCantidad) {
        if (nuevaCantidad < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa");
        }

        if (nuevaCantidad == 0) {
            // Reutilizamos la lógica de eliminar línea
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

        // 1) Cargamos el pedido con sus líneas y comprobamos si la línea existe
        Pedido pedidoActual = obtenerPedidoParaModificacion(idPedido);
        DetallePedido detalle = buscarDetalleEnPedido(pedidoActual, idProducto);
        if (detalle == null) {
            throw new LineaPedidoNoEncontradaException(idPedido, idProducto);
        }

        // 2) Quitamos la línea de la colección (orphanRemoval=true en Pedido eliminará la fila en BD)
        pedidoActual.removeLinea(detalle);

        // 3) Recalculamos el total
        recalcularTotal(pedidoActual);

        // 4) Guardamos y devolvemos
        return pedidoRepository.save(pedidoActual);
    }

    // -------------------- ESTADO Y ELIMINACIÓN --------------------

    @Override
    public Pedido cambiarEstado(Long idPedido, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new PedidoNoEncontradoException(idPedido));

        EstadoPedido actual = pedido.getEstado();

        validarTransicionEstado(actual, nuevoEstado);

        // si se cancela desde PENDIENTE, reponemos stock (asumimos que si estaba PENDIENTE
        // el stock ya fue descontado en confirmarPedido)
        if (nuevoEstado == EstadoPedido.CANCELADO && actual == EstadoPedido.PENDIENTE) {
            // devolver stock
            if (pedido.getDetalles() != null) {
                pedido.getDetalles().forEach(d -> productoRepository.incrementarStock(d.getProducto().getId(), d.getCantidad()));
            }
        }

        pedido.setEstado(nuevoEstado);
        return pedidoRepository.save(pedido);
    }

    @Override
    public void eliminarPedido(Long idPedido) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new PedidoNoEncontradoException(idPedido));

        // Regla simple: permitimos borrar siempre,
        // ya que el modelo borra cascada las líneas.
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

            // verificamos stock para cada línea
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
                    // intentamos descontar stock de forma atómica en BD
                    for (DetallePedido d : pedido.getDetalles()) {
                        int updated = productoRepository.descontarStock(d.getProducto().getId(), d.getCantidad());
                        if (updated == 0) {
                            // no se pudo descontar (otro hilo se adelantó), re-evaluar stock
                            throw new StockInsuficienteException(d.getProducto().getId(), d.getCantidad(),
                                    productoRepository.findById(d.getProducto().getId()).map(Producto::getStock).orElse(0));
                        }
                    }

                    // si hemos llegado aquí, todas las líneas se descontaron correctamente
                    pedido.setEstado(EstadoPedido.PENDIENTE);
                    return pedidoRepository.save(pedido);
                } catch (OptimisticLockingFailureException ex) {
                    // mantenemos la lógica por compatibilidad con otros casos, intentar reintento
                    if (attempt >= MAX_RETRIES) {
                        for (DetallePedido d : pedido.getDetalles()) {
                            Producto prod = productoRepository.findById(d.getProducto().getId()).orElseThrow();
                            int disponible = prod.getStock();
                            if (disponible < d.getCantidad()) {
                                throw new StockInsuficienteException(prod.getId(), d.getCantidad(), disponible);
                            }
                        }
                        throw ex;
                    }
                    try { TimeUnit.MILLISECONDS.sleep(50L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } catch (StockInsuficienteException sie) {
                    // Si detectamos que otro hilo consumió el stock mientras intentábamos descontar,
                    // reintentamos la operación completa (hasta MAX_RETRIES) para dar una oportunidad
                    // a que otros pedidos fallen y este detecte la falta de stock.
                    if (attempt >= MAX_RETRIES) throw sie;
                    try { TimeUnit.MILLISECONDS.sleep(50L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    // limpiar y reintentar
                }
            } else {
                // no hay detalles; marcamos PENDIENTE igualmente
                pedido.setEstado(EstadoPedido.PENDIENTE);
                return pedidoRepository.save(pedido);
            }
        }
    }

    // -------------------- MÉTODOS PRIVADOS AUXILIARES --------------------

    /**
     * Obtiene el pedido con sus líneas y productos cargados para modificación.
     */
    private Pedido obtenerPedidoParaModificacion(Long idPedido) {
        return pedidoRepository.findConLineasYProductos(idPedido)
                .orElseThrow(() -> new PedidoNoEncontradoException(idPedido));
    }

    /**
     * Busca un detalle dentro del pedido para un producto dado.
     */
    private DetallePedido buscarDetalleEnPedido(Pedido pedido, Long idProducto) {
        if (pedido.getDetalles() == null) {
            return null;
        }
        return pedido.getDetalles()
                .stream()
                .filter(d -> d.getProducto() != null && idProducto.equals(d.getProducto().getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Recalcula el total del pedido como sumatorio de cantidad * precio por línea.
     */
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

    /**
     * Regla de negocio de flujo de estados.
     */
    private void validarTransicionEstado(EstadoPedido actual, EstadoPedido nuevo) {
        if (actual == nuevo) {
            return;
        }

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
            case ENTREGADO, CANCELADO -> // no se permite cambiar desde ENTREGADO o CANCELADO
                    throw new EstadoPedidoInvalidoException(actual, nuevo);
        }
    }
}
