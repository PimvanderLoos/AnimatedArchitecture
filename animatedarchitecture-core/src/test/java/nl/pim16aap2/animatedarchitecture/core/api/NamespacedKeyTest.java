package nl.pim16aap2.animatedarchitecture.core.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NamespacedKeyTest
{
    @Test
    public void testValidNamespaceAndName()
    {
        final NamespacedKey key = new NamespacedKey("owner", "name");
        assertEquals("owner:name", key.getKey());
    }

    @Test
    public void testUpperCaseNamespaceAndName()
    {
        final NamespacedKey key = new NamespacedKey("Owner", "Name");
        assertEquals("owner:name", key.getKey());
    }

    @Test
    public void testNullNamespace()
    {
        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new NamespacedKey(null, "name")
        );
        assertEquals("Namespace cannot be null.", exception.getMessage());
    }

    @Test
    public void testNullName()
    {
        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new NamespacedKey("owner", null)
        );
        assertEquals("Name cannot be null.", exception.getMessage());
    }

    @Test
    public void testInvalidCharactersInNamespace()
    {
        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new NamespacedKey("Owner!", "name")
        );
        assertEquals("Namespace must match the regex rule: ^[a-z0-9_]+$. Found: Owner!", exception.getMessage());
    }

    @Test
    public void testInvalidCharactersInName()
    {
        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new NamespacedKey("owner", "Name!")
        );
        assertEquals("Name must match the regex rule: ^[a-z0-9_]+$. Found: Name!", exception.getMessage());
    }

    @Test
    public void testEqualsAndHashCode()
    {
        NamespacedKey key1 = new NamespacedKey("owner", "Name");
        NamespacedKey key2 = new NamespacedKey("Owner", "name");
        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    public void testToString()
    {
        NamespacedKey key = new NamespacedKey("owner", "name");
        assertEquals("NamespacedKey(key=owner:name)", key.toString());
    }
}
