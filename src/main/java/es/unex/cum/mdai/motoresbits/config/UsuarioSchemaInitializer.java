package es.unex.cum.mdai.motoresbits.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Inicializa/comprueba el esquema de `usuarios` al arrancar la aplicación (no ejecuta scripts automáticamente).
@Component
public class UsuarioSchemaInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioSchemaInitializer.class);

    private final DataSource dataSource;

    // Usar por defecto el script externo en scripts/schema.sql
    @Value("${app.init.schema:file:./scripts/schema.sql}")
    private String schemaPath;

    public UsuarioSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            // comprobar de forma robusta si existe la tabla 'usuarios' (evitar coincidencias parciales)
            boolean existe = tablaExiste(conn, "usuarios");
            if (existe) {
                // Si la tabla existe, comprobar si ya contiene datos. Solo si tiene filas consideraremos la BD poblada.
                Boolean tieneFilas = tablaTieneFilas(conn, "usuarios");
                if (Boolean.TRUE.equals(tieneFilas)) {
                    logger.info("La tabla 'usuarios' ya contiene datos. No se ejecutará el script de inicialización.");
                    // Otra inicialización puede encargarse del llenado; aquí solo comprobamos y salimos.
                    return;
                } else if (Boolean.FALSE.equals(tieneFilas)) {
                    logger.info("La tabla 'usuarios' existe pero está vacía. Se recomienda ejecutar el script de inicialización.");
                    // no ejecutamos el script aquí: el `DatabaseInitializer` es el responsable.
                } else {
                    // valor null = no se pudo comprobar; mantener comportamiento conservador: ejecutar script
                    logger.warn("No se pudo determinar si la tabla 'usuarios' tiene filas; se recomienda ejecutar el script por seguridad.");
                }
            } else {
                logger.info("Tabla 'usuarios' no encontrada en la base de datos. Se recomienda ejecutar el script de inicialización.");
            }

        } catch (Exception e) {
            logger.error("Error al comprobar esquema inicial: ", e);
        }
    }

    // Comprueba si existe una tabla con el nombre dado en la conexión, comparando el nombre ignorando mayúsculas/minúsculas.
    // Evita usar patrones que puedan devolver coincidencias parciales.
    private boolean tablaExiste(Connection conn, String tabla) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            // obtener todas las tablas del catálogo/esquema y comparar TABLE_NAME con igualdad ignorando case
            try (ResultSet rs = meta.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    if (tableName != null && tableName.equalsIgnoreCase(tabla)) {
                        return true;
                    }
                }
            }
        } catch (SQLException ex) {
            logger.warn("No se pudo comprobar la existencia de la tabla '{}' mediante DatabaseMetaData: {}", tabla, ex.getMessage());
        }
        return false;
    }

    // Devuelve TRUE si la tabla tiene al menos una fila, FALSE si existe pero está vacía, NULL si no se pudo determinar.
    private Boolean tablaTieneFilas(Connection conn, String tabla) {
        String sql = "SELECT COUNT(*) FROM " + tabla;
        try (Statement st = conn.createStatement(); java.sql.ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                long cnt = rs.getLong(1);
                return cnt > 0 ? Boolean.TRUE : Boolean.FALSE;
            }
        } catch (SQLException ex) {
            logger.warn("No se pudo ejecutar consulta COUNT sobre la tabla '{}': {}", tabla, ex.getMessage());
            // intentar con mayúsculas/literal entre comillas por si el motor exige mayúsculas
            try (Statement st2 = conn.createStatement(); java.sql.ResultSet rs2 = st2.executeQuery("SELECT COUNT(*) FROM \"" + tabla + "\"") ) {
                if (rs2.next()) {
                    long cnt = rs2.getLong(1);
                    return cnt > 0 ? Boolean.TRUE : Boolean.FALSE;
                }
            } catch (SQLException ex2) {
                logger.warn("Consulta alternativa COUNT falló para la tabla '{}': {}", tabla, ex2.getMessage());
            }
        }
        return null;
    }
}
