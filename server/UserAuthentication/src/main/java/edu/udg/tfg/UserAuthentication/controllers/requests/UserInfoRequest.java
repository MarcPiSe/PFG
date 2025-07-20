package edu.udg.tfg.UserAuthentication.controllers.requests;

public class UserInfoRequest {

    private String username;
    private String password;

    public UserInfoRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public UserInfoRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
