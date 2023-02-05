package nl.pim16aap2.bigdoors.core.moveblocks;

public final class AnimationUtil
{
    private AnimationUtil()
    {
    }

    /**
     * Converts the animation time into a number of animation ticks.
     *
     * @param animationDuration
     *     The duration of the animation in seconds.
     * @param serverTickTime
     *     The duration of a server tick in ms.
     * @return The number of ticks in the animation.
     */
    public static int getAnimationTicks(double animationDuration, int serverTickTime)
    {
        return (int) Math.min(Integer.MAX_VALUE, Math.round(1000 * animationDuration / serverTickTime));
    }
}
