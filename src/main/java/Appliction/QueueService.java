package Appliction;

import Domain.Domains.QueueDomain;
import Domain.QueueAggregates.QueueEntry;

import java.util.List;
import java.util.Optional;

public class QueueService {
    private QueueDomain domain;
    public QueueService(QueueDomain domain) {
        this.domain = domain;
    }
    public String checkStatus(String eventId, String username) {
        return domain.checkStatus(eventId, username);
    }
}
