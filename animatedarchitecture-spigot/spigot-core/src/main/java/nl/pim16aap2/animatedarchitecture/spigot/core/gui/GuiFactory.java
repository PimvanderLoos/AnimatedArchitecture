package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IGuiFactory;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
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
    private final IExecutor executor;

    @Inject //
    GuiFactory(MainGui.IFactory factory, StructureRetrieverFactory structureRetrieverFactory, IExecutor executor)
    {
        this.factory = factory;
        this.structureRetrieverFactory = structureRetrieverFactory;
        this.executor = executor;
    }

    @Override
    public void newGUI(IPlayer inventoryHolder, @Nullable IPlayer source)
    {
        final IPlayer finalSource = Objects.requireNonNullElse(source, inventoryHolder);
        structureRetrieverFactory
            .search(finalSource, "", StructureRetrieverFactory.StructureFinderMode.NEW_INSTANCE, PermissionLevel.USER)
            .getStructures()
            .thenApply(structures -> structures.parallelStream().map(MainGui.NamedStructure::new).toList())
            .thenCompose(structures -> executor.runOnMainThread(() -> factory.newGUI(inventoryHolder, structures)))
            .exceptionally(Util::exceptionally);
    }
}
