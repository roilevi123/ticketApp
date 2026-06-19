package config;

import com.ticketing.ticketapp.TicketappApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
@ActiveProfiles("test")
@SpringBootTest(classes = TicketappApplication.class)

@TestPropertySource(properties = {
        "repository.type=DB",
        "spring.datasource.url=jdbc:postgresql://136.115.146.17:5432/ticketapp_test_db",
        "spring.datasource.username=ticketapp_user",
        "spring.datasource.password=BGUticketapp1!",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=update"
})
class DatabaseDiagnosticsTest {

    @Autowired
    private Environment environment;

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private DataSourceProperties dataSourceProperties;

    @Test
    void printSpringDatabaseProperties() {
        System.out.println("========== SPRING DB PROPERTIES ==========");
        System.out.println("active profiles = " + String.join(",", environment.getActiveProfiles()));
        System.out.println("spring.datasource.url = " + environment.getProperty("spring.datasource.url"));
        System.out.println("spring.datasource.driver-class-name = " + environment.getProperty("spring.datasource.driver-class-name"));
        System.out.println("spring.datasource.username = " + environment.getProperty("spring.datasource.username"));
        System.out.println("spring.jpa.database-platform = " + environment.getProperty("spring.jpa.database-platform"));
        System.out.println("spring.jpa.hibernate.ddl-auto = " + environment.getProperty("spring.jpa.hibernate.ddl-auto"));
        System.out.println("repository.type = " + environment.getProperty("repository.type"));

        assertNotNull(environment.getProperty("spring.datasource.url"));
    }

    @Test
    void dataSourceBeanExists() {
        assertNotNull(dataSource, "DataSource bean was not created");
        System.out.println("DataSource class = " + dataSource.getClass().getName());

        if (dataSourceProperties != null) {
            System.out.println("DataSourceProperties url = " + dataSourceProperties.getUrl());
            System.out.println("DataSourceProperties driver = " + dataSourceProperties.getDriverClassName());
            System.out.println("DataSourceProperties username = " + dataSourceProperties.getUsername());
        }
    }

    @Test
    void canOpenDatabaseConnection() throws Exception {
        assertNotNull(dataSource, "DataSource bean was not created");

        try (Connection connection = dataSource.getConnection()) {
            System.out.println("========== JDBC CONNECTION ==========");
            System.out.println("URL = " + connection.getMetaData().getURL());
            System.out.println("USER = " + connection.getMetaData().getUserName());
            System.out.println("DB PRODUCT = " + connection.getMetaData().getDatabaseProductName());
            System.out.println("DB VERSION = " + connection.getMetaData().getDatabaseProductVersion());

            assertFalse(connection.isClosed());
        }
    }

    @Test
    void canRunSimpleSelect() throws Exception {
        assertNotNull(dataSource, "DataSource bean was not created");

        try (
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT 1")
        ) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
            System.out.println("SELECT 1 works");
        }
    }

    @Test
    void printAllPublicTables() throws Exception {
        assertNotNull(dataSource, "DataSource bean was not created");

        try (
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("""
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                        ORDER BY table_name
                        """)
        ) {
            System.out.println("========== PUBLIC TABLES ==========");

            int count = 0;
            while (rs.next()) {
                count++;
                System.out.println(rs.getString("table_name"));
            }

            System.out.println("table count = " + count);
        }
    }

    @Test
    void activeOrdersTableExists() throws Exception {
        assertNotNull(dataSource, "DataSource bean was not created");

        try (
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("""
                        SELECT table_name
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'active_orders'
                        """)
        ) {
            boolean exists = rs.next();
            System.out.println("active_orders exists = " + exists);
            assertTrue(exists, "active_orders table does not exist");
        }
    }
}