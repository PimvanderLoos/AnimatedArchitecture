package nl.pim16aap2.animatedarchitecture.spigot.core.propertyadapters.guiadapters;

import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiStateElement;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.experimental.ExtensionMethod;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
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
@EqualsAndHashCode(callSuper = true)
public final class PropertyGuiAdapterOpenStatus extends AbstractBooleanPropertyGuiAdapter
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
    public PropertyGuiAdapterOpenStatus(
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

    private boolean isOpen(PropertyGuiRequest<Boolean> request)
    {
        return Boolean.TRUE.equals(getPropertyValue(request));
    }

    @Override
    protected String getTitle(PropertyGuiRequest<Boolean> request)
    {
        return request.localizer().getMessage(TITLE_KEY);
    }

    @Override
    protected List<String> getLore(boolean isOpen, PropertyGuiRequest<Boolean> request)
    {
        final ILocalizer localizer = request.localizer();

        final String statusKey = isOpen ?
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

    private ItemStack createItemStack(boolean isOpen, PropertyGuiRequest<Boolean> request)
    {
        final Material material = getMaterial(isOpen);
        final String title = getTitle(request);
        final List<String> lore = getLore(isOpen, request);

        return createItemStack(material, title, lore);
    }

    @Override
    protected GuiStateElement.State getTrueState(String stateKey, PropertyGuiRequest<Boolean> request)
    {
        return new GuiStateElement.State(
            change -> isOpenButtonExecute(true, change, request),
            stateKey,
            createItemStack(true, request)
        );
    }

    @Override
    protected GuiStateElement.State getFalseState(String stateKey, PropertyGuiRequest<Boolean> request)
    {
        return new GuiStateElement.State(
            change -> isOpenButtonExecute(false, change, request),
            stateKey,
            createItemStack(false, request)
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
            .thenRun(() -> executor.runOnMainThread(() ->
                change.getGui()
                    .draw(request.player().getBukkitPlayer(), true, false)))
            .orTimeout(5, TimeUnit.SECONDS)
            .withExceptionContext(
                "Player %s setting open status to %b for structure %s",
                request.player(),
                newState,
                request.structure().getNameAndUid())
            .handleExceptional(ex -> handleExceptional(ex, request.player(), "is_open_button"));
    }

    @Override
    protected boolean getState(PropertyGuiRequest<Boolean> request)
    {
        return isOpen(request);
    }

    @Override
    protected Material getMaterial(boolean isOpen)
    {
        return isOpen ? MATERIAL_OPEN : MATERIAL_CLOSED;
    }
}
