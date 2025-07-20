package edu.udg.tfg.UserManagement.services;

import edu.udg.tfg.UserManagement.controllers.responses.UserAuthDetails;
import edu.udg.tfg.UserManagement.controllers.responses.UserDetails;
import edu.udg.tfg.UserManagement.entities.UserInfo;
import edu.udg.tfg.UserManagement.feignClients.userAuth.UserAuthClient;
import edu.udg.tfg.UserManagement.feignClients.userAuth.responses.Roles;
import edu.udg.tfg.UserManagement.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAuthClient userAuthClient;

    @Autowired
    private UserService userService;

    public List<UserDetails> getAllUsers(boolean superAdmin) {
        List<UserInfo> userInfos = userRepository.findAll();
        List<UserAuthDetails> userAuthDetails = userAuthClient.getAllUsers();

        return userInfos.stream()
                .map(userInfo -> {
                    UserAuthDetails authDetails = userAuthDetails.stream()
                            .filter(auth -> auth.getId().equals(userInfo.getId()))
                            .findFirst()
                            .orElse(null);
                    return new UserDetails(userInfo, authDetails, superAdmin);
                })
                .collect(Collectors.toList());
    }

    public UserDetails getUser(String username) {
        UUID userId = userService.getUserIdByUsername(username);
        UserInfo userInfo = userService.getUserById(userId);
        UserAuthDetails userAuthDetails = userAuthClient.getUserAuthDetails(userId);
        return new UserDetails(userInfo, userAuthDetails, false);
    }

    @Transactional
    public void updateUser(UUID id, UUID userId, UserDetails userDTO) {
        UserInfo userInfo = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        userInfo.setEmail(userDTO.getEmail());
        userInfo.setFirstName(userDTO.getFirstName());
        userInfo.setLastName(userDTO.getLastName());
        userRepository.save(userInfo);

        UserAuthDetails userAuthDetails = new UserAuthDetails();
        userAuthDetails.setUsername(userDTO.getUsername());
        userAuthDetails.setRole(Roles.valueOf(userDTO.getRole()));
        
        if (userDTO.getPassword() != null && !userDTO.getPassword().trim().isEmpty()) {
            userAuthDetails.setPassword(userDTO.getPassword());
        }
        
        userAuthClient.updateUser(id, userId, userAuthDetails);
    }
} 