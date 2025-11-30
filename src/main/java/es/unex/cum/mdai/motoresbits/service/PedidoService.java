package es.unex.cum.mdai.motoresbits.service;

import es.unex.cum.mdai.motoresbits.data.model.entity.Pedido;
import es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido;

import java.util.List;

public interface PedidoService {

    /**
     * Crea un nuevo pedido vacío para un usuario dado.
     */
    Pedido crearPedido(Long idUsuario);

    /**
     * Obtiene un pedido con sus líneas y productos (si existe) o lanza excepción.
     */
    Pedido obtenerPedido(Long idPedido);

    /**
     * Lista todos los pedidos de un usuario.
     */
    List<Pedido> listarPedidosUsuario(Long idUsuario);

    /**
     * Añade una línea al pedido. Si ya hay una línea para ese producto,
     * se suma la cantidad.
     */
    Pedido agregarLinea(Long idPedido, Long idProducto, int cantidad);

    /**
     * Cambia la cantidad de una línea. Si la cantidad es 0, elimina la línea.
     */
    Pedido cambiarCantidadLinea(Long idPedido, Long idProducto, int nuevaCantidad);

    /**
     * Elimina una línea concreta del pedido.
     */
    Pedido eliminarLinea(Long idPedido, Long idProducto);

    /**
     * Cambia el estado del pedido respetando un flujo de estados válido.
     */
    Pedido cambiarEstado(Long idPedido, EstadoPedido nuevoEstado);

    /**
     * Elimina un pedido (junto con sus líneas).
     */
    void eliminarPedido(Long idPedido);

    /**
     * Confirma el pedido: verifica stock para cada línea y descuenta la cantidad
     * correspondiente en los productos. Devuelve el pedido actualizado (estado PENDIENTE).
     * Si no hay stock suficiente para algún producto, lanza StockInsuficienteException.
     */
    Pedido confirmarPedido(Long idPedido);

    /**
     * Lista todos los pedidos (para uso de administrador)
     */
    java.util.List<Pedido> listarTodosPedidos();
}
