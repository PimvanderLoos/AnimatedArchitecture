package nl.pim16aap2.bigdoors.audio;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

/**
 * Represents an object that can play sounds.
 *
 * @author Pim
 */
public interface IAudioPlayer
{
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
    void playSound(IPLocation loc, String sound, float volume, float pitch);

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
    void playSound(Vector3Di pos, IPWorld world, String sound, float volume, float pitch);

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
    void playSound(Vector3Dd pos, IPWorld world, String sound, float volume, float pitch);

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
    void playSound(double x, double y, double z, IPWorld world, String sound, float volume, float pitch);
}
