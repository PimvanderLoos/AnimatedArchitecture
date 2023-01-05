package nl.pim16aap2.bigdoors.localization;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LocalizationPatcherTest
{
    @Test
    void isValidPatch()
    {
        Assertions.assertFalse(LocalizationPatcher.isValidPatch(new LocalizationEntry("key", "")));
        Assertions.assertTrue(LocalizationPatcher.isValidPatch(new LocalizationEntry("key", " ")));
        Assertions.assertTrue(LocalizationPatcher.isValidPatch(new LocalizationEntry("key", "value")));
    }
}
