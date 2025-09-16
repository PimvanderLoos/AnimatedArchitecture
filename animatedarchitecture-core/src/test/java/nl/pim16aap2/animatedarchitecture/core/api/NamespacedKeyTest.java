package nl.pim16aap2.animatedarchitecture.core.api;

import nl.pim16aap2.animatedarchitecture.core.exceptions.InvalidNameSpacedKeyException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class NamespacedKeyTest
{
    @Test
    public void testStaticOfWithNamespace()
    {
        final NamespacedKey key = NamespacedKey.of("owner:Name");
        assertEquals("owner:name", key.getFullKey());
    }

    @Test
    public void testStaticOfWithoutNamespace()
    {
        final NamespacedKey key = NamespacedKey.of("name");
        assertEquals("animatedarchitecture:name", key.getFullKey());
    }

    @Test
    public void testValidNamespaceAndName()
    {
        final NamespacedKey key = new NamespacedKey("owner", "name");
        assertEquals("owner:name", key.getFullKey());
    }

    @Test
    public void testUpperCaseNamespaceAndName()
    {
        final NamespacedKey key = new NamespacedKey("Owner", "Name");
        assertEquals("owner:name", key.getFullKey());
        Assertions.assertEquals("owner", key.getNamespace());
        Assertions.assertEquals("name", key.getKey());
    }

    @Test
    public void testNullNamespace()
    {
        final InvalidNameSpacedKeyException exception = assertThrows(
            InvalidNameSpacedKeyException.class,
            () -> new NamespacedKey(null, "name")
        );
        assertEquals("Namespace cannot be null.", exception.getMessage());
    }

    @Test
    public void testNullName()
    {
        final InvalidNameSpacedKeyException exception = assertThrows(
            InvalidNameSpacedKeyException.class,
            () -> new NamespacedKey("owner", null)
        );
        assertEquals("Name cannot be null.", exception.getMessage());
    }

    @Test
    public void testInvalidCharactersInNamespace()
    {
        final InvalidNameSpacedKeyException exception = assertThrows(
            InvalidNameSpacedKeyException.class,
            () -> new NamespacedKey("Owner!", "name")
        );
        assertEquals("Namespace must match the regex rule: ^[a-z0-9_-]+$. Found: Owner!", exception.getMessage());
    }

    @Test
    public void testInvalidCharactersInName()
    {
        final InvalidNameSpacedKeyException exception = assertThrows(
            InvalidNameSpacedKeyException.class,
            () -> new NamespacedKey("owner", "Name!")
        );
        assertEquals("Name must match the regex rule: ^[a-z0-9_-]+$. Found: Name!", exception.getMessage());
    }

    @Test
    public void testEqualsAndHashCode()
    {
        // Setup
        final NamespacedKey key1 = new NamespacedKey("owner", "Name");
        final NamespacedKey key2 = new NamespacedKey("Owner", "name");

        // Verify
        assertThat(key1).isEqualTo(key2);
        assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
    }

    @Test
    public void testToString()
    {
        // Setup
        final NamespacedKey key = new NamespacedKey("owner", "name");

        // Verify
        assertThat(key.toString()).isEqualTo("NamespacedKey(owner:name)");
    }
}
