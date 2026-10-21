package modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cabecera de compra (tabla Compra). Sus lineas estan en LineaCompra (1:N).
 *
 * La compra no tiene proveedor: se conoce a traves de Producto.proveedor_id
 * de sus lineas.
 *
 * Ciclo de vida: BORRADOR -> CONFIRMADA (pedido hecho) -> RECIBIDA (entra el
 * stock). Se puede ANULAR mientras no este recibida.
 */
public class Compra implements Entidad {

	private int id; // PK AUTO_INCREMENT
	private int usuarioId; // FK -> Usuario.id
	private LocalDateTime fecha; // DATETIME
	private EstadoCompra estado = EstadoCompra.BORRADOR; // ENUM
	private BigDecimal baseImponible = BigDecimal.ZERO; // DECIMAL(12,2)
	private BigDecimal total = BigDecimal.ZERO; // DECIMAL(12,2)

	public Compra() {
	}

	public Compra(int usuarioId) {
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

	public EstadoCompra getEstado() {
		return estado;
	}

	public void setEstado(EstadoCompra estado) {
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
		return "Compra " + id;
	}
}
