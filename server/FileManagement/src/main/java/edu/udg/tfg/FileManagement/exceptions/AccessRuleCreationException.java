package edu.udg.tfg.FileManagement.exceptions;

public class AccessRuleCreationException extends RuntimeException {
    public AccessRuleCreationException(String message) {
        super(message);
    }
    
    public AccessRuleCreationException(String message, Throwable cause) {
        super(message, cause);
    }
} 