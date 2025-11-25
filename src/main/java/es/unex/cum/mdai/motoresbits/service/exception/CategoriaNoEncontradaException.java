package es.unex.cum.mdai.motoresbits.service.exception;

public class CategoriaNoEncontradaException extends RuntimeException {
    public CategoriaNoEncontradaException(Long id) {
        super("No existe la categoría con id=" + id);
    }
}
