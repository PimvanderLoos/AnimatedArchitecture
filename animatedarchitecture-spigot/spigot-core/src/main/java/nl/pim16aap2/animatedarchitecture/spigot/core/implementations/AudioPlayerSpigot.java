package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.audio.IAudioPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.function.DoubleUnaryOperator;

/**
 * Represents an implementation of {@link IAudioPlayer} for the Spigot platform.
 */
@Singleton public class AudioPlayerSpigot implements IAudioPlayer
{

    private final IExecutor executor;

    @Inject public AudioPlayerSpigot(IExecutor executor)
    {
        this.executor = executor;
    }

    @Override public void playSound(
        double x,
        double y,
        double z,
        IWorld world,
        String sound,
        float volume,
        float pitch,
        double range,
        @Nullable DoubleUnaryOperator attenuationFunction)
    {
        playSound(
            new Location(Bukkit.getWorld(world.worldName()), x, y, z),
            sound,
            volume,
            pitch,
            range,
            attenuationFunction
        );
    }

    private void playSound(
        Location loc,
        String sound,
        float volume,
        float pitch,
        double range,
        @Nullable DoubleUnaryOperator attenuationFunction)
    {
        final @Nullable World world = loc.getWorld();
        if (world == null)
            return;

        executor.runOnMainThread(() -> playSound(loc, world, sound, volume, pitch, range, attenuationFunction));
    }

    private void playSound(
        Location loc,
        World world,
        String sound,
        float volume,
        float pitch,
        double range,
        @Nullable DoubleUnaryOperator attenuationFunction)
    {
        for (final Entity ent : world.getNearbyEntities(loc, range, range, range, e -> e instanceof Player))
            if (ent instanceof Player player)
                playSound(player, loc, sound, volume, pitch, attenuationFunction);
    }

    private void playSound(
        Player player,
        Location loc,
        String sound,
        float volume,
        float pitch,
        @Nullable DoubleUnaryOperator attenuationFunction)
    {
        final double distance = player.getLocation().distance(loc);
        if (distance > AUDIO_RANGE)
            return;

        float attenuatedVolume = volume;
        if (attenuationFunction != null)
            attenuatedVolume *= (float) attenuationFunction.applyAsDouble(distance);

        player.playSound(loc, sound, attenuatedVolume, pitch);
    }
}
