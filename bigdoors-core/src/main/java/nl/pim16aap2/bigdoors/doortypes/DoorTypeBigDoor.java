package nl.pim16aap2.bigdoors.doortypes;

import nl.pim16aap2.bigdoors.doors.BigDoor;
import nl.pim16aap2.bigdoors.util.Constants;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DoorTypeBigDoor extends DoorType
{
    private static final int TYPE_VERSION = 1;
    private static final List<Parameter> PARAMETERS;

    static
    {
        List<Parameter> parameterTMP = new ArrayList<>(3);
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "autoCloseTimer"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "autoOpenTimer"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "currentDirection"));
        PARAMETERS = Collections.unmodifiableList(parameterTMP);
    }

    @NotNull
    private static final DoorTypeBigDoor instance = new DoorTypeBigDoor();

    private DoorTypeBigDoor()
    {
        super(Constants.PLUGINNAME, "BigDoor", TYPE_VERSION, PARAMETERS, BigDoor::constructor, BigDoor::dataSupplier);
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
