package es.unex.cum.mdai.motoresbits.service.exception;

public class ProductoNoEncontradoException extends RuntimeException {

    public ProductoNoEncontradoException(Long id) {
        super("No existe el producto con id=" + id);
    }

    public ProductoNoEncontradoException(String msg) {
        super(msg);
    }
}
