package com.team1_5.credwise.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for password-related operations
 *
 * Provides methods for password generation, validation, and strength checking
 *
 * @author Credwise Development Team
 * @version 1.0.0
 */
@Component
public class PasswordUtil {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(12);

    // Password strength validation patterns
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\$$\\$${};':\"\\\\|,.<>/?]");

    /**
     * Generate a random, strong password
     *
     * @param length Length of the password
     * @return Generated password
     */
    public static String generateStrongPassword(int length) {
        if (length < 12) {
            length = 12; // Minimum recommended length
        }

        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(length);

        // Character sets
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?";
        String allChars = uppercase + lowercase + numbers + specialChars;

        // Ensure at least one character from each set
        password.append(uppercase.charAt(random.nextInt(uppercase.length())));
        password.append(lowercase.charAt(random.nextInt(lowercase.length())));
        password.append(numbers.charAt(random.nextInt(numbers.length())));
        password.append(specialChars.charAt(random.nextInt(specialChars.length())));

        // Fill the rest of the password
        for (int i = 4; i < length; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle the password
        return shuffleString(password.toString());
    }

    /**
     * Check password strength
     *
     * @param password Password to check
     * @return Password strength level
     */
    public static PasswordStrength checkPasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            return PasswordStrength.WEAK;
        }

        int strengthScore = 0;

        // Check length
        if (password.length() >= 12) strengthScore++;
        if (password.length() >= 16) strengthScore++;

        // Check character types
        if (UPPERCASE_PATTERN.matcher(password).find()) strengthScore++;
        if (LOWERCASE_PATTERN.matcher(password).find()) strengthScore++;
        if (NUMBER_PATTERN.matcher(password).find()) strengthScore++;
        if (SPECIAL_CHAR_PATTERN.matcher(password).find()) strengthScore++;

        // Determine strength
        if (strengthScore <= 2) return PasswordStrength.WEAK;
        if (strengthScore <= 4) return PasswordStrength.MEDIUM;
        return PasswordStrength.STRONG;
    }

    /**
     * Validate password against complexity requirements
     *
     * @param password Password to validate
     * @return Boolean indicating password validity
     */
    public static boolean isValidPassword(String password) {
        if (password == null) return false;

        // Regex for password validation
//        String passwordRegex = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\$$\\$${};':\"\\\\|,.<>/?]).{12,}$";

        String passwordRegex = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\{};':\"\\\\|,.<>?/]).{12,}$";

        return password.matches(passwordRegex);
    }

    /**
     * Encode password
     *
     * @param rawPassword Raw password
     * @return Encoded password
     */
    public static String encodePassword(String rawPassword) {
        return PASSWORD_ENCODER.encode(rawPassword);
    }

    /**
     * Check if raw password matches encoded password
     *
     * @param rawPassword Raw password
     * @param encodedPassword Encoded password
     * @return Boolean indicating password match
     */
    public static boolean matchPassword(String rawPassword, String encodedPassword) {
        return PASSWORD_ENCODER.matches(rawPassword, encodedPassword);
    }

    /**
     * Shuffle a string randomly
     *
     * @param input String to shuffle
     * @return Shuffled string
     */
    private static String shuffleString(String input) {
        char[] characters = input.toCharArray();
        SecureRandom random = new SecureRandom();

        for (int i = characters.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            char temp = characters[index];
            characters[index] = characters[i];
            characters[i] = temp;
        }

        return new String(characters);
    }

    /**
     * Password Strength Enum
     */
    public enum PasswordStrength {
        WEAK,
        MEDIUM,
        STRONG
    }
}