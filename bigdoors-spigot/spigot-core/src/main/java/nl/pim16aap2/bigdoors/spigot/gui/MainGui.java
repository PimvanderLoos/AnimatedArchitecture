package nl.pim16aap2.bigdoors.spigot.gui;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.IMovableConst;
import nl.pim16aap2.bigdoors.spigot.BigDoorsPlugin;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PPlayerSpigot;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

@ToString(onlyExplicitlyIncluded = true)
class MainGui implements IGuiPage.IGuiMovableDeletionListener
{
    private static final ItemStack FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);

    private final BigDoorsPlugin bigDoorsPlugin;
    private final ILocalizer localizer;
    private final InfoGui.IFactory infoGuiFactory;
    private final ITextFactory textFactory;
    private final GuiMovableDeletionManager deletionManager;
    private final IPExecutor executor;
    private InventoryGui inventoryGui;
    private @Nullable AbstractMovable selectedMovable;

    @ToString.Include
    private final Long2ObjectMap<AbstractMovable> movables;

    @Getter
    @ToString.Include
    private final PPlayerSpigot inventoryHolder;

    @AssistedInject//
    MainGui(
        BigDoorsPlugin bigDoorsPlugin, ILocalizer localizer, ITextFactory textFactory, InfoGui.IFactory infoGuiFactory,
        GuiMovableDeletionManager deletionManager, IPExecutor executor, @Assisted IPPlayer inventoryHolder,
        @Assisted List<AbstractMovable> movables)
    {
        this.textFactory = textFactory;
        this.deletionManager = deletionManager;
        this.executor = executor;
        this.bigDoorsPlugin = bigDoorsPlugin;
        this.localizer = localizer;
        this.infoGuiFactory = infoGuiFactory;
        this.inventoryHolder = Util.requireNonNull(SpigotAdapter.getPPlayerSpigot(inventoryHolder), "InventoryHolder");
        this.movables = getMovablesMap(movables);

        this.inventoryGui = createGUI();

        showGUI();

        deletionManager.registerDeletionListener(this);
    }

    private static Long2ObjectMap<AbstractMovable> getMovablesMap(List<AbstractMovable> movables)
    {
        final Long2ObjectMap<AbstractMovable> ret = new Long2ObjectOpenHashMap<>(movables.size());
        movables.stream().sorted(Comparator.comparing(AbstractMovable::getName))
                .forEach(movable -> ret.put(movable.getUid(), movable));
        return ret;
    }

    private void showGUI()
    {
        inventoryGui.show(inventoryHolder.getBukkitPlayer());
    }

    private InventoryGui createGUI()
    {
        final String[] guiSetup = GuiUtil.fillLinesWithChar('g', movables.size(), "fp     nl");

        final InventoryGui gui =
            new InventoryGui(bigDoorsPlugin,
                             inventoryHolder.getBukkitPlayer(),
                             localizer.getMessage("gui.main_page.title"),
                             guiSetup);
        gui.setCloseAction(GuiUtil.getDeletionListenerUnregisterCloseAction(deletionManager, this));
        gui.setFiller(FILLER);

        populateGUI(gui);

        return gui;
    }

    private void populateGUI(InventoryGui gui)
    {
        addElementGroup(gui);
        addHeader(gui);
    }

    private void addElementGroup(InventoryGui gui)
    {
        final GuiElementGroup group = new GuiElementGroup('g');
        for (final AbstractMovable movable : movables.values())
        {
            final StaticGuiElement guiElement = new StaticGuiElement(
                'e',
                new ItemStack(Material.OAK_DOOR),
                click ->
                {
                    selectedMovable = movable;
                    final InfoGui infoGui = infoGuiFactory.newInfoGUI(movable, inventoryHolder);
                    infoGui.getInventoryGui().setCloseAction(close ->
                                                             {
                                                                 this.selectedMovable = null;
                                                                 return true;
                                                             });
                    return true;
                },
                movable.getNameAndUid());
            group.addElement(guiElement);
        }
        group.setFiller(FILLER);
        gui.addElement(group);
    }

    private void addHeader(InventoryGui gui)
    {
        gui.addElement(new GuiPageElement(
            'f', new ItemStack(Material.ARROW), GuiPageElement.PageAction.FIRST,
            localizer.getMessage("gui.main_page.nav.first_page")));

        //noinspection SpellCheckingInspection
        gui.addElement(new GuiPageElement(
            'p', new ItemStack(Material.BIRCH_SIGN), GuiPageElement.PageAction.PREVIOUS,
            localizer.getMessage("gui.main_page.nav.previous_page", "%prevpage%", "%pages%")));

        //noinspection SpellCheckingInspection
        gui.addElement(new GuiPageElement(
            'n', new ItemStack(Material.BIRCH_SIGN), GuiPageElement.PageAction.NEXT,
            localizer.getMessage("gui.main_page.nav.next_page", "%nextpage%", "%pages%")));

        gui.addElement(new GuiPageElement(
            'l', new ItemStack(Material.ARROW), GuiPageElement.PageAction.LAST,
            localizer.getMessage("gui.main_page.nav.last_page")));
    }

    /**
     * Redraws the main GUI.
     * <p>
     * Note that any GUIs that are already open should be closed first.
     */
    private void redraw()
    {
        executor.assertMainThread();
        inventoryGui.setCloseAction(null);
        GuiUtil.closeAllGuis(inventoryHolder);
        inventoryGui = createGUI();
        showGUI();
    }

    /**
     * Removes a movable from the set of visible movables.
     * <p>
     * Calling this method will result in the player being notified of the removal. If this is undesired, use
     * {@link #onMovableDeletion(IMovableConst, boolean)} instead.
     *
     * @param movable
     *     The movable that was deleted.
     */
    @Override
    public void onMovableDeletion(IMovableConst movable)
    {
        onMovableDeletion(movable, true);
    }

    /**
     * Removes a movable from the set of visible movables.
     *
     * @param movable
     *     The movable that was deleted.
     * @param notify
     *     Whether to notify the inventory holder that the movable was deleted.
     */
    public void onMovableDeletion(IMovableConst movable, boolean notify)
    {
        executor.runOnMainThread(() -> onMovableDeletion0(movable, notify));
    }

    private void onMovableDeletion0(IMovableConst movable, boolean notify)
    {
        //noinspection ConstantValue
        if (movables.remove(movable.getUid()) != null)
        {
            if (selectedMovable == null || selectedMovable.getUid() == movable.getUid())
                this.redraw();
            if (notify)
                inventoryHolder.sendInfo(textFactory, localizer.getMessage("gui.notification.movable_inaccessible",
                                                                           movable.getNameAndUid()));
        }
    }

    @Override
    public String getPageName()
    {
        return "MainGui";
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates a new GUI.
         *
         * @param inventoryHolder
         *     The player for whom to create the GUI.
         * @param movables
         *     The movables to show in the GUI.
         */
        MainGui newGUI(IPPlayer inventoryHolder, List<AbstractMovable> movables);
    }
}
