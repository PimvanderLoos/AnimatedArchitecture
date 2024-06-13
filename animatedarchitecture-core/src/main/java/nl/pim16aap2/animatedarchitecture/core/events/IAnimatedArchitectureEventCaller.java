package nl.pim16aap2.animatedarchitecture.core.events;

/**
 * Represents an object that can call AnimatedArchitecture events.
 */
public interface IAnimatedArchitectureEventCaller
{
    /**
     * Calls a {@link IAnimatedArchitectureEvent}.
     *
     * @param animatedArchitectureEvent
     *     The {@link IAnimatedArchitectureEvent} to call.
     */
    <T extends IAnimatedArchitectureEvent> void callAnimatedArchitectureEvent(T animatedArchitectureEvent);
}
