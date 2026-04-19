package Domain.QueueAggregates;

public class QueueEntry {
    private String username;
    private long entryTime;
    private Long grantedAccessTime;
    private boolean isExpired;

    public QueueEntry(String username) {
        this.username = username;
        this.entryTime = System.currentTimeMillis();
        this.grantedAccessTime = null;
        this.isExpired = false;
    }

    // Getters
    public String getUsername() { return username; }
    public Long getGrantedAccessTime() { return grantedAccessTime; }
    public void setGrantedAccessTime(Long time) { this.grantedAccessTime = time; }
}
