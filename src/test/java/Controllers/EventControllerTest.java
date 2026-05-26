package Controllers;

import com.ticketing.ticketapp.Appliction.CompanyService;
import com.ticketing.ticketapp.Appliction.EventService;
import com.ticketing.ticketapp.Appliction.Response;
import com.ticketing.ticketapp.Controllers.EventController;
import com.ticketing.ticketapp.Domain.Company.CompanyDTO;
import com.ticketing.ticketapp.Domain.Event.EventDTO;
import com.ticketing.ticketapp.Domain.Event.MapArea;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    @Mock
    private CompanyService companyService;

    @InjectMocks
    private EventController eventController;

    private static final String TOKEN = "test-token";

    @Test
    void searchEvents_Success_Returns200() {
        EventDTO eventDTO = mock(EventDTO.class);
        List<EventDTO> results = List.of(eventDTO);
        when(eventService.searchEvents(TOKEN, "rock", null, null, null, null, null, null, null, null))
                .thenReturn(Response.success(results));

        ResponseEntity<?> response = eventController.searchEvents(
                TOKEN, "rock", null, null, null, null, null, null, null, null);

        assertEquals(200, response.getStatusCode().value());
        assertSame(results, response.getBody());
    }

    @Test
    void searchEvents_Failure_Returns400() {
        when(eventService.searchEvents(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Response.error("Search failed"));

        ResponseEntity<?> response = eventController.searchEvents(
                TOKEN, null, null, null, null, null, null, null, null, null);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Search failed", response.getBody());
    }

    @Test
    void getEventDetails_Success_Returns200() {
        EventDTO eventDTO = mock(EventDTO.class);
        when(eventService.getEvent(TOKEN, "CompanyA", "EventX"))
                .thenReturn(Response.success(eventDTO));

        ResponseEntity<?> response = eventController.getEventDetails(TOKEN, "CompanyA", "EventX");

        assertEquals(200, response.getStatusCode().value());
        assertSame(eventDTO, response.getBody());
    }

    @Test
    void getEventDetails_NotFound_Returns404() {
        when(eventService.getEvent(TOKEN, "CompanyA", "Unknown"))
                .thenReturn(Response.error("Event not found"));

        ResponseEntity<?> response = eventController.getEventDetails(TOKEN, "CompanyA", "Unknown");

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Event not found", response.getBody());
    }

    @Test
    void getEventMap_Success_Returns200() {
        MapArea[][] map = {{MapArea.SEAT, MapArea.STAND}};
        when(eventService.getMapArea(TOKEN, "CompanyA", "EventX"))
                .thenReturn(Response.success(map));

        ResponseEntity<?> response = eventController.getEventMap(TOKEN, "CompanyA", "EventX");

        assertEquals(200, response.getStatusCode().value());
        assertSame(map, response.getBody());
    }

    @Test
    void getEventMap_NotFound_Returns404() {
        when(eventService.getMapArea(TOKEN, "CompanyA", "Unknown"))
                .thenReturn(Response.error("Map not found"));

        ResponseEntity<?> response = eventController.getEventMap(TOKEN, "CompanyA", "Unknown");

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void getCompanyEvents_Success_Returns200() {
        EventDTO eventDTO = mock(EventDTO.class);
        List<EventDTO> events = List.of(eventDTO);
        when(eventService.getCompanyEvents(TOKEN, "CompanyA"))
                .thenReturn(Response.success(events));

        ResponseEntity<?> response = eventController.getCompanyEvents(TOKEN, "CompanyA");

        assertEquals(200, response.getStatusCode().value());
        assertSame(events, response.getBody());
    }

    @Test
    void getCompanyEvents_Failure_Returns400() {
        when(eventService.getCompanyEvents(TOKEN, "CompanyA"))
                .thenReturn(Response.error("Company not found"));

        ResponseEntity<?> response = eventController.getCompanyEvents(TOKEN, "CompanyA");

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void getActiveCompanies_Success_Returns200() {
        CompanyDTO companyDTO = new CompanyDTO("CompanyA", "founder1", 4.5);
        List<CompanyDTO> companies = List.of(companyDTO);
        when(companyService.getActiveCompanies(TOKEN))
                .thenReturn(Response.success(companies));

        ResponseEntity<?> response = eventController.getActiveCompanies(TOKEN);

        assertEquals(200, response.getStatusCode().value());
        assertSame(companies, response.getBody());
    }

    @Test
    void getActiveCompanies_Failure_Returns400() {
        when(companyService.getActiveCompanies(TOKEN))
                .thenReturn(Response.error("No active companies"));

        ResponseEntity<?> response = eventController.getActiveCompanies(TOKEN);

        assertEquals(400, response.getStatusCode().value());
    }
}
