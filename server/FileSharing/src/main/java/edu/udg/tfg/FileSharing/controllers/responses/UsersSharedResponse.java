package edu.udg.tfg.FileSharing.controllers.responses;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UsersSharedResponse {
    private List<UUID> users;

    public UsersSharedResponse() {
        users = new ArrayList<>();
    }

    public List<UUID> getUsers() {
        return users;
    }

    public void setUsers(List<UUID> users) {
        this.users = users;
    }
}
