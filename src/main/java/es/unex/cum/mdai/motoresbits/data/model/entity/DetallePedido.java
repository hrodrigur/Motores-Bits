package es.unex.cum.mdai.motoresbits.data.model.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "DETALLES_PEDIDO")
public class DetallePedido implements Serializable {

    @EmbeddedId
    private DetallePedidoId id = new DetallePedidoId(); // <- inicializada

    @MapsId("pedidoId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @MapsId("productoId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    public DetallePedido() {}

    /** Enlaza pedido y producto y sincroniza la PK compuesta */
    public void attach(Pedido p, Producto pr) {
        this.pedido = p;
        this.producto = pr;
        if (p != null) this.id.setPedidoId(p.getId());
        if (pr != null) this.id.setProductoId(pr.getId());
        // Si quieres mantener la bidireccionalidad desde Pedido:
        if (p != null && p.getDetalles() != null && !p.getDetalles().contains(this)) {
            p.getDetalles().add(this);
        }
    }

    // Getters / Setters

    public DetallePedidoId getId() { return id; }
    public void setId(DetallePedidoId id) { this.id = id; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
        if (pedido != null) this.id.setPedidoId(pedido.getId());
    }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) {
        this.producto = producto;
        if (producto != null) this.id.setProductoId(producto.getId());
    }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
}
