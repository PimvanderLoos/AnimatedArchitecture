package nl.pim16aap2.animatedarchitecture.core.api;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Represents a key that exists in a namespace.
 */
@Getter
@ToString
@EqualsAndHashCode
public final class NamespacedKey
{
    /**
     * The regex rule that the namespace and key must match.
     */
//    public static final String REGEX = "^[a-z0-9_-]+$";
    public static final Pattern PATTERN = Pattern.compile("^[a-z0-9_-]+$");

    /**
     * The namespace of this {@link NamespacedKey}.
     *
     * @return The namespace of this {@link NamespacedKey}.
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final String namespace;

    /**
     * The key of this {@link NamespacedKey}.
     *
     * @return The key of this {@link NamespacedKey}.
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final String key;

    /**
     * The fully qualified name of this {@link NamespacedKey}.
     * <p>
     * This is formatted as {@code namespace:key}.
     *
     * @return The fully qualified name of this {@link NamespacedKey}.
     */
    private final String fullKey;

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

        this.namespace = verifiedNamespace;
        this.key = verifiedName;
        this.fullKey = verifiedNamespace + ":" + verifiedName;
    }

    /**
     * Creates a new {@link NamespacedKey} from the provided input.
     * <p>
     * If the input is of the form {@code namespace:key}, the namespace and key are extracted.
     * <p>
     * If the input is of the form {@code key}, the namespace is set to {@link Constants#PLUGIN_NAME}.
     *
     * @param input
     *     The input to create the {@link NamespacedKey} from.
     * @return The created {@link NamespacedKey}.
     */
    public static NamespacedKey of(String input)
    {
        final String[] split = input.split(":", 2);
        if (split.length == 1)
            return new NamespacedKey(Constants.PLUGIN_NAME, split[0]);
        return new NamespacedKey(split[0], split[1]);
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
        if (!PATTERN.matcher(testLower).matches())
            throw new IllegalArgumentException(title + " must match the regex rule: " + PATTERN + ". Found: " + test);
        return testLower;
    }
}
