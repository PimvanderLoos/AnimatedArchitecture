package nl.pim16aap2.bigdoors.spigotutil;

/**
 * Represent the possible outcomes of trying to open a door.
 *
 * @author Pim
 */
public enum DoorOpenResult
{
    SUCCESS       (""),
    BUSY          ("GENERAL.DoorIsBusy"),
    LOCKED        ("GENERAL.DoorIsLocked"),
    ERROR         ("GENERAL.ToggleFailure"),
    NOPERMISSION  ("GENERAL.NoPermissionInNewLocation"),
    NODIRECTION   ("GENERAL.CannotFindOpenDirection"),
    ALREADYOPEN   ("GENERAL.DoorAlreadyOpen"),
    ALREADYCLOSED ("GENERAL.DoorAlreadyClosed"),
    TYPEDISABLED  ("GENERAL.DoorTypeDisabled");

    private String message;

    DoorOpenResult(String message)
    {
        this.message = message;
    }

    /**
     * Get the Key for the translation of this {@link DoorOpenResult}.
     * @param result The {@link DoorOpenResult}.
     * @return The Key for the translation of this {@link DoorOpenResult}.
     */
    public static String getMessage(DoorOpenResult result)
    {
        return result.message;
    }
}
