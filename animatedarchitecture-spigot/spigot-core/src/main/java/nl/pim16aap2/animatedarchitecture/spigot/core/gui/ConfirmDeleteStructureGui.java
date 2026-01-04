package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.CustomLog;
import lombok.ToString;
import lombok.experimental.ExtensionMethod;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.spigot.core.AnimatedArchitecturePlugin;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WrappedPlayer;

@CustomLog
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@ExtensionMethod(CompletableFutureExtensions.class)
class ConfirmDeleteStructureGui extends AbstractConfirmDeleteGui<Structure, ConfirmDeleteStructureGui>
{
    private final StructureRetrieverFactory structureRetrieverFactory;

    @AssistedInject
    ConfirmDeleteStructureGui(
        @Assisted Structure itemToDelete,
        @Assisted WrappedPlayer inventoryHolder,
        AnimatedArchitecturePlugin animatedArchitecturePlugin,
        CommandFactory commandFactory,
        IExecutor executor,
        StructureRetrieverFactory structureRetrieverFactory
    )
    {
        super(animatedArchitecturePlugin, commandFactory, executor, itemToDelete, inventoryHolder);
        this.structureRetrieverFactory = structureRetrieverFactory;
    }

    private Structure getStructure()
    {
        return itemToDelete;
    }

    private String getLocalizedStructureTypeName()
    {
        return localizer.getMessage(getStructure().getType().getLocalizationKey());
    }

    @Override
    public String getDeletePageConfirmationTitle()
    {
        return localizer.getMessage(
            "gui.delete_page.structure.title",
            getLocalizedStructureTypeName(),
            getStructure().getNameAndUid()
        );
    }

    @Override
    protected String getDeleteItemConfirmationTitle()
    {
        return localizer.getMessage(
            "gui.delete_page.structure.confirm",
            getLocalizedStructureTypeName());
    }

    @Override
    protected String getDeleteItemCancelTitle()
    {
        return localizer.getMessage(
            "gui.delete_page.structure.cancel",
            getLocalizedStructureTypeName()
        );
    }

    @Override
    protected void onDeleteConfirmed()
    {
        commandFactory
            .newDelete(getInventoryHolder(), structureRetrieverFactory.of(getStructure()))
            .run()
            .handleExceptional(ex ->
            {
                getInventoryHolder().sendGenericErrorMessage();
                log.atError().withCause(ex).log("Failed to delete structure.");
            });
    }

    /**
     * The factory interface for creating {@link ConfirmDeleteStructureGui} instances.
     */
    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates and opens a new {@link ConfirmDeleteStructureGui} for the given structure and player.
         *
         * @param structure
         *     The structure whose deletion is being confirmed.
         * @param playerSpigot
         *     The player who is deleting the structure.
         * @return The created {@link ConfirmDeleteStructureGui} instance.
         */
        @SuppressWarnings("NullableProblems")
        ConfirmDeleteStructureGui newDeleteStructureGui(Structure structure, WrappedPlayer playerSpigot);
    }
}
