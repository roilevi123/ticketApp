package com.ticketing.ticketapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class TicketappApplication {

    public static void main(String[] args) {
        try {
            SpringApplication app = new SpringApplication(TicketappApplication.class);
            app.setLogStartupInfo(false);
            app.setRegisterShutdownHook(true);
            app.run(args);
        } catch (Exception e) {
            System.err.println("***************************");
            System.err.println("TICKETAPP FAILED TO START");
            System.err.println("***************************");
//            System.err.println("Reason: " + rootMessage(e));
            System.exit(1);
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable current = e;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }
}