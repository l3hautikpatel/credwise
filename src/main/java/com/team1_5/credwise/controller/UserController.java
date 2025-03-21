package com.team1_5.credwise.controller;


import com.team1_5.credwise.dto.RegisterRequest;
import com.team1_5.credwise.dto.LoginRequest;
import com.team1_5.credwise.dto.LoginResponse;
import com.team1_5.credwise.dto.UserUpdateRequest;
import com.team1_5.credwise.model.User;
import com.team1_5.credwise.service.AuthService;
import com.team1_5.credwise.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUserProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest updateRequest
    ) {
        User updatedUser = userService.updateUserProfile(userId, updateRequest);
        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<Void> changeUserStatus(
            @PathVariable Long userId,
            @RequestParam User.UserStatus status
    ) {
        userService.changeUserStatus(userId, status);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}