package nl.pim16aap2.animatedarchitecture.spigot.core.propertyAdapter;

import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.StaticGuiElement;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.experimental.ExtensionMethod;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.spigot.core.gui.GuiUtil;
import org.bukkit.Material;

/**
 * Property adapter for {@link Property#BLOCKS_TO_MOVE}.
 */
@Singleton
@CustomLog
@ExtensionMethod(CompletableFutureExtensions.class)
@EqualsAndHashCode(callSuper = true)
public final class PropertyAdapterBlocksToMove extends AbstractStaticPropertyAdapter<Integer>
{
    public static final Property<Integer> PROPERTY = Property.BLOCKS_TO_MOVE;
    public static final Material MATERIAL = Material.STICKY_PISTON;

    private final CommandFactory commandFactory;
    private final StructureRetrieverFactory structureRetrieverFactory;

    @Inject
    PropertyAdapterBlocksToMove(
        CommandFactory commandFactory,
        StructureRetrieverFactory structureRetrieverFactory
    )
    {
        super(
            PROPERTY,
            MATERIAL,
            "gui.property.blocks_to_move.title",
            "gui.property.blocks_to_move.lore.editable",
            "gui.property.blocks_to_move.lore.readonly"
        );

        this.commandFactory = commandFactory;
        this.structureRetrieverFactory = structureRetrieverFactory;
    }

    @Override
    protected GuiElement.Action getAction(PropertyGuiRequest<Integer> request, StaticGuiElement element)
    {
        return click ->
        {
            commandFactory
                .getSetBlocksToMoveDelayed()
                .runDelayed(request.player(), structureRetrieverFactory.of(request.structure()))
                .handleExceptional(ex -> handleExceptional(ex, request.player(), "blocks_to_move_button"));
            GuiUtil.closeAllGuis(request.player());
            return true;
        };
    }
}
