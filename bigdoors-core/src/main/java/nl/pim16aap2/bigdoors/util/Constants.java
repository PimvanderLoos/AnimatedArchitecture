package nl.pim16aap2.bigdoors.util;


import lombok.NonNull;

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
    public static final @NonNull String BIGDOORSENTITYNAME = "BigDoorsEntity";

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
    public static final @NonNull String COMPATBYPASSPERMISSION = "bigdoors.admin.bypasscompat";

    /**
     * The name of this plugin.
     */
    public static final @NonNull String PLUGINNAME = "BigDoors";

    /**
     * The directory where all the extensions are loaded from.
     */
    public static final @NonNull String BIGDOORS_EXTENSIONS_FOLDER = "/Extensions";
}
