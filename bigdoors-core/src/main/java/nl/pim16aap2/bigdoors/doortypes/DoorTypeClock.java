package nl.pim16aap2.bigdoors.doortypes;

import nl.pim16aap2.bigdoors.doors.Clock;
import nl.pim16aap2.bigdoors.util.Constants;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DoorTypeClock extends DoorType
{
    private static final int TYPE_VERSION = 1;
    private static final List<Parameter> PARAMETERS;

    static
    {
        List<Parameter> parameterTMP = new ArrayList<>(2);
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "northSouth"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "hourArmSide"));
        PARAMETERS = Collections.unmodifiableList(parameterTMP);
    }

    @NotNull
    private static final DoorTypeClock instance = new DoorTypeClock();

    private DoorTypeClock()
    {
        super(Constants.PLUGINNAME, "Clock", TYPE_VERSION, PARAMETERS, Clock::constructor, Clock::dataSupplier);
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    @NotNull
    public static DoorTypeClock get()
    {
        return instance;
    }

}
