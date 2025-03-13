package com.team1_5.credwise.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF protection for API endpoints (only for development)
                .csrf(csrf -> csrf.disable())

                // Configure authorization
                .authorizeHttpRequests(authz -> authz
                        // Permit all requests to authentication-related endpoints
                        .requestMatchers("/api/auth/**").permitAll()

                        // Optionally, you can specify other public endpoints
                        .requestMatchers("/public/**").permitAll()

                        // All other endpoints require authentication
//                        .anyRequest().authenticated()

                )

                .formLogin().disable()  // Disable default login page
                .httpBasic().disable(); // Disable basic auth


        // Optional: Configure form login or HTTP basic auth
//                .httpBasic(httpBasic -> {});

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}