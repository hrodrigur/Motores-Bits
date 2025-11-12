package es.unex.cum.mdai.motoresbits.domain.service;

import java.math.BigDecimal;
import java.util.List;

import es.unex.cum.mdai.motoresbits.data.model.entity.DetallePedido;
import es.unex.cum.mdai.motoresbits.data.model.entity.Pedido;

public interface PedidoService {
    Pedido crearPedido(Long usuarioId);
    DetallePedido agregarProducto(Long pedidoId, Long productoId, int cantidad);
    void eliminarProducto(Long pedidoId, Long productoId);
    BigDecimal recalcularTotal(Long pedidoId);
    Pedido buscarPorId(Long id);
    List<Pedido> listarPorUsuario(Long usuarioId);
}
