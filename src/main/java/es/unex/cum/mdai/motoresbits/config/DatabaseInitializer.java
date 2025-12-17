package es.unex.cum.mdai.motoresbits.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    /*
     * Ruta del script SQL que inicializa la BD.
     * Por defecto: "scripts/schema.sql" relativa al directorio desde el que arrancas la app.
     * Puedes sobreescribirla en application.properties con:
     *   app.init.schema-path=/otra/ruta/mi_script.sql
     */
    @Value("${app.init.schema-path:scripts/schema.sql}")
    private String schemaPath;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void run(String... args) throws Exception {

        // Comprobación más robusta: mirar específicamente la existencia y contenido de la tabla 'usuarios'
        boolean debeInicializar = false;
        try {
            Integer existeUsuarios = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND LOWER(table_name) = 'usuarios'",
                    Integer.class
            );

            if (existeUsuarios == null || existeUsuarios == 0) {
                // No existe la tabla 'usuarios' → BD vacía o no inicializada
                debeInicializar = true;
            } else {
                // La tabla existe; comprobar si tiene filas
                try {
                    Integer cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class);
                    if (cnt == null || cnt == 0) {
                        // existe pero vacía → inicializar
                        debeInicializar = true;
                    } else {
                        // existe y contiene filas → no inicializar
                        debeInicializar = false;
                    }
                } catch (DataAccessException exCount) {
                    // No se pudo contar filas (posible problema con mayúsculas/identificadores) → intentar con comillas
                    try {
                        Integer cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM \"USUARIOS\"", Integer.class);
                        if (cnt == null || cnt == 0) {
                            debeInicializar = true;
                        } else {
                            debeInicializar = false;
                        }
                    } catch (DataAccessException exCount2) {
                        // Si no podemos decidir, inicializar por seguridad
                        debeInicializar = true;
                    }
                }
            }
        } catch (DataAccessException ex) {
            // Si falla la consulta sobre information_schema, inicializamos por seguridad
            debeInicializar = true;
        }

        if (!debeInicializar) {
            System.out.println("[DB INIT] La tabla 'usuarios' existe y contiene datos. No se ejecuta schema.sql.");
            return;
        }

        // Localizamos el fichero scripts/schema.sql
        Path path = Paths.get(schemaPath).toAbsolutePath();
        if (!Files.exists(path)) {
            System.err.println("[DB INIT] No se encuentra el script SQL en: " + path);
            return;
        }

        System.out.println("[DB INIT] No se han encontrado tablas. Ejecutando script de inicialización: " + path);

        // Ejecutamos el script contra el DataSource actual (MariaDB o H2)
        ResourceDatabasePopulator populator =
                new ResourceDatabasePopulator(new FileSystemResource(path.toFile()));

        populator.execute(dataSource);

        System.out.println("[DB INIT] Script schema.sql ejecutado correctamente.");
    }
}
