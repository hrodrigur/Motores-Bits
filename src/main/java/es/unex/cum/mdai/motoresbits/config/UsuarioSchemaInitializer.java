package es.unex.cum.mdai.motoresbits.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

@Component
public class UsuarioSchemaInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioSchemaInitializer.class);

    private final DataSource dataSource;

    @Value("${app.init.schema:sql/schema.sql}")
    private String schemaPath;

    public UsuarioSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            // buscar tabla 'usuarios' (insensitive a mayúsculas según DB)
            try (ResultSet rs = meta.getTables(null, null, "%USUARIOS%", new String[]{"TABLE"})) {
                if (rs.next()) {
                    logger.info("Tabla 'usuarios' ya existe. No se ejecuta el script de inicialización.");
                    return;
                }
            }

            // alternativa: comprobar con nombre exacto en varias mayúsculas/minúsculas
            try (ResultSet rs2 = meta.getTables(null, null, "usuarios", new String[]{"TABLE"})) {
                if (rs2.next()) {
                    logger.info("Tabla 'usuarios' ya existe. No se ejecuta el script de inicialización.");
                    return;
                }
            }

            logger.info("Tabla 'usuarios' no encontrada. Ejecutando script: {}", schemaPath);
            ResourceDatabasePopulator pop = new ResourceDatabasePopulator();
            pop.addScript(new ClassPathResource(schemaPath));
            DatabasePopulatorUtils.execute(pop, dataSource);
            logger.info("Script de inicialización ejecutado correctamente.");

        } catch (Exception e) {
            logger.error("Error al comprobar/ejecutar esquema inicial: ", e);
        }
    }
}

