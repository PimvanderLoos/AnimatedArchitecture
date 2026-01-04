package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.ExtensionMethod;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.spigot.core.AnimatedArchitecturePlugin;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WrappedPlayer;

/**
 * Gui page for deleting a property from a structure.
 */
@CustomLog
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@ExtensionMethod(CompletableFutureExtensions.class)
class ConfirmDeletePropertyGui extends AbstractConfirmDeleteGui<Property<?>, ConfirmDeletePropertyGui>
{
    private final Runnable onDeleteExecuted;
    private final StructureRetrieverFactory structureRetrieverFactory;

    @Getter(AccessLevel.PRIVATE)
    private final Structure structure;

    @AssistedInject
    ConfirmDeletePropertyGui(
        @Assisted Property<?> itemToDelete,
        @Assisted WrappedPlayer inventoryHolder,
        @Assisted Structure structure,
        @Assisted Runnable onDeleteExecuted,
        AnimatedArchitecturePlugin animatedArchitecturePlugin,
        CommandFactory commandFactory,
        IExecutor executor,
        StructureRetrieverFactory structureRetrieverFactory
    )
    {
        super(animatedArchitecturePlugin, commandFactory, executor, itemToDelete, inventoryHolder);
        this.onDeleteExecuted = onDeleteExecuted;
        this.structureRetrieverFactory = structureRetrieverFactory;
        this.structure = structure;
    }

    private Property<?> getProperty()
    {
        return itemToDelete;
    }

    @Override
    public String getDeletePageConfirmationTitle()
    {
        return localizer.getMessage(
            "gui.delete_page.property.title",
            getProperty().getTitle(localizer),
            localizer.getMessage(getStructure().getType().getLocalizationKey()),
            getStructure().getNameAndUid()
        );
    }

    @Override
    protected String getDeleteItemConfirmationTitle()
    {
        return localizer.getMessage("gui.delete_page.property.confirm");
    }

    @Override
    protected String getDeleteItemCancelTitle()
    {
        return localizer.getMessage("gui.delete_page.property.cancel");
    }

    @Override
    protected void onDeleteConfirmed()
    {
        commandFactory.newSetProperty(
            inventoryHolder,
            structureRetrieverFactory.of(structure),
            getProperty(),
            null
        );
        onDeleteExecuted.run();
    }

    /**
     * The factory interface for creating {@link ConfirmDeletePropertyGui} instances.
     */
    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates and opens a new {@link ConfirmDeletePropertyGui} for the given property, player, and structure.
         *
         * @param property
         *     The property to delete.
         * @param playerSpigot
         *     The player who is deleting the structure.
         * @param structure
         *     The structure from which the property will be deleted.
         * @param onDeleteExecuted
         *     The callback to run when the deletion has been confirmed and processed.
         * @return The created {@link ConfirmDeletePropertyGui} instance.
         */
        @SuppressWarnings("NullableProblems")
        ConfirmDeletePropertyGui newDeletePropertyGui(
            Property<?> property,
            WrappedPlayer playerSpigot,
            Structure structure,
            Runnable onDeleteExecuted
        );
    }
}
