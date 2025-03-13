package com.team1_5.credwise.service;

import org.slf4j.Logger;  // Use this import
import org.slf4j.LoggerFactory;  // Use this import for creating logger

import com.team1_5.credwise.dto.LoginRequest;
import com.team1_5.credwise.dto.LoginResponse;
import com.team1_5.credwise.dto.RegisterRequest;
import com.team1_5.credwise.model.User;
import com.team1_5.credwise.util.JwtUtil;
import com.team1_5.credwise.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    // Correct way to create a logger
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

//    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
//        this.userRepository = userRepository;
//        this.passwordEncoder = passwordEncoder;
//    }
    // Corrected constructor
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public String registerUser(RegisterRequest registerRequest) {
        // Log incoming registration request
        logger.info("Attempting to register user: {}", registerRequest.getEmail());

        // Check if user already exists by email or phone number
        if (userRepository.findByEmail(registerRequest.getEmail()) != null) {
            logger.warn("User already exists with email: {}", registerRequest.getEmail());
            throw new UserAlreadyExistsException("Email is already registered");
        }

        if (userRepository.findByPhoneNumber(registerRequest.getPhoneNumber()) != null) {
            logger.warn("User already exists with phone number: {}", registerRequest.getPhoneNumber());
            throw new UserAlreadyExistsException("Phone number is already registered");
        }

        try {
            // Create new user
            User user = new User();
            user.setFirstName(registerRequest.getFirstName());
            user.setLastName(registerRequest.getLastName());
            user.setEmail(registerRequest.getEmail());
            user.setPhoneNumber(registerRequest.getPhoneNumber());

            // Encrypt password
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

            // Save user
            userRepository.save(user);

            logger.info("User registered successfully: {}", registerRequest.getEmail());
            return "User registered successfully";
        } catch (Exception e) {
            logger.error("Error registering user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to register user", e);
        }
    }

    public LoginResponse loginUser(LoginRequest loginRequest) {
        logger.info("Attempting to login user: {}", loginRequest.getEmail());

        // Find user by email
        User user = userRepository.findByEmail(loginRequest.getEmail());
        if (user == null) {
            logger.warn("Login attempt for non-existent email: {}", loginRequest.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }

        // Check password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            logger.warn("Failed login attempt for email: {}", loginRequest.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail());

        // Create login response
        LoginResponse.UserDTO userDTO = new LoginResponse.UserDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
        );

        LoginResponse response = new LoginResponse(token, userDTO);

        logger.info("User logged in successfully: {}", loginRequest.getEmail());
        return response;
    }

}

// Custom exception
class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}