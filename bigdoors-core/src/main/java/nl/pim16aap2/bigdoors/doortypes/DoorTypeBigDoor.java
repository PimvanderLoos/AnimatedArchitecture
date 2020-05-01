package nl.pim16aap2.bigdoors.doortypes;

import nl.pim16aap2.bigdoors.doors.BigDoor;
import nl.pim16aap2.bigdoors.util.Constants;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class DoorTypeBigDoor extends DoorType
{
    private static final int TYPE_VERSION = 1;
    private static final List<Parameter> PARAMETERS = new ArrayList<>(2);

    static
    {
        PARAMETERS.add(new Parameter(ParameterType.INTEGER, "autoCloseTimer"));
        PARAMETERS.add(new Parameter(ParameterType.INTEGER, "currentDirection"));
    }

    @NotNull
    private static final DoorTypeBigDoor instance = new DoorTypeBigDoor();

    private DoorTypeBigDoor()
    {
        super(Constants.PLUGINNAME, "BigDoor", TYPE_VERSION, PARAMETERS, BigDoor::constructDoor, BigDoor::dataSupplier);
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    @NotNull
    public static DoorTypeBigDoor get()
    {
        return instance;
    }

}
