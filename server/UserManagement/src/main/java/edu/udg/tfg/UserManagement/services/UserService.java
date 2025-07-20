package edu.udg.tfg.UserManagement.services;

import edu.udg.tfg.UserManagement.controllers.requests.UpdateProfileRequest;
import edu.udg.tfg.UserManagement.controllers.requests.UserRequest;
import edu.udg.tfg.UserManagement.controllers.responses.UserSearchResult;
import edu.udg.tfg.UserManagement.controllers.responses.InternalUserDetails;
import edu.udg.tfg.UserManagement.controllers.responses.UserAuthDetails;
import edu.udg.tfg.UserManagement.controllers.responses.UserDetails;
import edu.udg.tfg.UserManagement.entities.UserInfo;
import edu.udg.tfg.UserManagement.feignClients.userAuth.UserAuthClient;
import edu.udg.tfg.UserManagement.feignClients.userAuth.responses.UserAuthInfoListResponse;
import edu.udg.tfg.UserManagement.feignClients.userAuth.responses.UserAuthInfoResponse;
import edu.udg.tfg.UserManagement.queue.Receiver;
import edu.udg.tfg.UserManagement.queue.Sender;
import edu.udg.tfg.UserManagement.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import edu.udg.tfg.UserManagement.feignClients.FileSharingClient;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.ArrayList;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Sender sender;

    @Autowired
    private UserAuthClient userAuthClient;

    @Autowired
    private FileSharingClient fileSharingClient;

    public List<UserInfo> getAllUsers() {
        return userRepository.findAll();
    }

    public UserInfo getUserById(UUID id) {
        return userRepository.findById(id)
                              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public UserDetails getUserProfile(UUID id) {
        UserInfo userInfo = getUserById(id);
        UserAuthDetails userAuthDetails = userAuthClient.getUserAuthDetails(id);
        return new UserDetails(userInfo, userAuthDetails, false);
    }

    public Optional<UserInfo> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    public UserInfo createUser(UserInfo userInfo) {
        return userRepository.save(userInfo);
    }

    public UserInfo updateUser(UUID id, UserRequest userRequest) {
        UserInfo userInfo = getUserById(id);
        userInfo.setFirstName(userRequest.getFirstName());
        userInfo.setLastName(userRequest.getLastName());
        userInfo.setEmail(userRequest.getEmail());

        return userRepository.save(userInfo);
    }

    public UserInfo updateUserProfile(UUID id, UpdateProfileRequest updateRequest) {
        UserInfo userInfo = getUserById(id);
        
        List<UserInfo> existingUsers = userRepository.findByEmailContainingIgnoreCase(updateRequest.getEmail());
        for (UserInfo existingUser : existingUsers) {
            if (!existingUser.getId().equals(id) && existingUser.getEmail().equalsIgnoreCase(updateRequest.getEmail())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use by another user");
            }
        }
        
        userInfo.setFirstName(updateRequest.getFirstName());
        userInfo.setLastName(updateRequest.getLastName());
        userInfo.setEmail(updateRequest.getEmail());

        return userRepository.save(userInfo);
    }

    @Transactional
    public void deleteUserLocal(UUID id) {
        userRepository.deleteById(id);
    }

    public List<UserSearchResult> searchUsers(String query) {
        List<UUID> ids = userAuthClient.searchUserIdsByUsername(query);

        List<UserInfo> users = userRepository.findByEmailContainingIgnoreCase(query);
        Map<UUID, UserInfo> map = users.stream().collect(Collectors.toMap(UserInfo::getId, userInfo -> userInfo));

        if (!ids.isEmpty()) {
            ids.forEach(id -> {
                if (!map.containsKey(id)) {
                    UserInfo userInfo = userRepository.findById(id).orElse(null);
                    if (userInfo != null) {
                        map.put(id, userInfo);
                    }
                }
            });
        }

        UserAuthInfoListResponse userAuthInfoList = userAuthClient.getUsernamesByIds(new ArrayList<>(map.keySet()));

        List<UserSearchResult> results = new ArrayList<>();
        for (UserAuthInfoResponse userAuthInfo : userAuthInfoList.getUsers()) {
            UserInfo userInfo = map.get(userAuthInfo.getId());
            if (userInfo != null) {
                results.add(new UserSearchResult(userInfo.getEmail(), userAuthInfo.getUsername()));
            }
        }
        return results;
    }

    public UUID getUserIdByUsername(String username) {
        List<UUID> userIds = userAuthClient.searchUserIdsByUsername(username);
        
        for (UUID userId : userIds) {
            try {
                UserAuthDetails userAuthDetails = userAuthClient.getUserAuthDetails(userId);
                if (userAuthDetails.getUsername().equals(username)) {
                    return userId;
                }
            } catch (Exception e) {
                continue;
            }
        }
        
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User with username '" + username + "' not found");
    }

    public InternalUserDetails getInternalUserDetails(UUID userId) {
        UserInfo userInfo = getUserById(userId);
        UserAuthDetails userAuthDetails = userAuthClient.getUserAuthDetails(userId);
        
        return new InternalUserDetails(
            userInfo.getId(),
            userAuthDetails.getUsername(),
            userInfo.getEmail(),
            userInfo.getFirstName(),
            userInfo.getLastName()
        );
    }
}