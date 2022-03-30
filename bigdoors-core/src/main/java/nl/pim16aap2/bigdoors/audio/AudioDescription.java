package nl.pim16aap2.bigdoors.audio;

import nl.pim16aap2.bigdoors.api.PSound;

/**
 * Describes a sound, with its pitch and volume etc.
 *
 * @param sound
 *     The sound to play.
 * @param volume
 *     The volume at which to play the sound.
 * @param pitch
 *     The pitch of the sound.
 * @author Pim
 */
public record AudioDescription(PSound sound, float volume, float pitch)
{}
