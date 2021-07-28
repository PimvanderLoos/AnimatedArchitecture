package nl.pim16aap2.bigdoors.spigot.util.implementations;

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
import org.jetbrains.annotations.NotNull;

/**
 * Represents an implementation of {@link ISoundEngine} for the Spigot platform.
 *
 * @author Pim
 */
public class PSoundEngineSpigot implements ISoundEngine
{
    @Override
    public void playSound(final @NotNull IPLocation loc, final @NotNull PSound sound, final float volume,
                          final float pitch)
    {
        BigDoors.get().getPlatform().getPExecutor().runAsync(() -> SpigotUtil
            .playSound(SpigotAdapter.getBukkitLocation(loc), PSound.getSoundName(sound), volume, pitch));
    }

    @Override
    public void playSound(final @NotNull Vector3Di pos, final @NotNull IPWorld world, final @NotNull PSound sound,
                          final float volume, final float pitch)
    {
        SpigotUtil.playSound(new Location(Bukkit.getWorld(world.worldName()), pos.x(), pos.y(), pos.z()),
                             PSound.getSoundName(sound), volume, pitch);
    }

    @Override
    public void playSound(final @NotNull Vector3Dd pos, final @NotNull IPWorld world, final @NotNull PSound sound,
                          final float volume, final float pitch)
    {
        SpigotUtil.playSound(new Location(Bukkit.getWorld(world.worldName()), pos.x(), pos.y(), pos.z()),
                             PSound.getSoundName(sound), volume, pitch);
    }

    @Override
    public void playSound(double x, double y, double z, final @NotNull IPWorld world, final @NotNull PSound sound,
                          final float volume, final float pitch)
    {
        SpigotUtil
            .playSound(new Location(Bukkit.getWorld(world.worldName()), x, y, z), PSound.getSoundName(sound), volume,
                       pitch);
    }
}
