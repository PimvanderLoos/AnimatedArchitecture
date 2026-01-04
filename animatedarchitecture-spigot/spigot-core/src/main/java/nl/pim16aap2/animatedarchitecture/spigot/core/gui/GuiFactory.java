package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import jakarta.inject.Inject;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IGuiFactory;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * The implementation of {@link IGuiFactory} for the Spigot platform.
 */
@CustomLog
@ExtensionMethod(CompletableFutureExtensions.class)
public class GuiFactory implements IGuiFactory
{
    private final MainGui.IFactory mainGuiFactory;
    private final StructureRetrieverFactory structureRetrieverFactory;
    private final IExecutor executor;

    @Inject
    GuiFactory(
        MainGui.IFactory mainGuiFactory,
        StructureRetrieverFactory structureRetrieverFactory,
        IExecutor executor
    )
    {
        this.mainGuiFactory = mainGuiFactory;
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
            .thenCompose(structures -> createAndOpenNewGui(inventoryHolder, structures))
            .orTimeout(5, TimeUnit.SECONDS)
            .handleExceptional(ex ->
            {
                log.atError().withCause(ex).log(
                    "Failed to create new GUI for player %s from source %s",
                    inventoryHolder,
                    finalSource
                );
                inventoryHolder.sendGenericErrorMessage();
            });
    }

    private CompletableFuture<?> createAndOpenNewGui(IPlayer inventoryHolder, List<MainGui.NamedStructure> structures)
    {
        return executor.runOnMainThread(() ->
            mainGuiFactory
                .newMainGui(inventoryHolder, structures)
                .createAndShowGui()
        );
    }
}
