package es.unex.cum.mdai.motoresbits.service;

import es.unex.cum.mdai.motoresbits.data.model.entity.Pedido;
import es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido;

import java.util.List;

// Servicio de pedidos: crear, modificar líneas, cambiar estado y confirmar pago.
public interface PedidoService {

    Pedido crearPedido(Long idUsuario);

    Pedido obtenerPedido(Long idPedido);

    List<Pedido> listarPedidosUsuario(Long idUsuario);

    Pedido agregarLinea(Long idPedido, Long idProducto, int cantidad);

    Pedido cambiarCantidadLinea(Long idPedido, Long idProducto, int nuevaCantidad);

    Pedido eliminarLinea(Long idPedido, Long idProducto);

    Pedido cambiarEstado(Long idPedido, EstadoPedido nuevoEstado);

    void eliminarPedido(Long idPedido);

    Pedido confirmarPedido(Long idPedido);

    List<Pedido> listarTodosPedidos();
}
