package nl.pim16aap2.bigDoors.util;

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

    private DoorOpenResult(String message)
    {
        this.message = message;
    }

    public static String getMessage(DoorOpenResult result)
    {
        return result.message;
    }
}
