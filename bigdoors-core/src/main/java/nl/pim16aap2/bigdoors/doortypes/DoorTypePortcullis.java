package nl.pim16aap2.bigdoors.doortypes;

import nl.pim16aap2.bigdoors.doors.Portcullis;
import nl.pim16aap2.bigdoors.util.Constants;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DoorTypePortcullis extends DoorType
{
    private static final int TYPE_VERSION = 1;
    private static final List<Parameter> PARAMETERS;

    static
    {
        List<Parameter> parameterTMP = new ArrayList<>(3);
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "blocksToMove"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "autoCloseTimer"));
        parameterTMP.add(new Parameter(ParameterType.INTEGER, "autoOpenTimer"));
        PARAMETERS = Collections.unmodifiableList(parameterTMP);
    }

    @NotNull
    private static final DoorTypePortcullis instance = new DoorTypePortcullis();

    private DoorTypePortcullis()
    {
        super(Constants.PLUGINNAME, "Portcullis", TYPE_VERSION, PARAMETERS, Portcullis::constructor,
              Portcullis::dataSupplier);
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    @NotNull
    public static DoorTypePortcullis get()
    {
        return instance;
    }

}
