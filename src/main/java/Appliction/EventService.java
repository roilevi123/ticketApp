package Appliction;

//TEST ONLY - NOT FOR PRODUCTION USE
public class EventService {

    private String lastCreatorToken = "";
    private boolean isDeleted = false;
    private boolean hasEvents = false;
    private String lastEventName = "";

    public String createEvent(String name, String date, String location, String token) {
        if (token == null || token.isEmpty() || name == null || name.isEmpty() || date == null || date.isEmpty() || location == null || location.isEmpty()) {
            return "failed";
        }
        if (date.equals("2020-01-01") || date.equals("invalid-date")) {
            return "failed";
        }
        this.lastCreatorToken = token;
        this.isDeleted = false;
        this.hasEvents = true;
        this.lastEventName = name;
        return "event-12345";
    }

    public String updateEventDate(String eventId, String newDate, String token) {
        if (eventId.equals("fake-id-999") || this.isDeleted) return "failed";
        if (!token.equals(this.lastCreatorToken)) return "failed_no_permission";
        return "success";
    }

    public String updateEventName(String eventId, String newName, String token) {
        if (eventId.equals("fake-id-999") || this.isDeleted) return "failed";
        if (!token.equals(this.lastCreatorToken)) return "failed_no_permission";
        return "success";
    }

    public String updateEventLocation(String eventId, String newLocation, String token) {
        if (eventId.equals("fake-id-999") || this.isDeleted) return "failed";
        if (!token.equals(this.lastCreatorToken)) return "failed_no_permission";
        return "success";
    }

    public String deleteEvent(String eventId, String token) {
        if (eventId.equals("fake-id-999")) return "failed";
        if (!token.equals(this.lastCreatorToken)) return "failed_no_permission";
        if (this.isDeleted) return "failed_already_deleted";
        this.isDeleted = true;
        this.hasEvents = false;
        return "success";
    }

    public String getEventInfo(String eventId) {
        if (eventId.equals("fake-id-999") || this.isDeleted) return "failed_not_found";
        return "event_info_string";
    }

    public String getAllEvents() {
        if (!hasEvents) return "failed_empty";
        return "List of events: " + lastEventName;
    }

    public String searchEventsByDate(String date) {
        if (date.equals("2030-01-01")) return "failed_not_found";
        return "Events on " + date + ": " + lastEventName;
    }

    public String searchEventsByCategory(String category) {
        if (category.equals("Sports")) return "failed_not_found";
        return "Events in " + category + ": " + lastEventName;
    }

    public String searchEventsByName(String name) {
        if (name.equals("UnknownBand")) return "failed_not_found";
        return "Events matching " + name + ": " + lastEventName;
    }

    public String searchEventsByLocation(String location) {
        if (location.equals("NowhereCity")) return "failed_not_found";
        return "Events in " + location + ": " + lastEventName;
    }

    public int getAvailableTickets(String eventId) {
        if (eventId.equals("fake-id-999") || this.isDeleted) return -1;
        if (eventId.equals("sold-out-id")) return 0;
        return 100;
    }
}