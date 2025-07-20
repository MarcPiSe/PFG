package edu.udg.tfg.UserManagement.controllers.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.udg.tfg.UserManagement.entities.UserInfo;

import java.util.Date;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDetails {

    private String username;
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private Date createdDate;
    private Date lastModifiedDate;

    public UserDetails() {
    }

    public UserDetails(UserInfo userInfo, UserAuthDetails userAuthDetails, boolean superAdmin) {
        this.email = userInfo.getEmail();
        this.firstName = userInfo.getFirstName();
        this.lastName = userInfo.getLastName();
        this.createdDate = userInfo.getCreatedDate();
        this.lastModifiedDate = userInfo.getLastModifiedDate();

        if (userAuthDetails != null) {
            this.role = userAuthDetails.getRole().name();
            this.username = userAuthDetails.getUsername();
            if(superAdmin) {
                this.password = userAuthDetails.getPassword();
            }
        }
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
    
    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
    
    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
    
} 