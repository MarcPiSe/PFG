package edu.udg.tfg.UserManagement.controllers.responses;

/**
 * DTO para resultados de búsqueda de usuarios en endpoints públicos.
 * NO incluye el userId para cumplir con los requisitos de la Fase 3.5.
 */
public class UserSearchResult {
    private String email;
    private String username;

    public UserSearchResult(String email, String username) {
        this.email = email;
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
} 