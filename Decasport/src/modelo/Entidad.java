package erp.modelo;

/**
 * Toda fila de una tabla tiene una clave primaria entera (AUTO_INCREMENT).
 */
public interface Entidad {

	int getId();

	void setId(int id);
}
