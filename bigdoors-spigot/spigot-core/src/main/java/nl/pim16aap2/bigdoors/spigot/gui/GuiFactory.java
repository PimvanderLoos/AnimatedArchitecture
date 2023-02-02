package nl.pim16aap2.bigdoors.spigot.gui;

import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.IGuiFactory;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.structureretriever.StructureRetrieverFactory;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Objects;

/**
 * The implementation of {@link IGuiFactory} for the Spigot platform.
 */
public class GuiFactory implements IGuiFactory
{
    private final MainGui.IFactory factory;
    private final StructureRetrieverFactory structureRetrieverFactory;
    private final IPExecutor executor;

    @Inject //
    GuiFactory(MainGui.IFactory factory, StructureRetrieverFactory structureRetrieverFactory, IPExecutor executor)
    {
        this.factory = factory;
        this.structureRetrieverFactory = structureRetrieverFactory;
        this.executor = executor;
    }

    @Override
    public void newGUI(IPPlayer inventoryHolder, @Nullable IPPlayer source)
    {
        final IPPlayer finalSource = Objects.requireNonNullElse(source, inventoryHolder);
        structureRetrieverFactory
            .search(finalSource, "", StructureRetrieverFactory.StructureFinderMode.NEW_INSTANCE)
            .getStructures()
            .thenApply(doors -> executor.runOnMainThread(() -> factory.newGUI(inventoryHolder, doors)))
            .exceptionally(Util::exceptionally);
    }
}
