package com.team1_5.credwise.controller;


import com.team1_5.credwise.dto.*;
import com.team1_5.credwise.service.AuthService;
import com.team1_5.credwise.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;





@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            @RequestHeader("Authorization") String token
    ) {
        return ResponseEntity.ok(authenticationService.refreshToken(token.substring(7)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String token
    ) {
        Long userId = jwtUtil.extractUserId(token.substring(7));
        authenticationService.logout(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> requestPasswordReset(
            @RequestParam String email
    ) {
        authenticationService.requestPasswordReset(email);
        return ResponseEntity.accepted().build();
    }
}