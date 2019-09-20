package nl.pim16aap2.bigdoors.util;

/**
 * Represents a list of constant variables.
 *
 * @author Pim
 */
public final class Constants
{
    /**
     * The name
     */
    public static final String BIGDOORSENTITYNAME = "BigDoorsEntity";

    /**
     * Whether or not the current build is a dev build. Certain options are enabled, disabled, or overridden depending
     * on this variable.
     */
    public static final boolean DEVBUILD = true;

    /**
     * Minimum number of ticks a door needs to cool down before it can be toggled again. This should help with some rare
     * cases of overlapping processes and whatnot.
     */
    public static final int MINIMUMDOORDELAY = 10;

    /**
     * The permission node that allows a player to bypass all compatibility hooks.
     */
    public static final String COMPATBYPASSPERMISSION = "bigdoors.admin.bypasscompat";
}
