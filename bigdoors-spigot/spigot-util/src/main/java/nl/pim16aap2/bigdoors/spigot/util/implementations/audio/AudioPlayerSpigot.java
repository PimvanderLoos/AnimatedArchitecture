package nl.pim16aap2.bigdoors.spigot.util.implementations.audio;

import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.audio.IAudioPlayer;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents an implementation of {@link IAudioPlayer} for the Spigot platform.
 *
 * @author Pim
 */
@Singleton
public class AudioPlayerSpigot implements IAudioPlayer
{
    private final IPExecutor executor;

    @Inject
    public AudioPlayerSpigot(IPExecutor executor)
    {
        this.executor = executor;
    }

    @Override
    public void playSound(IPLocation loc, String sound, float volume, float pitch)
    {
        playSound(SpigotAdapter.getBukkitLocation(loc), sound, volume, pitch);
    }

    @Override
    public void playSound(Vector3Di pos, IPWorld world, String sound, float volume, float pitch)
    {
        playSound(new Location(Bukkit.getWorld(world.worldName()), pos.x(), pos.y(), pos.z()), sound, volume, pitch);
    }

    @Override
    public void playSound(Vector3Dd pos, IPWorld world, String sound, float volume, float pitch)
    {
        playSound(new Location(Bukkit.getWorld(world.worldName()), pos.x(), pos.y(), pos.z()), sound, volume, pitch);
    }

    @Override
    public void playSound(double x, double y, double z, IPWorld world, String sound, float volume, float pitch)
    {
        playSound(new Location(Bukkit.getWorld(world.worldName()), x, y, z), sound, volume, pitch);
    }

    /**
     * Play a sound for all players in a range of 15 blocks around the provided location.
     */
    private void playSound(Location loc, String sound, float volume, float pitch)
    {
        final @Nullable World world = loc.getWorld();
        if (world == null)
            return;

        if (executor.isMainThread())
            playSound(loc, world, sound, volume, pitch, 15);
        else
            executor.scheduleOnMainThread(() -> playSound(loc, world, sound, volume, pitch, 15));
    }

    private void playSound(Location loc, World world, String sound, float volume, float pitch, int range)
    {
        for (final Entity ent : world.getNearbyEntities(loc, range, range, range))
            if (ent instanceof Player player)
                player.playSound(loc, sound, volume, pitch);
    }
}
