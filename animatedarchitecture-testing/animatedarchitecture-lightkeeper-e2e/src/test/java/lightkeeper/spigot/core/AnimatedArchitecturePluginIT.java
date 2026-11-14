package lightkeeper.spigot.core;

import nl.pim16aap2.lightkeeper.framework.ILightkeeperFramework;
import nl.pim16aap2.lightkeeper.framework.LightkeeperExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static nl.pim16aap2.lightkeeper.framework.assertions.LightkeeperAssertions.assertThat;

/** Checks the intended normal startup scenario with an explicit Vault fixture. */
@ExtendWith(LightkeeperExtension.class)
class AnimatedArchitecturePluginIT
{
    @Test
    void plugin_shouldEnableWithVaultFixture(ILightkeeperFramework framework)
    {
        // setup
        final String pluginName = "AnimatedArchitecture";

        // execute
        final var server = assertThat(framework).server();

        // verify
        server.plugin(pluginName).isEnabled();
    }
}
