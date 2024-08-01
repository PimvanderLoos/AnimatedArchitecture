package nl.pim16aap2.animatedarchitecture.core.api;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Represents a key that exists in a namespace.
 */
@Getter
@ToString
@EqualsAndHashCode
public final class NamespacedKey
{
    // Regex rule for the owner and name.
    // Each String can only contain lowercase letters, numbers, and underscores.
    private static final String REGEX = "^[a-z0-9_]+$";

    /**
     * The key of this {@link NamespacedKey}.
     * <p>
     * This key is formatted as follows: "owner:name".
     *
     * @return The key of this {@link NamespacedKey}.
     */
    private final String key;

    /**
     * Creates a new {@link NamespacedKey} with the given namespace and name.
     * <p>
     * Each String can only contain letters, numbers, and underscores.
     *
     * @param namespace
     *     The namespace of the key.
     * @param name
     *     The name of the key.
     */
    public NamespacedKey(String namespace, String name)
    {
        final String verifiedNamespace = verify("Namespace", namespace);
        final String verifiedName = verify("Name", name);

        this.key = verifiedNamespace + ":" + verifiedName;
    }

    /**
     * Verifies that the given test is not null and matches the regex.
     * <p>
     * The input is converted to lowercase.
     *
     * @param title
     *     The title of the test. For example, "Namespace" or "Name". This is used in the exception message to indicate
     *     which part of the key is invalid.
     * @param test
     *     The String to test.
     * @return The test String in lowercase if it is valid.
     *
     * @throws IllegalArgumentException
     *     If the test is null or does not match the regex rule.
     */
    static String verify(String title, @Nullable String test)
    {
        if (test == null)
            throw new IllegalArgumentException(title + " cannot be null.");

        final String testLower = test.toLowerCase(Locale.ROOT);
        if (!testLower.matches(REGEX))
            throw new IllegalArgumentException(title + " must match the regex rule: " + REGEX + ". Found: " + test);
        return testLower;
    }
}
