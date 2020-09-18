package nl.pim16aap2.bigdoors.spigot.util.implementations;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.ISoundEngine;
import nl.pim16aap2.bigdoors.api.PSound;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.util.vector.Vector3DdConst;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
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
    public void playSound(final @NotNull IPLocationConst loc, final @NotNull PSound sound, final float volume,
                          final float pitch)
    {
        BigDoors.get().getPlatform().newPExecutor().runAsync(() -> SpigotUtil
            .playSound(SpigotAdapter.getBukkitLocation(loc), PSound.getSoundName(sound), volume, pitch));
    }

    @Override
    public void playSound(final @NotNull Vector3DiConst pos, final @NotNull IPWorld world, final @NotNull PSound sound,
                          final float volume, final float pitch)
    {
        SpigotUtil.playSound(new Location(Bukkit.getWorld(world.getWorldName()), pos.getX(), pos.getY(), pos.getZ()),
                             PSound.getSoundName(sound), volume, pitch);
    }

    @Override
    public void playSound(final @NotNull Vector3DdConst pos, final @NotNull IPWorld world, final @NotNull PSound sound,
                          final float volume, final float pitch)
    {
        SpigotUtil.playSound(new Location(Bukkit.getWorld(world.getWorldName()), pos.getX(), pos.getY(), pos.getZ()),
                             PSound.getSoundName(sound), volume, pitch);
    }

    @Override
    public void playSound(double x, double y, double z, final @NotNull IPWorld world, final @NotNull PSound sound,
                          final float volume, final float pitch)
    {
        SpigotUtil
            .playSound(new Location(Bukkit.getWorld(world.getWorldName()), x, y, z), PSound.getSoundName(sound), volume,
                       pitch);
    }
}
