package nl.pim16aap2.animatedarchitecture.core.structures;

/**
 * Represents the different redstone modes a structure can have.
 * <p>
 * The redstone mode determines how the structure reacts to redstone signals. There are three different modes:
 * <ul>
 *     <li>{@link #IGNORE}: The structure ignores redstone signals.</li>
 *     <li>not-yet-named: The structure behaves in a way where powered = open; unpowered = closed.</li>
 *     <li>not-yet-named: The structure behaves in a way where it toggles between open and closed when the redstone signal changes.</li>
 *     </ul>
 */
public enum RedstoneMode
{
    /**
     * The structure ignores all redstone signals.
     */
    IGNORE,

    /**
     * The structure behaves in a way where powered = open; unpowered = closed.
     */
    POWERED_OPEN,

    /**
     * The structure behaves in a way where it toggles between open and closed when the redstone signal changes.
     */
    TOGGLE,
    ;

    /**
     * The default redstone mode.
     */
    public static final RedstoneMode DEFAULT = POWERED_OPEN;
}
