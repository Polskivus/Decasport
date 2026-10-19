package objetos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Venta {

    public enum Estado { BORRADOR, CONFIRMADA, ENTREGADA, ANULADA }

    private int id;
    private int clienteId;
    private int usuarioId;
    private LocalDateTime fecha;
    private Estado estado;
    private BigDecimal baseImponible;
    private BigDecimal total;

    public Venta(int id, int clienteId, int usuarioId, LocalDateTime fecha, Estado estado, BigDecimal baseImponible, BigDecimal total) {
        this.id = id;
        this.clienteId = clienteId;
        this.usuarioId = usuarioId;
        this.fecha = fecha;
        this.estado = estado;
        this.baseImponible = baseImponible;
        this.total = total;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getClienteId() { return clienteId; }
    public void setClienteId(int clienteId) { this.clienteId = clienteId; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public BigDecimal getBaseImponible() { return baseImponible; }
    public void setBaseImponible(BigDecimal baseImponible) { this.baseImponible = baseImponible; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
}
