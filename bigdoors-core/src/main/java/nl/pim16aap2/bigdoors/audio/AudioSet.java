package nl.pim16aap2.bigdoors.audio;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a set of audio descriptions that together define the set of audio descriptions to be used in an
 * animation.
 *
 * @param activeAudio
 *     The audio to use while the animation is active. Taking a door as an example, this could be sound of the scraping
 *     of the bottom part of the door along the floor.
 * @param endAudio
 *     The audio to use at the end of the animation when it is shutting down. Looking at a door again, this could
 *     describe the sound of the door slamming into the door frame.
 */
public record AudioSet(@Nullable AudioDescription activeAudio, @Nullable AudioDescription endAudio)
{
    /**
     * @return True if all audio descriptions in this set are empty.
     */
    boolean isEmpty()
    {
        return activeAudio == null && endAudio == null;
    }
}
