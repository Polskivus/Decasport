package erp.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Albaran o factura (tabla DocumentoFiscal).
 *
 * Pertenece exactamente a una venta o a una compra. Una factura puede
 * referenciar el albaran del que sale (albaran_id, autorreferencia) y cada
 * albaran solo puede estar en una factura.
 */
public class DocumentoFiscal implements Entidad {

	private int id; // PK AUTO_INCREMENT
	private TipoDocumento tipo; // ENUM
	private String numero; // VARCHAR(30) UQ
	private LocalDateTime fecha; // DATETIME
	private Integer ventaId; // FK -> Venta.id (opcional)
	private Integer compraId; // FK -> Compra.id (opcional)
	private Integer albaranId; // FK -> DocumentoFiscal.id (opcional)
	private BigDecimal baseImponible = BigDecimal.ZERO; // DECIMAL(12,2)
	private BigDecimal total = BigDecimal.ZERO; // DECIMAL(12,2)
	private EstadoDocumento estado = EstadoDocumento.EMITIDO; // ENUM

	public DocumentoFiscal() {
	}

	public DocumentoFiscal(TipoDocumento tipo, String numero, Integer ventaId, Integer compraId, Integer albaranId,
			BigDecimal baseImponible, BigDecimal total) {
		if (tipo == null) {
			throw new ErpException("El tipo de documento es obligatorio");
		}
		// CHECK de la tabla: exactamente una de venta_id o compra_id
		if ((ventaId == null) == (compraId == null)) {
			throw new ErpException("El documento debe ser de una venta o de una compra (solo una)");
		}
		if (albaranId != null && tipo != TipoDocumento.FACTURA) {
			throw new ErpException("Solo una factura puede referenciar un albaran");
		}
		this.tipo = tipo;
		this.numero = Validar.texto(numero, "numero", 30, true);
		this.ventaId = ventaId;
		this.compraId = compraId;
		this.albaranId = albaranId;
		this.baseImponible = Validar.importe(baseImponible, "base imponible");
		this.total = Validar.importe(total, "total");
		this.fecha = LocalDateTime.now();
	}

	public boolean isAnulado() {
		return estado == EstadoDocumento.ANULADO;
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

	public TipoDocumento getTipo() {
		return tipo;
	}

	public String getNumero() {
		return numero;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public Integer getVentaId() {
		return ventaId;
	}

	public Integer getCompraId() {
		return compraId;
	}

	public Integer getAlbaranId() {
		return albaranId;
	}

	public BigDecimal getBaseImponible() {
		return baseImponible;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public EstadoDocumento getEstado() {
		return estado;
	}

	public void setEstado(EstadoDocumento estado) {
		this.estado = estado;
	}

	@Override
	public String toString() {
		return tipo + " " + numero;
	}
}
