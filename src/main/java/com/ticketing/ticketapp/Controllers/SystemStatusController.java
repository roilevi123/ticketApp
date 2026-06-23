package com.ticketing.ticketapp.Controllers;

import com.ticketing.ticketapp.Infastructure.ExternalSystemMonitor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = {
        "http://localhost:3000",
        "http://localhost:5173"
})
@RestController
@RequestMapping("/api/system")
public class SystemStatusController {

    private final ExternalSystemMonitor monitor;

    public SystemStatusController(ExternalSystemMonitor monitor) {
        this.monitor = monitor;
    }

    @GetMapping("/status")
    public boolean status() {
        return monitor.isAvailable();
    }
}