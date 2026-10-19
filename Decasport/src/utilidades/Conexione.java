package utilidades;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utilidad de conexión JDBC contra la base de datos PostgreSQL del proyecto.
 *
 * Requiere el driver org.postgresql (dependencia "org.postgresql:postgresql" en el pom.xml).
 */
public class Conexione {

    // ------------------------------------------------------------------
    // Parámetros de conexión: cámbialos según tu PostgreSQL local.
    // ------------------------------------------------------------------
    private static final String HOST = "localhost";
    private static final String PUERTO = "5432";
    private static final String BASE_DATOS = "erp";
    private static final String USUARIO = "postgres";
    private static final String PASSWORD = "postgres";

    private static final String URL = "jdbc:postgresql://" + HOST + ":" + PUERTO + "/" + BASE_DATOS;

    // Registra el driver una sola vez, al cargar la clase.
    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("No se encontró el driver de PostgreSQL (org.postgresql:postgresql). "
                    + "Revisa las dependencias del pom.xml.");
        }
    }

    /**
     * Devuelve una conexión nueva. Quien la use debe cerrarla
     * (preferiblemente con try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, PASSWORD);
    }

    /**
     * Cierra varios recursos JDBC ignorando los nulos.
     * Ejemplo: Conexione.cerrar(rs, ps, cn);
     */
    public static void cerrar(AutoCloseable... recursos) {
        for (AutoCloseable recurso : recursos) {
            if (recurso != null) {
                try {
                    recurso.close();
                } catch (Exception e) {
                    System.err.println("Error al cerrar recurso: " + e.getMessage());
                }
            }
        }
    }

    /** Comprueba que la base de datos responde. */
    public static boolean probarConexion() {
        try (Connection cn = getConnection()) {
            return cn != null && !cn.isClosed();
        } catch (SQLException e) {
            System.err.println("Error de conexión: " + e.getMessage());
            return false;
        }
    }

    /** Prueba rápida de conexión. */
    public static void main(String[] args) {
        if (probarConexion()) {
            System.out.println("Conexión correcta con la base de datos '" + BASE_DATOS + "'.");
        } else {
            System.out.println("No se pudo conectar con la base de datos '" + BASE_DATOS + "'.");
        }
    }
}
