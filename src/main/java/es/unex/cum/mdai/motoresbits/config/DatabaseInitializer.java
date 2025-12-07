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

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    /**
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

        boolean debeInicializar = false;

        try {
            // Comprobamos cuántas tablas hay en el esquema actual.
            // Funciona en MariaDB/MySQL y en H2 en modo MariaDB.
            Integer totalTables = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) " +
                            "FROM information_schema.tables " +
                            "WHERE table_schema = DATABASE()",
                    Integer.class
            );

            if (totalTables == null || totalTables == 0) {
                // No hay ninguna tabla → BD "vacía" → hay que lanzar schema.sql
                debeInicializar = true;
            }
        } catch (DataAccessException ex) {
            // Si falla la consulta (por ejemplo, schema aún no existe bien) → inicializamos por seguridad
            debeInicializar = true;
        }

        if (!debeInicializar) {
            System.out.println("[DB INIT] Ya existen tablas en la base de datos. No se ejecuta schema.sql.");
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
