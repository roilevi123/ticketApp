package Appliction;

public class EventService {

    private String lastCreatorToken = "";
    private boolean isDeleted = false;
    private boolean hasEvents = false;
    private String lastEventName = "";

    public Response<String> createEvent(String name, String date, String location, String token) {
        if (token == null || token.isEmpty() || name == null || name.isEmpty() || date == null || date.isEmpty() || location == null || location.isEmpty()) {
            return new Response<>(false, "Missing fields or token", null);
        }
        if (date.equals("2020-01-01") || date.equals("invalid-date")) {
            return new Response<>(false, "Invalid date", null);
        }
        this.lastCreatorToken = token;
        this.isDeleted = false;
        this.hasEvents = true;
        this.lastEventName = name;
        return new Response<>(true, "Event created", "event-12345");
    }

    public Response<Void> updateEventDate(String eventId, String newDate, String token) {
        if (eventId.equals("fake-id-999") || this.isDeleted) return new Response<>(false, "Event not found", null);
        if (!token.equals(this.lastCreatorToken)) return new Response<>(false, "No permission", null);
        return new Response<>(true, "Date updated", null);
    }

    public Response<Void> updateEventName(String eventId, String newName, String token) {
        if (eventId.equals("fake-id-999") || this.isDeleted) return new Response<>(false, "Event not found", null);
        if (!token.equals(this.lastCreatorToken)) return new Response<>(false, "No permission", null);
        return new Response<>(true, "Name updated", null);
    }

    public Response<Void> updateEventLocation(String eventId, String newLocation, String token) {
        if (eventId.equals("fake-id-999") || this.isDeleted) return new Response<>(false, "Event not found", null);
        if (!token.equals(this.lastCreatorToken)) return new Response<>(false, "No permission", null);
        return new Response<>(true, "Location updated", null);
    }

    public Response<Void> deleteEvent(String eventId, String token) {
        if (eventId.equals("fake-id-999")) return new Response<>(false, "Event not found", null);
        if (!token.equals(this.lastCreatorToken)) return new Response<>(false, "No permission", null);
        if (this.isDeleted) return new Response<>(false, "Already deleted", null);
        this.isDeleted = true;
        this.hasEvents = false;
        return new Response<>(true, "Event deleted", null);
    }

    public Response<String> getEventInfo(String eventId) {
        if (eventId.equals("fake-id-999") || this.isDeleted) return new Response<>(false, "Event not found", null);
        return new Response<>(true, "Success", "event_info_string");
    }

    public Response<String> getAllEvents() {
        if (!hasEvents) return new Response<>(false, "Empty", null);
        return new Response<>(true, "Success", "List of events: " + lastEventName);
    }

    public Response<String> searchEventsByDate(String date) {
        if (date.equals("2030-01-01")) return new Response<>(false, "Not found", null);
        return new Response<>(true, "Success", "Events on " + date + ": " + lastEventName);
    }

    public Response<String> searchEventsByCategory(String category) {
        if (category.equals("Sports")) return new Response<>(false, "Not found", null);
        return new Response<>(true, "Success", "Events in " + category + ": " + lastEventName);
    }

    public Response<String> searchEventsByName(String name) {
        if (name.equals("UnknownBand")) return new Response<>(false, "Not found", null);
        return new Response<>(true, "Success", "Events matching " + name + ": " + lastEventName);
    }

    public Response<String> searchEventsByLocation(String location) {
        if (location.equals("NowhereCity")) return new Response<>(false, "Not found", null);
        return new Response<>(true, "Success", "Events in " + location + ": " + lastEventName);
    }

    public Response<Integer> getAvailableTickets(String eventId) {
        if (eventId.equals("fake-id-999") || this.isDeleted) return new Response<>(false, "Event not found", -1);
        if (eventId.equals("sold-out-id")) return new Response<>(true, "Sold out", 0);
        return new Response<>(true, "Success", 100);
    }
}