package edu.udg.tfg.FileManagement.feignClients.fileShare;

import java.util.List;
import java.util.UUID;

public class UsersSharedResponse {
    private List<UUID> users;

    public UsersSharedResponse() {
    }

    public List<UUID> getUsers() {
        return users;
    }

    public void setUsers(List<UUID> users) {
        this.users = users;
    }
}
