package config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationConfigTest {

    private final Environment env = new MockEnvironment()
            .withProperty("repository.type", "MEMORY")
            .withProperty("initial.state.enabled", "false")
            .withProperty("spring.jpa.hibernate.ddl-auto", "create-drop")
            .withProperty("spring.datasource.url", "jdbc:h2:mem:testdb")
            .withProperty("admin.name", "admin")
            .withProperty("admin.passward", "admin");

    @Test
    void repositoryType_ShouldBeMemoryOrDb() {
        String type = env.getProperty("repository.type");

        assertNotNull(type, "repository.type is missing");
        assertTrue(
                type.equals("MEMORY") || type.equals("DB"),
                "repository.type must be MEMORY or DB"
        );
    }

    @Test
    void adminCredentials_ShouldExist() {
        assertNotBlank("admin.name");
        assertNotBlank("admin.passward");
    }

    @Test
    void initialStateEnabled_ShouldBeBoolean() {
        assertBoolean("initial.state.enabled");
    }

    @Test
    void ddlAuto_ShouldBeValid() {
        String ddl = env.getProperty("spring.jpa.hibernate.ddl-auto");

        assertNotNull(ddl, "spring.jpa.hibernate.ddl-auto is missing");

        assertTrue(
                List.of("none", "validate", "update", "create", "create-drop")
                        .contains(ddl),
                "spring.jpa.hibernate.ddl-auto has illegal value: " + ddl
        );
    }

    @Test
    void datasourceUrl_ShouldExist() {
        assertNotBlank("spring.datasource.url");
    }

    private void assertBoolean(String key) {
        String value = env.getProperty(key);

        assertNotNull(value, key + " is missing");
        assertTrue(
                value.equals("true") || value.equals("false"),
                key + " must be true or false, but was: " + value
        );
    }

    private void assertNotBlank(String key) {
        String value = env.getProperty(key);

        assertNotNull(value, key + " is missing");
        assertFalse(value.isBlank(), key + " must not be blank");
    }
}