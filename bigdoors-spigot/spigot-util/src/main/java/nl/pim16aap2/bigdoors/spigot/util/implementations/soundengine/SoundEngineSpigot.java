package nl.pim16aap2.bigdoors.spigot.util.implementations.soundengine;

import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.ISoundEngine;
import nl.pim16aap2.bigdoors.api.PSound;
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
 * Represents an implementation of {@link ISoundEngine} for the Spigot platform.
 *
 * @author Pim
 */
@Singleton
public class SoundEngineSpigot implements ISoundEngine
{
    private final IPExecutor executor;

    @Inject
    public SoundEngineSpigot(IPExecutor executor)
    {
        this.executor = executor;
    }

    @Override
    public void playSound(IPLocation loc, PSound sound, float volume, float pitch)
    {
        playSound(SpigotAdapter.getBukkitLocation(loc), PSound.getSoundName(sound), volume, pitch);
    }

    @Override
    public void playSound(Vector3Di pos, IPWorld world, PSound sound, float volume, float pitch)
    {
        playSound(new Location(Bukkit.getWorld(world.worldName()), pos.x(), pos.y(), pos.z()),
                  PSound.getSoundName(sound), volume, pitch);
    }

    @Override
    public void playSound(Vector3Dd pos, IPWorld world, PSound sound, float volume, float pitch)
    {
        playSound(new Location(Bukkit.getWorld(world.worldName()), pos.x(), pos.y(), pos.z()),
                  PSound.getSoundName(sound), volume, pitch);
    }

    @Override
    public void playSound(double x, double y, double z, IPWorld world, PSound sound, float volume, float pitch)
    {
        playSound(new Location(Bukkit.getWorld(world.worldName()), x, y, z), PSound.getSoundName(sound), volume, pitch);
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
