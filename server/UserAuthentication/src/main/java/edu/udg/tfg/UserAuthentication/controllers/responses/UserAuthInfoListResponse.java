package edu.udg.tfg.UserAuthentication.controllers.responses;

import java.util.List;

public class UserAuthInfoListResponse {
    private List<UserAuthInfoResponse> users;

    public UserAuthInfoListResponse(List<UserAuthInfoResponse> users) {
        this.users = users;
    }

    public List<UserAuthInfoResponse> getUsers() {
        return users;
    }

    public void setUsers(List<UserAuthInfoResponse> users) {
        this.users = users;
    }
} 