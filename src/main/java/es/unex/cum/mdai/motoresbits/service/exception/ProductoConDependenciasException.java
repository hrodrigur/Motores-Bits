package es.unex.cum.mdai.motoresbits.service.exception;

public class ProductoConDependenciasException extends RuntimeException {
    public ProductoConDependenciasException(Long idProducto) {
        super("No se puede eliminar el producto " + idProducto + " porque tiene reseñas o pedidos asociados");
    }
}
