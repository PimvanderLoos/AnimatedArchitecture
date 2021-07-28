package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
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
    void playSound(@NotNull IPLocationConst loc, @NotNull PSound sound, float volume, float pitch);

    /**
     * Play a sound for all players in a range of 15 blocks around the provided location.
     *
     * @param pos    The position of the sound.
     * @param world  The world of the position.
     * @param sound  The sound to play.
     * @param volume The volume
     * @param pitch  The pitch
     */
    void playSound(@NotNull Vector3Di pos, @NotNull IPWorld world, @NotNull PSound sound,
                   float volume, float pitch);

    /**
     * Play a sound for all players in a range of 15 blocks around the provided location.
     *
     * @param pos    The position of the sound.
     * @param world  The world of the position.
     * @param sound  The sound to play.
     * @param volume The volume
     * @param pitch  The pitch
     */
    void playSound(@NotNull Vector3Dd pos, @NotNull IPWorld world, @NotNull PSound sound,
                   float volume, float pitch);

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
    void playSound(double x, double y, double z, @NotNull IPWorld world,
                   @NotNull PSound sound, float volume, float pitch);
}
