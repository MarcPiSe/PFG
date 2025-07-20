package edu.udg.tfg.UserManagement.controllers.responses;

import edu.udg.tfg.UserManagement.feignClients.userAuth.responses.Roles;
import java.util.UUID;

public class UserAuthDetails {
    private UUID id;
    private String username;
    private String password;
    private Roles role;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Roles getRole() { return role; }
    public void setRole(Roles role) { this.role = role; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
} 