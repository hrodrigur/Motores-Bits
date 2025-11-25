package es.unex.cum.mdai.motoresbits.service.exception;

public class CategoriaConProductosException extends RuntimeException {
    public CategoriaConProductosException(Long idCategoria) {
        super("No se puede eliminar la categoría " + idCategoria + " porque tiene productos asociados");
    }
}
