package edu.udg.tfg.UserAuthentication.services;

import edu.udg.tfg.UserAuthentication.controllers.requests.UserRegisterRequest;
import edu.udg.tfg.UserAuthentication.controllers.responses.UserAuthInfoListResponse;
import edu.udg.tfg.UserAuthentication.controllers.responses.UserAuthInfoResponse;
import edu.udg.tfg.UserAuthentication.entities.UserEntity;
import edu.udg.tfg.UserAuthentication.feignClients.fileManagement.FileManagementClient;
import edu.udg.tfg.UserAuthentication.feignClients.userManagement.UserManagementClient;
import edu.udg.tfg.UserAuthentication.feignClients.userManagement.UserRequest;
import edu.udg.tfg.UserAuthentication.queue.Sender;
import edu.udg.tfg.UserAuthentication.repositories.UserRepository;
import edu.udg.tfg.UserAuthentication.feignClients.syncService.SyncServiceClient;
import feign.FeignException;
import jakarta.ws.rs.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import edu.udg.tfg.UserAuthentication.entities.Roles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileManagementClient fileManagementClient;

    @Autowired
    private UserManagementClient userManagementClient;

    @Autowired
    private SyncServiceClient syncServiceClient;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Sender sender;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$");

    private void validateEmail(String email) {
        if (email == null || email.trim().length() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email format");
        }
    }

    private void validatePassword(String password, boolean isRequired) {
        if (password == null || password.trim().length() == 0) {
            if (isRequired) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
            }
            return;
        }
        
        if (password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters long");
        }
        
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least one uppercase letter, one lowercase letter, and one number");
        }
    }

    @Transactional
    public void register(UserRegisterRequest userInfoRequest) {
        validateEmail(userInfoRequest.getEmail());
        
        validatePassword(userInfoRequest.getPassword(), true);
        
        if (userInfoRequest.getFirstName() != null && userInfoRequest.getFirstName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First name cannot be empty");
        }
        
        try {
            UserEntity user = new UserEntity();
            user.setUsername(userInfoRequest.getUsername());
            user.setPassword(passwordEncoder.encode(userInfoRequest.getPassword()));
            user.setLastPasswordChange(new Date());
            user.setRole(Roles.USER);
            user = userRepository.save(user);
            
            String rootId = fileManagementClient.createRoot(user.getId());
            syncServiceClient.addRoot(user.getId(), UUID.fromString(rootId));
            userManagementClient.creteUser(user.getId(), userRequest(userInfoRequest));
            
        } catch (FeignException.Forbidden e) {
            UserEntity user = userRepository.findByUsername(userInfoRequest.getUsername()).orElse(null);
            if(user != null) {
                userRepository.delete(user);
            } 
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Root not created");
        } catch (Exception e) {
            UserEntity user = userRepository.findByUsername(userInfoRequest.getUsername()).orElse(null);
            if(user != null) {
                userRepository.delete(user);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"User registration failed: " + e.getMessage(), e);
        }
    }

    private UserRequest userRequest(UserRegisterRequest userInfoRequest) {
        UserRequest userRequest = new UserRequest();
        userRequest.setEmail(userInfoRequest.getEmail());
        userRequest.setCreatedDate(new Date());
        userRequest.setFirstName(userInfoRequest.getFirstName());
        userRequest.setLastName(userInfoRequest.getLastName());
        userRequest.setLastModifiedDate(new Date());
        return userRequest;
    }

    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username);
    }

    public Optional<UserEntity> findById(UUID id) {
        return userRepository.findById(id);
    }

    public UserEntity loadUserByUsername(String username) {
        Optional<UserEntity> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with username: " + username);
        }
        return  userOptional.get();
    }

    public boolean checkCredentials(String username, String password) {
        Optional<UserEntity> user = findByUsername(username);
        return user.isPresent() && user.get().getPassword() != null && passwordEncoder.matches(password, user.get().getPassword());
    }

    public UserEntity getUserByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }
        
        validatePassword(newPassword, true);
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLastPasswordChange(new Date());
        userRepository.save(user);
    }

    public UserEntity getUserName(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public List<UUID> searchUserIdsByUsername(String username) {
        return userRepository.findByUsernameContainingIgnoreCase(username).stream()
                .map(UserEntity::getId)
                .toList();
    }

    public UserAuthInfoListResponse getUsernamesByIds(List<UUID> userIds) {
        List<UserAuthInfoResponse> userInfos = userRepository.findAllById(userIds).stream()
                .map(user -> new UserAuthInfoResponse(user.getId(), user.getUsername()))
                .collect(Collectors.toList());
        return new UserAuthInfoListResponse(userInfos);
    }

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    public boolean isEmailAvailable(String email) {
        try {
            ResponseEntity<Boolean> response = userManagementClient.checkEmailAvailability(email);
            return response.getBody() != null ? response.getBody() : false;
        } catch (Exception e) {
            return false;
        }
    }

    public void updateUser(UUID userId, String username, String role) {
        Optional<UserEntity> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            UserEntity user = userOptional.get();
            user.setUsername(username);
            user.setRole(Roles.valueOf(role));
            userRepository.save(user);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    public void updateUserByAdmin(UUID userId, String username, String role, String password) {
        Optional<UserEntity> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            UserEntity user = userOptional.get();
            user.setUsername(username);
            user.setRole(Roles.valueOf(role));
            
            if (password != null && !password.trim().isEmpty()) {
                validatePassword(password, false);
                user.setPassword(passwordEncoder.encode(password));
                user.setLastPasswordChange(new Date());
            }
            
            userRepository.save(user);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    public void deleteByUser(UUID userId) {
        userRepository.deleteById(userId);
    }
}
