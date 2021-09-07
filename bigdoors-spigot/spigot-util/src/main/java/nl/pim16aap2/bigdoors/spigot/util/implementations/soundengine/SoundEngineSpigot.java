package nl.pim16aap2.bigdoors.spigot.util.implementations.soundengine;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.ISoundEngine;
import nl.pim16aap2.bigdoors.api.PSound;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bukkit.Bukkit;
import org.bukkit.Location;

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
    @Inject
    public SoundEngineSpigot()
    {
    }

    @Override
    public void playSound(IPLocation loc, PSound sound, float volume, float pitch)
    {
        BigDoors.get().getPlatform().getPExecutor().runAsync(() -> SpigotUtil
            .playSound(SpigotAdapter.getBukkitLocation(loc), PSound.getSoundName(sound), volume, pitch));
    }

    @Override
    public void playSound(Vector3Di pos, IPWorld world, PSound sound, float volume, float pitch)
    {
        SpigotUtil.playSound(new Location(Bukkit.getWorld(world.worldName()), pos.x(), pos.y(), pos.z()),
                             PSound.getSoundName(sound), volume, pitch);
    }

    @Override
    public void playSound(Vector3Dd pos, IPWorld world, PSound sound, float volume, float pitch)
    {
        SpigotUtil.playSound(new Location(Bukkit.getWorld(world.worldName()), pos.x(), pos.y(), pos.z()),
                             PSound.getSoundName(sound), volume, pitch);
    }

    @Override
    public void playSound(double x, double y, double z, IPWorld world, PSound sound, float volume, float pitch)
    {
        SpigotUtil
            .playSound(new Location(Bukkit.getWorld(world.worldName()), x, y, z), PSound.getSoundName(sound), volume,
                       pitch);
    }
}
