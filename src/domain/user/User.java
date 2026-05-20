package domain.user;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * This Code is written by Mr. Sohanur, the Software Engineer of Code&Coffee inc.
 * Abstract base class representing any authenticated user in the SmartShield system.
 *
 * Design intent: This class is never instantiated directly. All users are either a Customer or an Admin.
 * Common identity and authentication concerns live here;
 * role specific behavior lives in subclasses.
 *
 * OOP Principles demonstrated:
 * - Abstraction : abstract class prevents direct instantiation
 * - Encapsulation : all fields private, password never exposed
 * - Polymorphism : getDashboardSummary() resolved at runtime
 *
 */
public abstract class User {

    // --- Identity Fields ---
    // final: assigned once at object creation, never reassigned
    private final int userId;
    private final LocalDateTime createdAt;
    private final UserRole role;

    // Mutable identity fields — can be updated via profile management
    private String name;
    private String email;

    // Security field — NEVER expose via a getter
    // The object answers questions about the password; it never reveals the hash
    private String passwordHash;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Protected constructor — only callable from subclasses via super().
     * This enforces that User cannot be instantiated without a concrete subtype.
     *
     * @param userId       the unique database ID for this user
     * @param name         the user's full name
     * @param email        the user's email address (used for login)
     * @param passwordHash the pre-hashed password (hashing done in service layer)
     * @param role         the role determining system access level
     */
    protected User(int userId, String name, String email,
                   String passwordHash, UserRole role) {

        // Defensive validation — fail fast if invalid data reaches the domain
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("User name cannot be null or blank");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email address: " + email);
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be null or blank");
        }

        this.userId       = userId;
        this.name         = name;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.role         = role;
        this.createdAt    = LocalDateTime.now();  // immutable timestamp at creation
    }

    // -------------------------------------------------------------------------
    // Abstract Contract
    // -------------------------------------------------------------------------

    /**
     * Each user type must define its own dashboard summary.
     * A Customer sees account balance and recent transactions.
     * An Admin sees fraud alerts and system statistics.
     *
     * This method is the polymorphism hook — the service layer calls it
     * without knowing or caring which subtype it holds.
     *
     * @return a formatted summary string appropriate for this user type
     */
    public abstract String getDashboardSummary();

    // -------------------------------------------------------------------------
    // Authentication
    // -------------------------------------------------------------------------

    /**
     * Verifies a login attempt without exposing the stored hash.
     *
     * The caller passes a hashed input (hashed by AuthenticationService).
     * This method compares hashes internally and returns only a boolean.
     * The actual hash value never leaves this object.
     *
     * @param hashedInput the hashed version of the password attempt
     * @return true if the hashes match, false otherwise
     */
    public boolean verifyPassword(String hashedInput) {
        if (hashedInput == null) return false;
        return this.passwordHash.equals(hashedInput);
    }

    /**
     * Replaces the stored password hash.
     * Called by AuthenticationService after validating the old password.
     * Not exposed as a public setter — password changes require business logic.
     *
     * @param newPasswordHash the new pre-hashed password
     */
    protected void updatePasswordHash(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new IllegalArgumentException("New password hash cannot be blank");
        }
        this.passwordHash = newPasswordHash;
    }

    // -------------------------------------------------------------------------
    // Getters — Read Access
    // -------------------------------------------------------------------------

    public int getUserId()              { return userId; }
    public String getName()             { return name; }
    public String getEmail()            { return email; }
    public UserRole getRole()           { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // -------------------------------------------------------------------------
    // Protected Setters — Subclass-controlled mutation
    // -------------------------------------------------------------------------

    // Protected: only subclasses (Customer, Admin) can delegate profile updates.
    // External code must go through the service layer, which calls the subclass,
    // which calls these. This preserves the encapsulation chain.

    protected void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
        this.name = name;
    }

    protected void setEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email: " + email);
        }
        this.email = email;
    }

    // -------------------------------------------------------------------------
    // Standard Object Methods
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User other = (User) o;
        // Two users are equal if they have the same database ID
        return this.userId == other.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        // IMPORTANT: passwordHash deliberately excluded from toString()
        // Never let a hash appear in logs or console output
        return String.format("User[id=%d, name='%s', role=%s, createdAt=%s]",
                userId, name, role, createdAt);
    }
}

/**
 * Defines the two valid roles in the SmartShield system.
 *
 * Using an enum instead of String ensures:
 * 1. Invalid roles cannot be assigned (compile-time safety)
 * 2. Role comparisons use == instead of .equals(), avoiding null bugs
 * 3. Switch statements on UserRole are exhaustive and compiler-audited
 */
public enum UserRole {
    CUSTOMER,
    ADMIN;

    /**
     * Returns true if this role has administrative system access.
     * Centralizes the authorization rule in one place.
     */
    public boolean isAdminRole() {
        return this == ADMIN;
    }
}


