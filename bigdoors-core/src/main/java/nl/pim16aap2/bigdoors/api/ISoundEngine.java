package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.util.vector.IVector3DdConst;
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import org.jetbrains.annotations.NotNull;

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
    void playSound(final @NotNull IPLocation loc, final @NotNull PSound sound, final float volume, final float pitch);

    /**
     * Play a sound for all players in a range of 15 blocks around the provided location.
     *
     * @param pos    The position of the sound.
     * @param world  The world of the position.
     * @param sound  The sound to play.
     * @param volume The volume
     * @param pitch  The pitch
     */
    void playSound(final @NotNull IVector3DiConst pos, final @NotNull IPWorld world, final @NotNull PSound sound,
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
    void playSound(final @NotNull IVector3DdConst pos, final @NotNull IPWorld world, final @NotNull PSound sound,
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
    void playSound(final double x, final double y, final double z, final @NotNull IPWorld world,
                   final @NotNull PSound sound, final float volume, final float pitch);
}
