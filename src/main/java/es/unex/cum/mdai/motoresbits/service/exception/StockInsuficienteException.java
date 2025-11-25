package es.unex.cum.mdai.motoresbits.service.exception;

public class StockInsuficienteException extends RuntimeException {
    public StockInsuficienteException(Long idProducto, int solicitado, int disponible) {
        super("Stock insuficiente para producto " + idProducto + ": solicitado=" + solicitado + ", disponible=" + disponible);
    }
}

