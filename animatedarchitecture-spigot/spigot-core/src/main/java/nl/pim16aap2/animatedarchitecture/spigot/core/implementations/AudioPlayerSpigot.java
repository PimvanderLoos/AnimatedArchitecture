package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.audio.IAudioPlayer;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
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
    /**
     * Half the range of the audio in blocks in the x, y, and z direction.
     */
    private static final int AUDIO_RANGE = 15;

    private final IExecutor executor;

    @Inject
    public AudioPlayerSpigot(IExecutor executor)
    {
        this.executor = executor;
    }

    @Override
    public void playSound(ILocation loc, String sound, float volume, float pitch)
    {
        playSound(SpigotAdapter.getBukkitLocation(loc), sound, volume, pitch);
    }

    @Override
    public void playSound(Vector3Di pos, IWorld world, String sound, float volume, float pitch)
    {
        playSound(new Location(Bukkit.getWorld(world.worldName()), pos.x(), pos.y(), pos.z()), sound, volume, pitch);
    }

    @Override
    public void playSound(Vector3Dd pos, IWorld world, String sound, float volume, float pitch)
    {
        playSound(new Location(Bukkit.getWorld(world.worldName()), pos.x(), pos.y(), pos.z()), sound, volume, pitch);
    }

    @Override
    public void playSound(double x, double y, double z, IWorld world, String sound, float volume, float pitch)
    {
        playSound(new Location(Bukkit.getWorld(world.worldName()), x, y, z), sound, volume, pitch);
    }

    /**
     * Play a sound for all players in a range of {@link #AUDIO_RANGE} blocks around the provided location.
     */
    private void playSound(Location loc, String sound, float volume, float pitch)
    {
        final @Nullable World world = loc.getWorld();
        if (world == null)
            return;

        executor.runOnMainThread(() -> playSound(loc, world, sound, volume, pitch, AUDIO_RANGE));
    }

    private void playSound(Location loc, World world, String sound, float volume, float pitch, int range)
    {
        for (final Entity ent : world.getNearbyEntities(loc, range, range, range))
            if (ent instanceof Player player)
                player.playSound(loc, sound, SoundCategory.BLOCKS, volume, pitch);
    }
}
