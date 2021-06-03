package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.api.PSound;
import org.jetbrains.annotations.NotNull;

/**
 * Describes a sound, with its pitch and volume etc.
 */
public class PSoundDescription
{
    private final @NotNull PSound sound;
    private final float volume;
    private final float pitch;

    /**
     * The description of a sound.
     *
     * @param sound  The sound to play.
     * @param volume The volume at which to play the sound.
     * @param pitch  The pitch of the sound.
     */
    public PSoundDescription(final @NotNull PSound sound, final float volume, final float pitch)
    {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    public @NotNull PSound getSound()
    {
        return sound;
    }

    public float getVolume()
    {
        return volume;
    }

    public float getPitch()
    {
        return pitch;
    }
}
