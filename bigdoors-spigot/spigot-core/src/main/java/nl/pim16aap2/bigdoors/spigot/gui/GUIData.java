package nl.pim16aap2.bigdoors.spigot.gui;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;

import java.util.List;

class GUIData
{
    private final IPPlayer source;
    private final List<AbstractDoor> doors;

    @AssistedInject //
    GUIData(
        @Assisted IPPlayer source, @Assisted List<AbstractDoor> doors)
    {
        this.source = source;
        this.doors = doors;
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
