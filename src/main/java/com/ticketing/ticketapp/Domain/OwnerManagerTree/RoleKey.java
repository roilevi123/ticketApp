package com.ticketing.ticketapp.Domain.OwnerManagerTree;

import java.io.Serializable;
import java.util.Objects;

public class RoleKey implements Serializable {
    private String userID;
    private String companyName;

    public RoleKey() {}

    public RoleKey(String userID, String companyName) {
        this.userID = userID;
        this.companyName = companyName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoleKey)) return false;
        RoleKey that = (RoleKey) o;
        return Objects.equals(userID, that.userID) && Objects.equals(companyName, that.companyName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userID, companyName);
    }
}
