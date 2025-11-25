package es.unex.cum.mdai.motoresbits.service.exception;

public class LineaPedidoNoEncontradaException extends RuntimeException {
    public LineaPedidoNoEncontradaException(Long idPedido, Long idProducto) {
        super("No existe línea en el pedido " + idPedido + " para el producto " + idProducto);
    }
}
