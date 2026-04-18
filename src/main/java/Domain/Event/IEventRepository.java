package Domain.Event;

public interface IEventRepository {
    void save(Event event);
    Event findById(String eventId);
    void update(Event event);
    void delete(String eventId);
    void deleteAllEvents();
}
