package com.team1_5.credwise.service;

import com.team1_5.credwise.dto.RegisterRequest;
import com.team1_5.credwise.model.User;
import com.team1_5.credwise.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;




@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String registerUser(RegisterRequest registerRequest) {
        // Check if email already exists
        if (userRepository.findByEmail(registerRequest.getEmail()) != null) {
            return "Email is already registered";
        }

        // Create new user
        User user = new User();
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setEmail(registerRequest.getEmail());
        user.setDateOfBirth(registerRequest.getDateOfBirth());

        // Encrypt password
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        // Save user
        userRepository.save(user);

        return "User registered successfully";
    }
}