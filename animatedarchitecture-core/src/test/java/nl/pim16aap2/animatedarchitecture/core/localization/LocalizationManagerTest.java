package nl.pim16aap2.animatedarchitecture.core.localization;

import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.config.IConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Locale;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalizationManagerTest
{
    @Mock
    private IConfig config;

    @TempDir
    private Path tmpDir;

    @Test
    void initialize_shouldLoadBaseLocalizationResources()
    {
        // setup
        when(config.locale()).thenReturn(Locale.ENGLISH);
        when(config.allowClientLocale()).thenReturn(false);
        final LocalizationManager localizationManager =
            new LocalizationManager(new RestartableHolder(), tmpDir, "Core", config, true);

        // execute
        localizationManager.initialize();
        final String message = localizationManager.getLocalizer().getMessage("constants.error.generic");

        // verify
        Assertions.assertFalse(message.startsWith(Localizer.KEY_NOT_FOUND_MESSAGE));
    }
}
