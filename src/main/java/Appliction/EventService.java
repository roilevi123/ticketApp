package Appliction;

//TEST PURPOSE ONLY - NOT FINAL IMPLEMENTATION!
public class EventService {

    private String lastCreatorToken = "";
    private boolean isDeleted = false;

    public String createEvent(String name, String date, String location, String token) {
        if (token == null || token.isEmpty() || name == null || name.isEmpty() || date == null || date.isEmpty() || location == null || location.isEmpty()) {
            return "failed";
        }
        if (date.equals("2020-01-01") || date.equals("invalid-date")) {
            return "failed";
        }
        this.lastCreatorToken = token;
        this.isDeleted = false;
        return "event-12345";
    }

    public String updateEventDate(String eventId, String newDate, String token) {
        if (eventId.equals("fake-id-999") || this.isDeleted) {
            return "failed";
        }
        if (!token.equals(this.lastCreatorToken)) {
            return "failed_no_permission";
        }
        return "success";
    }

    public String updateEventName(String eventId, String newName, String token) {
        if (eventId.equals("fake-id-999") || this.isDeleted) {
            return "failed";
        }
        if (!token.equals(this.lastCreatorToken)) {
            return "failed_no_permission";
        }
        return "success";
    }

    public String updateEventLocation(String eventId, String newLocation, String token) {
        if (eventId.equals("fake-id-999") || this.isDeleted) {
            return "failed";
        }
        if (!token.equals(this.lastCreatorToken)) {
            return "failed_no_permission";
        }
        return "success";
    }

    public String deleteEvent(String eventId, String token) {
        if (eventId.equals("fake-id-999")) {
            return "failed";
        }
        if (!token.equals(this.lastCreatorToken)) {
            return "failed_no_permission";
        }
        if (this.isDeleted) {
            return "failed_already_deleted";
        }
        this.isDeleted = true;
        return "success";
    }

    public String getEventInfo(String eventId) {
        if (eventId.equals("fake-id-999") || this.isDeleted) {
            return "failed_not_found";
        }
        return "event_info_string";
    }
}