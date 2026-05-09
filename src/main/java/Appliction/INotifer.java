package Appliction;

import Domain.OwnerManagerTree.Manager;

public interface INotifer {
    boolean notifyUser(String userId, String message);
    boolean saveMessage(String userId,String message);
    boolean sendReserveMessage(String userId);
}
