package nl.pim16aap2.bigdoors.spigot.gui;

import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.IGuiFactory;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Objects;

/**
 * The implementation of {@link IGuiFactory} for the Spigot platform.
 */
public class GuiFactory implements IGuiFactory
{
    private final MainGui.IFactory factory;
    private final MovableRetrieverFactory movableRetrieverFactory;
    private final IPExecutor executor;

    @Inject //
    GuiFactory(MainGui.IFactory factory, MovableRetrieverFactory movableRetrieverFactory, IPExecutor executor)
    {
        this.factory = factory;
        this.movableRetrieverFactory = movableRetrieverFactory;
        this.executor = executor;
    }

    @Override
    public void newGUI(IPPlayer inventoryHolder, @Nullable IPPlayer source)
    {
        final IPPlayer finalSource = Objects.requireNonNullElse(source, inventoryHolder);
        movableRetrieverFactory
            .search(finalSource, "", MovableRetrieverFactory.MovableFinderMode.NEW_INSTANCE)
            .getMovables()
            .thenApply(doors -> executor.runOnMainThread(() -> factory.newGUI(inventoryHolder, doors)))
            .exceptionally(Util::exceptionally);
    }
}
