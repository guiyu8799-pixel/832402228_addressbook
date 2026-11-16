package com.fzu.addressbook.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Creates the directory that holds the SQLite file before the data source is opened.
 *
 * <p>SQLite creates the database file on demand but does not create missing parent directories, and
 * the JDBC driver fails with "path to ... does not exist" when they are absent. Running this as an
 * {@link EnvironmentPostProcessor} guarantees the directory exists both for {@code java -jar} and
 * for the test context, without every caller having to remember it.</p>
 */
public class DatabaseDirectoryPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String configuredPath = environment.getProperty("app.database.path", "./data/addressbook.db");
        Path database = Paths.get(configuredPath).toAbsolutePath().normalize();
        Path parent = database.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException exception) {
            throw new IllegalStateException("无法创建数据库目录：" + parent, exception);
        }
    }

    @Override
    public int getOrder() {
        // Run after the application.yml files have been loaded into the environment.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
