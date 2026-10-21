package modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cabecera de venta (tabla Venta). Sus lineas estan en LineaVenta (1:N).
 *
 * Ciclo de vida: BORRADOR -> CONFIRMADA (sale el stock) -> ENTREGADA.
 * Se puede ANULAR mientras no este entregada.
 */
public class Venta implements Entidad {

	private int id; // PK AUTO_INCREMENT
	private int clienteId; // FK -> Cliente.id
	private int usuarioId; // FK -> Usuario.id
	private LocalDateTime fecha; // DATETIME
	private EstadoVenta estado = EstadoVenta.BORRADOR; // ENUM
	private BigDecimal baseImponible = BigDecimal.ZERO; // DECIMAL(12,2)
	private BigDecimal total = BigDecimal.ZERO; // DECIMAL(12,2)

	public Venta() {
	}

	public Venta(int clienteId, int usuarioId) {
		setClienteId(clienteId);
		setUsuarioId(usuarioId);
		this.fecha = LocalDateTime.now();
	}

	// ------------------------------------------------------------------
	// Getters y setters
	// ------------------------------------------------------------------
	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

	public int getClienteId() {
		return clienteId;
	}

	public void setClienteId(int clienteId) {
		this.clienteId = Validar.referencia(clienteId, "cliente");
	}

	public int getUsuarioId() {
		return usuarioId;
	}

	public void setUsuarioId(int usuarioId) {
		this.usuarioId = Validar.referencia(usuarioId, "usuario");
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}

	public EstadoVenta getEstado() {
		return estado;
	}

	public void setEstado(EstadoVenta estado) {
		this.estado = estado;
	}

	public BigDecimal getBaseImponible() {
		return baseImponible;
	}

	public void setBaseImponible(BigDecimal baseImponible) {
		this.baseImponible = Validar.importe(baseImponible, "base imponible");
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = Validar.importe(total, "total");
	}

	@Override
	public String toString() {
		return "Venta " + id;
	}
}
