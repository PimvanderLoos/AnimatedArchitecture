package nl.pim16aap2.bigdoors.localization;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LocalizationPatcherTest
{
    @Test
    void isValidPatch()
    {
        Assertions.assertFalse(LocalizationPatcher.isValidPatch("key", "key="));
        //noinspection ConstantConditions
        Assertions.assertFalse(LocalizationPatcher.isValidPatch(null, "key="));
        Assertions.assertTrue(LocalizationPatcher.isValidPatch("key", "key= "));
        Assertions.assertTrue(LocalizationPatcher.isValidPatch("key", "key=value"));
    }
}
