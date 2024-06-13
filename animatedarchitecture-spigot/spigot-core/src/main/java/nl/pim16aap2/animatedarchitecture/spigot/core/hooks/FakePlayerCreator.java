package nl.pim16aap2.animatedarchitecture.spigot.core.hooks;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.spigot.core.AnimatedArchitecturePlugin;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Class used to create a fake-online player who is actually offline.
 */
@Singleton
@Flogger
public class FakePlayerCreator
{
    private final @Nullable Constructor<?> ctor;

    @Inject FakePlayerCreator(
        AnimatedArchitecturePlugin plugin,
        @Named("pluginBaseDirectory") Path pluginBaseDirectory)
    {
        this.ctor = getFakePlayerConstructor(plugin, pluginBaseDirectory);
    }

    private @Nullable Constructor<?> getFakePlayerConstructor(
        AnimatedArchitecturePlugin plugin,
        Path pluginBaseDirectory)
    {
        try
        {
            final Path outputDir = pluginBaseDirectory.resolve("generated_classes");
            return new FakePlayerClassGenerator(plugin, outputDir).getGeneratedConstructor();
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to create fake player constructor!");
            return null;
        }
    }

    /**
     * Checks whether a fake player can be created.
     *
     * @return {@code true} if a fake player can be created, {@code false} otherwise.
     */
    public boolean canCreatePlayers()
    {
        return ctor != null;
    }

    /**
     * Creates a fake player.
     *
     * @param player
     *     The offline player to create the fake-online player for.
     * @param location
     *     The location to create the fake player at.
     * @return The fake player.
     */
    public Optional<Player> createPlayer(OfflinePlayer player, Location location)
    {
        if (ctor == null)
        {
            log.atFine().log("Failed to create fake player: No constructor available!");
            return Optional.empty();
        }

        try
        {
            return Optional.of((Player) ctor.newInstance(player, location));
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to create fake player!");
            return Optional.empty();
        }
    }
}
