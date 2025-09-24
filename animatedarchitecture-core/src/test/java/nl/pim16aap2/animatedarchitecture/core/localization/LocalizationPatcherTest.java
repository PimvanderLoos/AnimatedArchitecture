package nl.pim16aap2.animatedarchitecture.core.localization;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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
