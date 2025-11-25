package es.unex.cum.mdai.motoresbits.service.exception;

public class PedidoNoEncontradoException extends RuntimeException {
    public PedidoNoEncontradoException(Long id) {
        super("No se ha encontrado el pedido con id=" + id);
    }
}
