package nl.pim16aap2.bigdoors.api;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.util.vector.Vector3DdConst;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;

/**
 * Represents an object that can play sounds.
 *
 * @author Pim
 */
public interface ISoundEngine
{
    /**
     * Play a sound for all players in a range of 15 blocks around the provided location.
     *
     * @param loc    The location of the sound.
     * @param sound  The sound to play.
     * @param volume The volume
     * @param pitch  The pitch
     */
    void playSound(final @NonNull IPLocationConst loc, final @NonNull PSound sound, final float volume,
                   final float pitch);

    /**
     * Play a sound for all players in a range of 15 blocks around the provided location.
     *
     * @param pos    The position of the sound.
     * @param world  The world of the position.
     * @param sound  The sound to play.
     * @param volume The volume
     * @param pitch  The pitch
     */
    void playSound(final @NonNull Vector3DiConst pos, final @NonNull IPWorld world, final @NonNull PSound sound,
                   final float volume, final float pitch);

    /**
     * Play a sound for all players in a range of 15 blocks around the provided location.
     *
     * @param pos    The position of the sound.
     * @param world  The world of the position.
     * @param sound  The sound to play.
     * @param volume The volume
     * @param pitch  The pitch
     */
    void playSound(final @NonNull Vector3DdConst pos, final @NonNull IPWorld world, final @NonNull PSound sound,
                   final float volume, final float pitch);

    /**
     * Play a sound for all players in a range of 15 blocks around the provided location.
     *
     * @param x      The x coordinate of the location.
     * @param y      The y coordinate of the location.
     * @param z      The z coordinate of the location.
     * @param world  The world of the position.
     * @param sound  The sound to play.
     * @param volume The volume
     * @param pitch  The pitch
     */
    void playSound(final double x, final double y, final double z, final @NonNull IPWorld world,
                   final @NonNull PSound sound, final float volume, final float pitch);
}
