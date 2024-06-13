package nl.pim16aap2.animatedarchitecture.core.audio;

import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleUnaryOperator;

/**
 * Represents an object that can play sounds.
 */
@SuppressWarnings("unused")
public interface IAudioPlayer
{
    /**
     * The radius in blocks around the sound source where players can hear the sound.
     */
    double AUDIO_RANGE = 15;

    /**
     * Play a sound for all players in a range of 15 blocks around the provided location.
     *
     * @param loc
     *     The location of the sound.
     * @param sound
     *     The sound to play.
     * @param volume
     *     The volume
     * @param pitch
     *     The pitch
     */
    default void playSound(ILocation loc, String sound, float volume, float pitch)
    {
        playSound(loc.getX(), loc.getY(), loc.getZ(), loc.getWorld(), sound, volume, pitch, AUDIO_RANGE, null);
    }

    /**
     * Play a sound for all players in a range of 15 blocks around the provided location.
     *
     * @param pos
     *     The position of the sound.
     * @param world
     *     The world of the position.
     * @param sound
     *     The sound to play.
     * @param volume
     *     The volume
     * @param pitch
     *     The pitch
     */
    default void playSound(IVector3D pos, IWorld world, String sound, float volume, float pitch)
    {
        playSound(pos.xD(), pos.yD(), pos.zD(), world, sound, volume, pitch, AUDIO_RANGE, null);
    }

    /**
     * Play a sound for all players in a range of 15 blocks around the provided location.
     *
     * @param x
     *     The x coordinate of the location.
     * @param y
     *     The y coordinate of the location.
     * @param z
     *     The z coordinate of the location.
     * @param world
     *     The world of the position.
     * @param sound
     *     The sound to play.
     * @param volume
     *     The volume
     * @param pitch
     *     The pitch
     */
    default void playSound(double x, double y, double z, IWorld world, String sound, float volume, float pitch)
    {
        playSound(x, y, z, world, sound, volume, pitch, AUDIO_RANGE);
    }

    /**
     * Play a sound for all players in a range of 15 blocks around the provided location.
     *
     * @param x
     *     The x coordinate of the location.
     * @param y
     *     The y coordinate of the location.
     * @param z
     *     The z coordinate of the location.
     * @param world
     *     The world of the position.
     * @param sound
     *     The sound to play.
     * @param volume
     *     The volume
     * @param pitch
     *     The pitch
     * @param range
     *     The range of the sound.
     */
    default void playSound(
        double x,
        double y,
        double z,
        IWorld world,
        String sound,
        float volume,
        float pitch,
        double range)
    {
        playSound(x, y, z, world, sound, volume, pitch, range, null);
    }

    /**
     * Play a sound for all players in a range of 15 blocks around the provided location.
     *
     * @param x
     *     The x coordinate of the location.
     * @param y
     *     The y coordinate of the location.
     * @param z
     *     The z coordinate of the location.
     * @param world
     *     The world of the position.
     * @param sound
     *     The sound to play.
     * @param volume
     *     The volume
     * @param pitch
     *     The pitch
     * @param range
     *     The range of the sound.
     * @param attenuationFunction
     *     The function that determines how the volume of the sound changes with distance. If null, the volume will not
     *     change. The function takes the distance as input and returns the multiplier for the volume.
     *     <p>
     *     For example, if the function returns 0.5 for a distance of 10, the volume will be multiplied by 0.5 for all
     *     players that are 10 blocks away from the sound.
     */
    void playSound(
        double x,
        double y,
        double z,
        IWorld world,
        String sound,
        float volume,
        float pitch,
        double range,
        @Nullable DoubleUnaryOperator attenuationFunction
    );
}
