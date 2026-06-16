package com.ticketing.ticketapp.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DatabaseHealthMonitor {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthMonitor.class);

    private final DataSource dataSource;
    private volatile boolean dbAvailable = true;

    public DatabaseHealthMonitor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Scheduled(fixedDelay = 30_000)
    public void checkHealth() {
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute("SELECT 1");
            if (!dbAvailable) {
                logger.info("DatabaseHealthMonitor: database connection restored.");
                dbAvailable = true;
            }
        } catch (SQLException e) {
            if (dbAvailable) {
                logger.error("DatabaseHealthMonitor: database connection lost: {}", e.getMessage());
                dbAvailable = false;
            }
        }
    }

    public boolean isDbAvailable() {
        return dbAvailable;
    }
}
