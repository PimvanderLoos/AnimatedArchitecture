package nl.pim16aap2.animatedarchitecture.spigot.core.propertyAdapter;

import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiStateElement;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IPropertyValue;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WrappedPlayer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.animatedarchitecture.core.util.Constants.DEFAULT_COMMAND_TIMEOUT_SECONDS;

/**
 * Represents a property adapter for {@link Property#OPEN_STATUS}.
 */
@Singleton
@CustomLog
@ExtensionMethod(CompletableFutureExtensions.class)
public final class PropertyAdapterOpenStatus extends AbstractBooleanPropertyAdapter
{
    public static final Property<Boolean> PROPERTY = Property.OPEN_STATUS;

    public static final Material MATERIAL_OPEN = Material.WARPED_DOOR;
    public static final Material MATERIAL_CLOSED = Material.MANGROVE_DOOR;

    public static final String TITLE_KEY = "gui.property.open_status.title";
    public static final String LORE_EDITABLE_KEY = "gui.property.open_status.lore.editable";
    public static final String LORE_READONLY_KEY = "gui.property.open_status.lore.readonly";

    private final StructureRetrieverFactory structureRetrieverFactory;
    private final CommandFactory commandFactory;
    private final IExecutor executor;

    @Inject
    public PropertyAdapterOpenStatus(
        StructureRetrieverFactory structureRetrieverFactory,
        CommandFactory commandFactory,
        IExecutor executor
    )
    {
        super(PROPERTY);
        this.structureRetrieverFactory = structureRetrieverFactory;
        this.commandFactory = commandFactory;
        this.executor = executor;
    }

    private boolean isOpen(IPropertyValue<Boolean> propertyValue)
    {
        return Boolean.TRUE.equals(propertyValue.value());
    }

    @Override
    protected Material getMaterial(PropertyGuiRequest<Boolean> request)
    {
        return isOpen(request.propertyValue()) ? MATERIAL_OPEN : MATERIAL_CLOSED;
    }

    @Override
    protected String getTitle(PropertyGuiRequest<Boolean> request)
    {
        return request.localizer().getMessage(TITLE_KEY);
    }

    @Override
    protected List<String> getLore(PropertyGuiRequest<Boolean> request)
    {
        final ILocalizer localizer = request.localizer();

        final String statusKey = isOpen(request.propertyValue()) ?
            "constants.open_status.open" :
            "constants.open_status.closed";
        final String localizedStatus = localizer.getMessage(statusKey);

        final String loreKey = canEdit(request.permissionLevel())
            ? LORE_EDITABLE_KEY
            : LORE_READONLY_KEY;

        return localizer.getMessage(loreKey, localizedStatus)
            .lines()
            .toList();
    }

    @Override
    protected GuiStateElement.State getTrueState(String stateKey, PropertyGuiRequest<Boolean> request)
    {
        final ILocalizer localizer = request.localizer();
        return new GuiStateElement.State(
            change -> isOpenButtonExecute(true, change, request),
            stateKey,
            new ItemStack(MATERIAL_OPEN),
            localizer.getMessage(
                "gui.info_page.attribute.set_open",
                localizer.getMessage(request.structure().getType().getLocalizationKey()))
        );
    }

    @Override
    protected GuiStateElement.State getFalseState(String stateKey, PropertyGuiRequest<Boolean> request)
    {
        final ILocalizer localizer = request.localizer();
        return new GuiStateElement.State(
            change -> isOpenButtonExecute(false, change, request),
            stateKey,
            new ItemStack(MATERIAL_CLOSED),
            localizer.getMessage(
                "gui.info_page.attribute.set_closed",
                localizer.getMessage(request.structure().getType().getLocalizationKey()))
        );
    }

    private void isOpenButtonExecute(
        boolean newState,
        GuiElement.Click change,
        PropertyGuiRequest<Boolean> request
    )
    {
        commandFactory
            .newSetOpenStatus(request.player(), structureRetrieverFactory.of(request.structure()), newState)
            .runWithRawResult(DEFAULT_COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // Force a draw with dynamic fields update to ensure the correct
            // state is displayed in case the command did not change the status.
            .thenRun(() ->
                executor.runOnMainThread(() ->
                    change.getGui()
                        .draw(request.player().getBukkitPlayer(), true, false)))
            .orTimeout(1, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleExceptional(ex, request.player(), "is_open_button"));
    }

    private void handleExceptional(Throwable ex, WrappedPlayer player, String context)
    {
        player.sendError("commands.base.error.generic");
        log.atError().withCause(ex).log("Failed to handle action '%s' for player '%s'", context, player);
    }

    @Override
    protected boolean getState(PropertyGuiRequest<Boolean> request)
    {
        return request.structure().isLocked();
    }
}
