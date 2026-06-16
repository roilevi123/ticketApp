package Infastructure;

import com.ticketing.ticketapp.Config.DatabaseHealthMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.AbstractDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simulates a database drop-off and reconnection without requiring a real DB.
 * Uses a controllable fake DataSource backed by H2 to avoid JDK-module mocking
 * restrictions, exercising the detect → outage → recovery lifecycle.
 */
public class DatabaseConnectionRecoveryTest {

    /**
     * Fake DataSource that can be switched between healthy and failing modes.
     * Backed by H2 in-memory so real Connection objects are returned when healthy.
     */
    private static class ControllableDataSource extends AbstractDataSource {
        private volatile boolean failing = false;

        void setFailing(boolean fail) {
            failing = fail;
        }

        @Override
        public Connection getConnection() throws SQLException {
            if (failing) {
                throw new SQLException("Simulated connection refused");
            }
            return DriverManager.getConnection("jdbc:h2:mem:healthtest;DB_CLOSE_DELAY=-1", "sa", "");
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getConnection();
        }
    }

    private ControllableDataSource dataSource;
    private DatabaseHealthMonitor monitor;

    @BeforeEach
    void setUp() {
        dataSource = new ControllableDataSource();
        monitor = new DatabaseHealthMonitor(dataSource);
    }

    @Test
    void detects_healthy_connection() {
        dataSource.setFailing(false);

        monitor.checkHealth();

        assertTrue(monitor.isDbAvailable(), "Monitor should report DB as available when connection succeeds");
    }

    @Test
    void detects_database_outage() {
        dataSource.setFailing(true);

        monitor.checkHealth();

        assertFalse(monitor.isDbAvailable(), "Monitor should report DB as unavailable when connection fails");
    }

    @Test
    void recovers_after_outage() {
        // Phase 1: simulate outage
        dataSource.setFailing(true);
        monitor.checkHealth();
        assertFalse(monitor.isDbAvailable());

        // Phase 2: DB comes back
        dataSource.setFailing(false);
        monitor.checkHealth();
        assertTrue(monitor.isDbAvailable(), "Monitor should report DB as available once connection is restored");
    }

    @Test
    void repeated_outage_does_not_change_state() {
        dataSource.setFailing(true);
        monitor.checkHealth();
        assertFalse(monitor.isDbAvailable());

        monitor.checkHealth();
        assertFalse(monitor.isDbAvailable(), "Repeated failures should keep state as unavailable");
    }

    @Test
    void repeated_healthy_checks_keep_state_available() {
        dataSource.setFailing(false);

        monitor.checkHealth();
        assertTrue(monitor.isDbAvailable());

        monitor.checkHealth();
        assertTrue(monitor.isDbAvailable(), "Repeated successes should keep state as available");
    }
}
