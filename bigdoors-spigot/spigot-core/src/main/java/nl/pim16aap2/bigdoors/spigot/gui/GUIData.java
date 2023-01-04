package nl.pim16aap2.bigdoors.spigot.gui;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class GUIData
{
    private static final int DOORS_PER_PAGE = 9 * 4;

    private final IPPlayer source;

    private final List<AbstractDoor> doorsConst;

    private final List<AbstractDoor> doorsMutable;

    @AssistedInject //
    GUIData(
        @Assisted IPPlayer source, @Assisted List<AbstractDoor> doors)
    {
        this.source = source;
        this.doorsMutable = new ArrayList<>(doors);
        this.doorsConst = Collections.unmodifiableList(this.doorsMutable);
        Util.sortAlphabetically(doorsMutable, AbstractDoor::getName);
    }

    List<AbstractDoor> getDoors()
    {
        return doorsConst;
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * @param source
         *     The {@link IPPlayer} whose doors are used.
         * @param doors
         *     The doors that have been retrieved.
         */
        GUIData newGUIData(IPPlayer source, List<AbstractDoor> doors);
    }
}
