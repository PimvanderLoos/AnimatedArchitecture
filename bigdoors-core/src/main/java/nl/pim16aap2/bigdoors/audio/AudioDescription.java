package nl.pim16aap2.bigdoors.audio;

/**
 * Describes a sound, with its pitch and volume etc.
 *
 * @param sound
 *     The name of the sound to play.
 * @param volume
 *     The volume at which to play the sound.
 * @param pitch
 *     The pitch of the sound.
 * @param duration
 *     The duration of the sound.
 * @author Pim
 */
public record AudioDescription(String sound, float volume, float pitch, int duration)
{
}
