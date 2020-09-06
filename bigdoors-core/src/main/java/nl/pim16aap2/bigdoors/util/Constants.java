package nl.pim16aap2.bigdoors.util;

import org.jetbrains.annotations.NotNull;

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
    @NotNull
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
    @NotNull
    public static final String COMPATBYPASSPERMISSION = "bigdoors.admin.bypasscompat";

    /**
     * The name of this plugin.
     */
    @NotNull
    public static final String PLUGINNAME = "BigDoors";

    /**
     * The directory where all the extensions are loaded from.
     */
    public static final @NotNull String BIGDOORS_EXTENSIONS_FOLDER = "/Extensions";
}
