package domain.user;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * This code is written by Mr. Sohanur Rohman, software engineer of code&coffee inc.
 * Represents a bank customer in the SmartShield system.
 *
 * A Customer is a User with financial identity (national ID, phone),
 * a linked account (by ID, not by object reference — see design notes),
 * and optional Protected Mode for vulnerable users.
 *
 * OOP principles demonstrated:
 *  - Inheritance    : extends User, gains identity and authentication
 *  - Encapsulation  : isProtectedMode controlled via named methods, not setter
 *  - Polymorphism   : getDashboardSummary() provides customer-specific view
 *  - Composition    : accountId links to Account without tight coupling
 */
public class Customer extends User {

    // Immutable fields — set at registration, never change
    private final String nationalId;     // Bangladesh NID — fixed by government
    private final int    accountId;      // FK to accounts table — not the Account object

    // Mutable profile field
    private String phone;

    // Protected Mode — controlled state, never a raw setter
    private boolean       isProtectedMode;
    private LocalDateTime protectedModeEnabledAt;  // audit trail: when was it enabled?

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Creates a new Customer.
     *
     * Notice: UserRole.CUSTOMER is hardcoded — a Customer is always a customer.
     * No role parameter is accepted; the type hierarchy enforces the role.
     *
     * @param userId       database-assigned user ID
     * @param name         customer's full name
     * @param email        login email
     * @param passwordHash pre-hashed password (AuthenticationService hashes it)
     * @param phone        contact number (mutable — customers change phones)
     * @param nationalId   Bangladesh NID (immutable — fixed identity document)
     * @param accountId    FK to the customer's primary account (ID only, not object)
     */
    public Customer(int userId, String name, String email, String passwordHash,
                    String phone, String nationalId, int accountId) {

        // Chain to parent — UserRole.CUSTOMER is intentionally hardcoded here
        super(userId, name, email, passwordHash, UserRole.CUSTOMER);

        // Validate customer-specific fields
        if (nationalId == null || nationalId.isBlank()) {
            throw new IllegalArgumentException(
                    "National ID cannot be null or blank for customer ID: " + userId);
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException(
                    "Phone cannot be null or blank for customer ID: " + userId);
        }
        if (accountId <= 0) {
            throw new IllegalArgumentException(
                    "Account ID must be a positive integer, received: " + accountId);
        }

        this.nationalId            = nationalId;
        this.accountId             = accountId;
        this.phone                 = phone;
        this.isProtectedMode       = false;   // disabled by default at registration
        this.protectedModeEnabledAt = null;
    }

    // -------------------------------------------------------------------------
    // Polymorphic Contract — implements abstract method from User
    // -------------------------------------------------------------------------

    /**
     * Returns a customer-facing dashboard summary.
     *
     * This is the polymorphism point: a service holding a User reference
     * calls getDashboardSummary() and gets this output when the object
     * is actually a Customer — without any instanceof check.
     */
    @Override
    public String getDashboardSummary() {
        String protectionStatus = isProtectedMode
                ? "Protected Mode ON (since " + protectedModeEnabledAt + ")"
                : "Standard Mode";

        return String.format(
                "Customer Dashboard | Name: %s | Account ID: %d | Mode: %s",
                getName(), accountId, protectionStatus
        );
    }

    // -------------------------------------------------------------------------
    // Protected Mode — business operations, not raw setters
    // -------------------------------------------------------------------------

    /**
     * Activates Protected Mode for this customer.
     *
     * Protected Mode adds extra friction to transactions:
     * longer confirmation delays, additional warnings, and
     * educational fraud prompts — designed for elderly or
     * low-digital-literacy users in Bangladesh's fraud ecosystem.
     *
     * Records the activation timestamp for audit purposes.
     */
    public void enableProtectedMode() {
        this.isProtectedMode        = true;
        this.protectedModeEnabledAt = LocalDateTime.now();
    }

    /**
     * Deactivates Protected Mode.
     *
     * In production this would require admin authorization.
     * Clears the audit timestamp on deactivation.
     */
    public void disableProtectedMode() {
        this.isProtectedMode        = false;
        this.protectedModeEnabledAt = null;
    }

    // -------------------------------------------------------------------------
    // Profile Update Methods
    // -------------------------------------------------------------------------

    /**
     * Updates the customer's display name.
     *
     * Delegates to User's protected setName() — CustomerService calls this
     * public method rather than accessing User's internal setter directly.
     * This preserves the encapsulation chain across layers.
     */
    public void updateName(String newName) {
        setName(newName);    // protected method inherited from User
    }

    /**
     * Updates the customer's login email.
     * Delegates to User's protected setEmail().
     */
    public void updateEmail(String newEmail) {
        setEmail(newEmail);  // protected method inherited from User
    }

    /**
     * Updates the customer's phone number.
     * Phone is mutable — customers change providers and numbers.
     */
    public void updatePhone(String newPhone) {
        if (newPhone == null || newPhone.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be blank");
        }
        this.phone = newPhone;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public int     getAccountId()              { return accountId; }
    public String  getNationalId()             { return nationalId; }
    public String  getPhone()                  { return phone; }
    public boolean isProtectedMode()           { return isProtectedMode; }
    public LocalDateTime getProtectedModeEnabledAt() { return protectedModeEnabledAt; }

    // -------------------------------------------------------------------------
    // Standard Object Methods
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer)) return false;
        Customer other = (Customer) o;
        // Identity equality: same database user ID = same customer
        // Delegates to User.equals() which compares userId
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        // nationalId deliberately excluded — sensitive identity document
        return String.format(
                "Customer[id=%d, name='%s', phone='%s', protectedMode=%s, accountId=%d]",
                getUserId(), getName(), phone, isProtectedMode, accountId
        );
    }
}
