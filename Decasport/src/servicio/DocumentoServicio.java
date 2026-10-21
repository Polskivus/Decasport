package servicio;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

import datos.BaseDatosMemoria;
import modelo.Compra;
import modelo.DocumentoFiscal;
import modelo.ErpException;
import modelo.EstadoCompra;
import modelo.EstadoDocumento;
import modelo.EstadoVenta;
import modelo.TipoDocumento;
import modelo.Venta;

/**
 * Paquete 5: Facturacion y albaranes.
 *
 * Una venta (confirmada o entregada) o una compra (recibida) puede tener un
 * albaran y una factura vigentes. La factura enlaza con el albaran si existe,
 * asi se sigue la cadena Factura -> Albaran -> Venta/Compra -> lineas.
 */
public class DocumentoServicio {

	private final BaseDatosMemoria bd;

	public DocumentoServicio(BaseDatosMemoria bd) {
		this.bd = bd;
	}

	public List<DocumentoFiscal> listar() {
		return bd.documentos.todos();
	}

	public DocumentoFiscal obtener(int id) {
		return bd.documentos.obtener(id);
	}

	public List<DocumentoFiscal> documentosDeVenta(int ventaId) {
		return bd.documentos.filtrar(d -> d.getVentaId() != null && d.getVentaId() == ventaId);
	}

	public List<DocumentoFiscal> documentosDeCompra(int compraId) {
		return bd.documentos.filtrar(d -> d.getCompraId() != null && d.getCompraId() == compraId);
	}

	public List<DocumentoFiscal> facturasPendientesDeCobro() {
		return bd.documentos.filtrar(d -> d.getTipo() == TipoDocumento.FACTURA && d.getEstado() == EstadoDocumento.EMITIDO);
	}

	// ------------------------------------------------------------------
	// Emision
	// ------------------------------------------------------------------
	public DocumentoFiscal emitirParaVenta(int ventaId, TipoDocumento tipo) {
		Venta venta = bd.ventas.obtener(ventaId);
		if (venta.getEstado() != EstadoVenta.CONFIRMADA && venta.getEstado() != EstadoVenta.ENTREGADA) {
			throw new ErpException("Solo se emiten documentos de ventas confirmadas o entregadas");
		}
		List<DocumentoFiscal> existentes = documentosDeVenta(ventaId);
		return emitir(tipo, existentes, ventaId, null, venta.getBaseImponible(), venta.getTotal());
	}

	public DocumentoFiscal emitirParaCompra(int compraId, TipoDocumento tipo) {
		Compra compra = bd.compras.obtener(compraId);
		if (compra.getEstado() != EstadoCompra.RECIBIDA) {
			throw new ErpException("Solo se emiten documentos de compras recibidas");
		}
		List<DocumentoFiscal> existentes = documentosDeCompra(compraId);
		return emitir(tipo, existentes, null, compraId, compra.getBaseImponible(), compra.getTotal());
	}

	private DocumentoFiscal emitir(TipoDocumento tipo, List<DocumentoFiscal> existentes, Integer ventaId,
			Integer compraId, BigDecimal base, BigDecimal total) {
		for (DocumentoFiscal d : existentes) {
			if (d.getTipo() == tipo && !d.isAnulado()) {
				throw new ErpException("Ya existe " + d + " para este documento de origen");
			}
		}
		Integer albaranId = null;
		if (tipo == TipoDocumento.FACTURA) {
			albaranId = albaranLibre(existentes);
		}
		DocumentoFiscal doc = new DocumentoFiscal(tipo, siguienteNumero(tipo), ventaId, compraId, albaranId, base,
				total);
		return bd.documentos.insertar(doc);
	}

	// Albaran vigente que todavia no esta en ninguna factura (cada albaran solo en una)
	private Integer albaranLibre(List<DocumentoFiscal> existentes) {
		for (DocumentoFiscal d : existentes) {
			if (d.getTipo() == TipoDocumento.ALBARAN && !d.isAnulado()) {
				int id = d.getId();
				boolean usado = bd.documentos.existe(f -> f.getAlbaranId() != null && f.getAlbaranId() == id);
				if (!usado) {
					return id;
				}
			}
		}
		return null;
	}

	// numero UQ: ALB-2026-0001, FAC-2026-0001...
	private String siguienteNumero(TipoDocumento tipo) {
		String prefijo = (tipo == TipoDocumento.ALBARAN ? "ALB-" : "FAC-") + Year.now().getValue() + "-";
		int n = 0;
		for (DocumentoFiscal d : bd.documentos.todos()) {
			if (d.getNumero().startsWith(prefijo)) {
				n = Math.max(n, Integer.parseInt(d.getNumero().substring(prefijo.length())));
			}
		}
		return prefijo + String.format("%04d", n + 1);
	}

	// ------------------------------------------------------------------
	// Cambios de estado
	// ------------------------------------------------------------------
	public void marcarPagada(int documentoId) {
		DocumentoFiscal doc = obtener(documentoId);
		if (doc.getTipo() != TipoDocumento.FACTURA) {
			throw new ErpException("Solo las facturas se marcan como pagadas");
		}
		if (doc.getEstado() != EstadoDocumento.EMITIDO) {
			throw new ErpException("La factura no esta pendiente (" + doc.getEstado() + ")");
		}
		doc.setEstado(EstadoDocumento.PAGADO);
	}

	public void anular(int documentoId) {
		DocumentoFiscal doc = obtener(documentoId);
		if (doc.getEstado() != EstadoDocumento.EMITIDO) {
			throw new ErpException("Solo se anulan documentos emitidos (este: " + doc.getEstado() + ")");
		}
		if (doc.getTipo() == TipoDocumento.ALBARAN && bd.documentos
				.existe(f -> f.getAlbaranId() != null && f.getAlbaranId() == documentoId && !f.isAnulado())) {
			throw new ErpException("El albaran esta en una factura vigente; anula antes la factura");
		}
		doc.setEstado(EstadoDocumento.ANULADO);
	}
}