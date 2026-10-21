package servicio;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import datos.BaseDatosMemoria;
import modelo.ErpException;
import modelo.EstadoVenta;
import modelo.LineaVenta;
import modelo.Producto;
import modelo.TipoMovimiento;
import modelo.Usuario;
import modelo.Venta;

/**
 * Paquete 4: Ventas. Cabeceras y lineas, y los cambios de estado.
 *
 * BORRADOR: se pueden anadir y quitar lineas.
 * CONFIRMADA: sale el stock (movimiento SALIDA por cada linea).
 * ENTREGADA: la mercancia ha llegado al cliente.
 * ANULADA: si estaba confirmada, el stock vuelve a entrar.
 */
public class VentaServicio {

	private final BaseDatosMemoria bd;
	private final InventarioServicio inventario;

	public VentaServicio(BaseDatosMemoria bd, InventarioServicio inventario) {
		this.bd = bd;
		this.inventario = inventario;
	}

	public List<Venta> listar() {
		return bd.ventas.todos();
	}

	public List<Venta> listarPorEstado(EstadoVenta estado) {
		return bd.ventas.filtrar(v -> v.getEstado() == estado);
	}

	public List<Venta> ventasDeCliente(int clienteId) {
		return bd.ventas.filtrar(v -> v.getClienteId() == clienteId);
	}

	public Venta obtener(int id) {
		return bd.ventas.obtener(id);
	}

	public List<LineaVenta> lineas(int ventaId) {
		return bd.lineasVenta.filtrar(l -> l.getVentaId() == ventaId);
	}

	// ------------------------------------------------------------------
	// Borrador
	// ------------------------------------------------------------------
	public Venta crear(int clienteId, Usuario usuario) {
		bd.clientes.obtener(clienteId);
		return bd.ventas.insertar(new Venta(clienteId, usuario.getId()));
	}

	/** Anade una linea al precio de venta actual del producto. */
	public LineaVenta anadirLinea(int ventaId, int productoId, int cantidad) {
		Venta venta = obtenerEnBorrador(ventaId);
		Producto producto = inventario.obtenerProducto(productoId);
		LineaVenta linea = new LineaVenta(ventaId, productoId, cantidad, producto.getPrecioVenta());
		bd.lineasVenta.insertar(linea);
		recalcular(venta);
		return linea;
	}

	public void quitarLinea(int ventaId, int lineaId) {
		Venta venta = obtenerEnBorrador(ventaId);
		LineaVenta linea = bd.lineasVenta.obtener(lineaId);
		if (linea.getVentaId() != ventaId) {
			throw new ErpException("La linea " + lineaId + " no es de la venta " + ventaId);
		}
		bd.lineasVenta.eliminar(lineaId);
		recalcular(venta);
	}

	/** Borra un borrador y sus lineas (ON DELETE CASCADE). */
	public void eliminarBorrador(int ventaId) {
		obtenerEnBorrador(ventaId);
		bd.lineasVenta.eliminarSi(l -> l.getVentaId() == ventaId);
		bd.ventas.eliminar(ventaId);
	}

	private void recalcular(Venta venta) {
		BigDecimal base = BigDecimal.ZERO;
		for (LineaVenta l : lineas(venta.getId())) {
			base = base.add(l.getSubtotal());
		}
		venta.setBaseImponible(base);
		venta.setTotal(Importes.total(base));
	}

	private Venta obtenerEnBorrador(int ventaId) {
		Venta venta = obtener(ventaId);
		if (venta.getEstado() != EstadoVenta.BORRADOR) {
			throw new ErpException("La venta " + ventaId + " ya no esta en borrador (" + venta.getEstado() + ")");
		}
		return venta;
	}

	// ------------------------------------------------------------------
	// Cambios de estado
	// ------------------------------------------------------------------
	/** Comprueba el stock de todas las lineas y despues lo descuenta. */
	public void confirmar(int ventaId) {
		Venta venta = obtenerEnBorrador(ventaId);
		List<LineaVenta> lineas = lineas(ventaId);
		if (lineas.isEmpty()) {
			throw new ErpException("La venta no tiene lineas");
		}
		// Primero se comprueba todo, para no dejar la venta a medias
		for (Map.Entry<Integer, Integer> e : cantidadPorProducto(lineas).entrySet()) {
			Producto p = inventario.obtenerProducto(e.getKey());
			if (p.getStockActual() < e.getValue()) {
				throw new ErpException("Stock insuficiente de " + p + ": hay " + p.getStockActual() + ", se piden "
						+ e.getValue());
			}
		}
		for (LineaVenta l : lineas) {
			inventario.registrarMovimiento(l.getProductoId(), TipoMovimiento.SALIDA, l.getCantidad(), ventaId, null);
		}
		venta.setEstado(EstadoVenta.CONFIRMADA);
	}

	public void entregar(int ventaId) {
		Venta venta = obtener(ventaId);
		if (venta.getEstado() != EstadoVenta.CONFIRMADA) {
			throw new ErpException("Solo se entregan ventas confirmadas (esta: " + venta.getEstado() + ")");
		}
		venta.setEstado(EstadoVenta.ENTREGADA);
	}

	/** Anula la venta; si ya habia salido el stock, lo devuelve al almacen. */
	public void anular(int ventaId) {
		Venta venta = obtener(ventaId);
		if (venta.getEstado() == EstadoVenta.ANULADA) {
			throw new ErpException("La venta ya esta anulada");
		}
		if (venta.getEstado() == EstadoVenta.ENTREGADA) {
			throw new ErpException("Una venta entregada no se puede anular");
		}
		if (bd.documentos.existe(d -> d.getVentaId() != null && d.getVentaId() == ventaId && !d.isAnulado())) {
			throw new ErpException("La venta tiene albaranes o facturas sin anular");
		}
		if (venta.getEstado() == EstadoVenta.CONFIRMADA) {
			for (LineaVenta l : lineas(ventaId)) {
				inventario.registrarMovimiento(l.getProductoId(), TipoMovimiento.ENTRADA, l.getCantidad(), ventaId,
						null);
			}
		}
		venta.setEstado(EstadoVenta.ANULADA);
	}

	private Map<Integer, Integer> cantidadPorProducto(List<LineaVenta> lineas) {
		Map<Integer, Integer> total = new LinkedHashMap<>();
		for (LineaVenta l : lineas) {
			total.merge(l.getProductoId(), l.getCantidad(), Integer::sum);
		}
		return total;
	}
}