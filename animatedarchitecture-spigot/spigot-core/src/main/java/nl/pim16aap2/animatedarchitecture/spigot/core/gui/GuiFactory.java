package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import lombok.experimental.ExtensionMethod;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IGuiFactory;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * The implementation of {@link IGuiFactory} for the Spigot platform.
 */
@Flogger
@ExtensionMethod(CompletableFutureExtensions.class)
public class GuiFactory implements IGuiFactory
{
    private final MainGui.IFactory factory;
    private final ILocalizer localizer;
    private final ITextFactory textFactory;
    private final StructureRetrieverFactory structureRetrieverFactory;
    private final IExecutor executor;

    @Inject
    GuiFactory(
        MainGui.IFactory factory,
        ILocalizer localizer,
        ITextFactory textFactory,
        StructureRetrieverFactory structureRetrieverFactory,
        IExecutor executor
    )
    {
        this.factory = factory;
        this.localizer = localizer;
        this.textFactory = textFactory;
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
            .orTimeout(5, TimeUnit.SECONDS)
            .handleExceptional(ex ->
            {
                log.atSevere().withCause(ex).log(
                    "Failed to create new GUI for player %s from source %s",
                    inventoryHolder,
                    finalSource
                );
                inventoryHolder.sendError(textFactory, localizer.getMessage("constants.error.generic"));
            });
    }
}
