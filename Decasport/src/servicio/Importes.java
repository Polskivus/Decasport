package servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculo de importes. Se aplica un IVA unico a toda la venta o compra.
 */
public final class Importes {

	public static final BigDecimal IVA = new BigDecimal("0.21");

	private Importes() {
	}

	public static BigDecimal redondear(BigDecimal importe) {
		return importe.setScale(2, RoundingMode.HALF_UP);
	}

	/** Total = base imponible + IVA. */
	public static BigDecimal total(BigDecimal baseImponible) {
		return redondear(baseImponible.add(baseImponible.multiply(IVA)));
	}

	public static String formato(BigDecimal importe) {
		return String.format("%,.2f EUR", importe);
	}
}