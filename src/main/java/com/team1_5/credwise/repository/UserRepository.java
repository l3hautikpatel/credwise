//package com.team1_5.credwise.repository;
//
//import com.team1_5.credwise.model.User;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//@Repository
//public interface UserRepository extends JpaRepository<User, Long> {
//    User findByEmail(String email);
//    User findByPhoneNumber(String phoneNumber);
//}



package com.team1_5.credwise.repository;

import com.team1_5.credwise.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing User entities
 *
 * Provides database operations and custom query methods for users
 *
 * @author Credwise Development Team
 * @version 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email address
     *
     * @param email User's email address
     * @return Optional containing the user
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by phone number
     *
     * @param phoneNumber User's phone number
     * @return Optional containing the user
     */
    Optional<User> findByPhoneNumber(String phoneNumber);

    /**
     * Check if a user exists with the given email
     *
     * @param email User's email address
     * @return Boolean indicating user existence
     */
    boolean existsByEmail(String email);

    /**
     * Check if a user exists with the given phone number
     *
     * @param phoneNumber User's phone number
     * @return Boolean indicating user existence
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Find users registered within a specific date range
     *
     * @param startDate Beginning of the registration period
     * @param endDate End of the registration period
     * @return List of users registered in the specified period
     */
    List<User> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find users by their account status
     *
     * @param status User account status
     * @return List of users with the specified status
     */
    List<User> findByStatus(User.UserStatus status);

    /**
     * Find users with loan applications
     *
     * @return List of users who have submitted loan applications
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.loanApplications la")
    List<User> findUsersWithLoanApplications();

    /**
     * Find users by credit score range
     *
     * @param minScore Minimum credit score
     * @param maxScore Maximum credit score
     * @return List of users within the specified credit score range
     */
    @Query("SELECT u FROM User u WHERE u.creditScore BETWEEN :minScore AND :maxScore")
    List<User> findUsersByCreditScoreRange(
            @Param("minScore") Integer minScore,
            @Param("maxScore") Integer maxScore
    );

    /**
     * Find users by multiple criteria
     *
     * @param email Optional email filter
     * @param status Optional user status filter
     * @param minCreditScore Optional minimum credit score
     * @return List of users matching the criteria
     */
    @Query("SELECT u FROM User u WHERE " +
            "(:email IS NULL OR u.email = :email) AND " +
            "(:status IS NULL OR u.status = :status) AND " +
            "(:minCreditScore IS NULL OR u.creditScore >= :minCreditScore)")
    List<User> findByMultipleCriteria(
            @Param("email") String email,
            @Param("status") User.UserStatus status,
            @Param("minCreditScore") Integer minCreditScore
    );

    /**
     * Count users by registration date
     *
     * @param startDate Beginning of the registration period
     * @param endDate End of the registration period
     * @return Number of users registered in the specified period
     */
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find users with no loan applications
     *
     * @return List of users without any loan applications
     */
    @Query("SELECT u FROM User u LEFT JOIN u.loanApplications la WHERE la IS EMPTY")
    List<User> findUsersWithoutLoanApplications();

    /**
     * Custom method to update user status
     *
     * @param userId User ID
     * @param status New user status
     */
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :userId")
    void updateUserStatus(
            @Param("userId") Long userId,
            @Param("status") User.UserStatus status
    );
}