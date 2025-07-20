package edu.udg.tfg.UserAuthentication.controllers;

import edu.udg.tfg.UserAuthentication.controllers.requests.PasswordChangeRequest;
import edu.udg.tfg.UserAuthentication.controllers.requests.UserInfoRequest;
import edu.udg.tfg.UserAuthentication.controllers.requests.UserRegisterRequest;
import edu.udg.tfg.UserAuthentication.controllers.responses.UserAuthInfoListResponse;
import edu.udg.tfg.UserAuthentication.controllers.responses.UserAuthResponse;
import edu.udg.tfg.UserAuthentication.entities.UserEntity;
import edu.udg.tfg.UserAuthentication.services.UserService;
import edu.udg.tfg.UserAuthentication.utils.JwtTokenUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;



import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users/auth")
public class AuthenticationController {

    public static final Logger LOG = LoggerFactory.getLogger(AuthenticationController.class);

    public static final String ACCESS_NAME = "accessToken";
    public static final String REFRESH_NAME = "refreshToken";

    private final UserService userService;

    private final JwtTokenUtil jwtTokenUtil;

    public AuthenticationController(@Qualifier("userService") UserService userService, JwtTokenUtil jwtTokenUtil) {
        this.userService = userService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    private ResponseEntity<?> addTokenHeaders(UserEntity user) {
        String accessToken = jwtTokenUtil.generateToken(user, false);
        String refreshToken = jwtTokenUtil.generateToken(user, true);

        return ResponseEntity.ok().header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).header("X-Refresh-Token", refreshToken).build();
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody @Valid UserRegisterRequest userInfoRequest, HttpServletResponse response) {
        LOG.info("Registering new user: {}", userInfoRequest);
        
        if (userService.findByUsername(userInfoRequest.getUsername()).isPresent()) {
            LOG.info("User not registered. Username already taken.");
            return ResponseEntity.badRequest().body("Username is already taken");
        }
        
        if (!userService.isEmailAvailable(userInfoRequest.getEmail())) {
            LOG.info("User not registered. Email already taken.");
            return ResponseEntity.badRequest().body("Email is already taken");
        }
        
        try {
            userService.register(userInfoRequest);
            LOG.info("User registered successfully");

            UserEntity user = userService.loadUserByUsername(userInfoRequest.getUsername());
            return addTokenHeaders(user);
        } catch (IllegalArgumentException e) {
            LOG.warn("User registration failed due to validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody UserInfoRequest userInfoRequest) {
        LOG.info("Authenticating user: {}", userInfoRequest);

        if(!userService.checkCredentials(userInfoRequest.getUsername(), userInfoRequest.getPassword())) {
            LOG.info("User or password not valid");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
        LOG.info("User authenticated successfully");

        UserEntity user = userService.loadUserByUsername(userInfoRequest.getUsername());
        return addTokenHeaders(user);
    }

    @PostMapping("/keep-alive")
    public ResponseEntity<?> keepAlive(@RequestHeader("X-Refresh-Token") String refreshToken, HttpServletResponse response) {
        LOG.info("Refreshing tokens using refresh token from header.");

        if (!jwtTokenUtil.validateTokenExpiration(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired refresh token");
        }

        String username = jwtTokenUtil.extractUsername(refreshToken);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }

        UserEntity user = userService.loadUserByUsername(username);
        return addTokenHeaders(user);
    }

    @PostMapping("/check")
    public ResponseEntity<UserAuthResponse> authenticateUser(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        LOG.info("Checking if token is valid: {}", token);

        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        String jwtToken = token.substring(7);
        if (!jwtTokenUtil.validateTokenExpiration(jwtToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        String username = jwtTokenUtil.extractUsername(jwtToken);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        UserEntity user = userService.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        LOG.info("User authenticated successfully");

        UserAuthResponse userAuthResponse = new UserAuthResponse();
        userAuthResponse.setId(user.getId());
        userAuthResponse.setUsername(user.getUsername());
        userAuthResponse.setRole(user.getRole().name());
        userAuthResponse.setConnectionId(jwtTokenUtil.extractConnectionId(jwtToken));

        return ResponseEntity.ok(userAuthResponse);
    }

    @GetMapping("/{username}/id")
    public ResponseEntity<?> getId(@PathVariable("username") String username) {
        UserEntity userInfo = userService.getUserByUsername(username);
        return ResponseEntity.ok(userInfo.getId());
    }

    @PostMapping("/usernames")
    public ResponseEntity<UserAuthInfoListResponse> getUsernamesByIds(@RequestBody List<UUID> userIds) {
        return ResponseEntity.ok(userService.getUsernamesByIds(userIds));
    }

    @GetMapping("/internal/search-ids-by-username")
    public ResponseEntity<List<UUID>> searchUserIdsByUsername(@RequestParam("q") String query) {
        return ResponseEntity.ok(userService.searchUserIdsByUsername(query));
    }

    @GetMapping("/username")
    public ResponseEntity<?> getUserName(@RequestHeader("X-User-Id") UUID id) {
        UserEntity userInfo = userService.getUserName(id);
        return ResponseEntity.ok(userInfo.getUsername());
    }

    @GetMapping("/internal/users/{userId}")
    public ResponseEntity<UserEntity> getUserDetails(@PathVariable("userId") UUID userId) {
        UserEntity user = userService.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(user);
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestHeader("X-User-Id") UUID id, @RequestBody PasswordChangeRequest request) {

        UserEntity user = userService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        try {
            userService.changePassword(user.getId(), request.getOldPassword(), request.getNewPassword());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        return ResponseEntity.ok("Password changed successfully");
    }

    @GetMapping("/check-username")
    public ResponseEntity<Boolean> checkUsernameAvailability(@RequestParam("username") String username) {
        boolean isAvailable = userService.findByUsername(username).isEmpty();
        return ResponseEntity.ok(isAvailable);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAccount(@RequestHeader("X-User-Id") UUID id) {
        userService.deleteByUser(id);
        return ResponseEntity.ok("User account deletion process started");
    }

    @DeleteMapping("/internal/{userId}")
    public ResponseEntity<?> deleteAccountInternal(@PathVariable("userId") UUID userId) {
        userService.deleteByUser(userId);
        return ResponseEntity.ok("User account deletion process started");
    }

    @RequestMapping(value = "/login", method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handlePreflight() {
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> unhandledException(RuntimeException e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> statusException(ResponseStatusException e) {
        e.printStackTrace();
        return ResponseEntity.status(e.getStatusCode()).body(e.getMessage());
    }
}
