package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

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
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.spigot.core.AnimatedArchitecturePlugin;
import nl.pim16aap2.animatedarchitecture.spigot.core.config.ConfigSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.PlayerSpigot;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

@ToString(onlyExplicitlyIncluded = true)
class MainGui implements IGuiPage.IGuiStructureDeletionListener
{
    private static final ItemStack FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);

    private final AnimatedArchitecturePlugin animatedArchitecturePlugin;
    private final ILocalizer localizer;
    private final InfoGui.IFactory infoGuiFactory;
    private final CreateStructureGui.IFactory createStructureGuiFactory;
    private final ITextFactory textFactory;
    private final GuiStructureDeletionManager deletionManager;
    private final IExecutor executor;
    private InventoryGui inventoryGui;
    private @Nullable AbstractStructure selectedStructure;

    @ToString.Include
    private final Long2ObjectMap<AbstractStructure> structures;

    private final ConfigSpigot config;

    @Getter
    @ToString.Include
    private final PlayerSpigot inventoryHolder;

    @AssistedInject//
    MainGui(
        AnimatedArchitecturePlugin animatedArchitecturePlugin,
        ILocalizer localizer,
        ITextFactory textFactory,
        InfoGui.IFactory infoGuiFactory,
        CreateStructureGui.IFactory createStructureGuiFactory,
        GuiStructureDeletionManager deletionManager,
        IExecutor executor,
        ConfigSpigot config,
        @Assisted IPlayer inventoryHolder,
        @Assisted List<AbstractStructure> structures)
    {
        this.textFactory = textFactory;
        this.createStructureGuiFactory = createStructureGuiFactory;
        this.deletionManager = deletionManager;
        this.executor = executor;
        this.animatedArchitecturePlugin = animatedArchitecturePlugin;
        this.localizer = localizer;
        this.infoGuiFactory = infoGuiFactory;
        this.config = config;
        this.inventoryHolder = Util.requireNonNull(SpigotAdapter.getPlayerSpigot(inventoryHolder), "InventoryHolder");
        this.structures = getStructuresMap(structures);

        this.inventoryGui = createGUI();

        showGUI();

        deletionManager.registerDeletionListener(this);
    }

    private static Long2ObjectMap<AbstractStructure> getStructuresMap(List<AbstractStructure> structures)
    {
        final Long2ObjectMap<AbstractStructure> ret = new Long2ObjectOpenHashMap<>(structures.size());
        structures.stream().sorted(Comparator.comparing(AbstractStructure::getName))
                  .forEach(structure -> ret.put(structure.getUid(), structure));
        return ret;
    }

    private void showGUI()
    {
        inventoryGui.show(inventoryHolder.getBukkitPlayer());
    }

    private InventoryGui createGUI()
    {
        final String[] guiSetup = GuiUtil.fillLinesWithChar('g', structures.size(), "fp  h  nl");

        final InventoryGui gui =
            new InventoryGui(animatedArchitecturePlugin,
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
        for (final AbstractStructure structure : structures.values())
        {
            final StaticGuiElement guiElement = new StaticGuiElement(
                'e',
                new ItemStack(config.getGuiMaterial(structure.getType())),
                click ->
                {
                    selectedStructure = structure;
                    final InfoGui infoGui = infoGuiFactory.newInfoGUI(structure, inventoryHolder);
                    infoGui.getInventoryGui().setCloseAction(
                        close ->
                        {
                            this.selectedStructure = null;
                            return true;
                        });
                    return true;
                },
                structure.getNameAndUid());
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

        gui.addElement(new StaticGuiElement(
            'h',
            new ItemStack(Material.WRITABLE_BOOK),
            click ->
            {
                createStructureGuiFactory.newCreateStructureGui(inventoryHolder);
                return true;
            },
            localizer.getMessage("gui.new_structure_page.header")));

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
     * Removes a structure from the set of visible structures.
     * <p>
     * Calling this method will result in the player being notified of the removal. If this is undesired, use
     * {@link #onStructureDeletion(IStructureConst, boolean)} instead.
     *
     * @param structure
     *     The structure that was deleted.
     */
    @Override
    public void onStructureDeletion(IStructureConst structure)
    {
        onStructureDeletion(structure, true);
    }

    /**
     * Removes a structure from the set of visible structures.
     *
     * @param structure
     *     The structure that was deleted.
     * @param notify
     *     Whether to notify the inventory holder that the structure was deleted.
     */
    public void onStructureDeletion(IStructureConst structure, boolean notify)
    {
        executor.runOnMainThread(() -> onStructureDeletion0(structure, notify));
    }

    private void onStructureDeletion0(IStructureConst structure, boolean notify)
    {
        //noinspection ConstantValue
        if (structures.remove(structure.getUid()) != null)
        {
            if (selectedStructure == null || selectedStructure.getUid() == structure.getUid())
                this.redraw();
            if (notify)
                inventoryHolder.sendMessage(textFactory.newText().append(
                    localizer.getMessage("gui.notification.structure_inaccessible"), TextType.ERROR,
                    arg -> arg.highlight(structure.getName())));
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
         * @param structures
         *     The structures to show in the GUI.
         */
        MainGui newGUI(IPlayer inventoryHolder, List<AbstractStructure> structures);
    }
}
