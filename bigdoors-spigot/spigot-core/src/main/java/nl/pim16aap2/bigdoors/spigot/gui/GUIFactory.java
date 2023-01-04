package nl.pim16aap2.bigdoors.spigot.gui;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.IGUIFactory;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Objects;

public class GUIFactory implements IGUIFactory
{
    private final GUI.IFactory factory;
    private final GUIData.IFactory guiDataFactory;
    private final DoorRetrieverFactory doorRetrieverFactory;

    @Inject //
    GUIFactory(GUI.IFactory factory, GUIData.IFactory guiDataFactory, DoorRetrieverFactory doorRetrieverFactory)
    {
        this.factory = factory;
        this.guiDataFactory = guiDataFactory;
        this.doorRetrieverFactory = doorRetrieverFactory;
    }

    @Override
    public void newGUI(IPPlayer inventoryHolder, @Nullable IPPlayer source)
    {
        final IPPlayer finalSource = Objects.requireNonNullElse(source, inventoryHolder);
        doorRetrieverFactory.search(finalSource, "", DoorRetrieverFactory.DoorFinderMode.NEW_INSTANCE)
                            .getDoors()
                            .thenApply(doorList -> guiDataFactory.newGUIData(finalSource, doorList))
                            .thenApply(guiData -> factory.newGUI(inventoryHolder, guiData))
                            .exceptionally(Util::exceptionally);
    }
}
