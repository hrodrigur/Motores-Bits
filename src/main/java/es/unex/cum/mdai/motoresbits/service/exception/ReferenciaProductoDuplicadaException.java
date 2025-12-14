package es.unex.cum.mdai.motoresbits.service.exception;

public class ReferenciaProductoDuplicadaException extends RuntimeException {
    public ReferenciaProductoDuplicadaException(String referencia) {
        super("Ya existe un producto con la referencia '" + referencia + "'");
    }
}

