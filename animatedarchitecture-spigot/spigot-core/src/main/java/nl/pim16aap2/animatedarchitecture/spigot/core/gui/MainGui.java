package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
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

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@ToString(onlyExplicitlyIncluded = true)
@NotThreadSafe
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
    private final ConfigSpigot config;

    @Getter
    @ToString.Include
    private final PlayerSpigot inventoryHolder;

    private InventoryGui inventoryGui;

    private @Nullable AbstractStructure selectedStructure;

    @ToString.Include
    private Long2ObjectMap<NamedStructure> structures;

    private SortingMethod sortingMethod = SortingMethod.BY_NAME;

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
        @Assisted List<NamedStructure> structures)
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

    private static Long2ObjectMap<NamedStructure> getStructuresMap(List<NamedStructure> structures)
    {
        final Long2ObjectMap<NamedStructure> ret = new Long2ObjectLinkedOpenHashMap<>(structures.size());
        structures.stream().sorted(Comparator.comparing(NamedStructure::name))
                  .forEach(structure -> ret.put(structure.structure.getUid(), structure));
        return ret;
    }

    private void showGUI()
    {
        inventoryGui.show(inventoryHolder.getBukkitPlayer());
    }

    private InventoryGui createGUI()
    {
        final String[] guiSetup = GuiUtil.fillLinesWithChar('g', structures.size(), "fps h  nl");

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
        for (final NamedStructure structure : structures.values())
        {
            final StaticGuiElement guiElement = new StaticGuiElement(
                'e',
                new ItemStack(config.getGuiMaterial(structure.structure().getType())),
                click ->
                {
                    selectedStructure = structure.structure();
                    final InfoGui infoGui = infoGuiFactory.newInfoGUI(structure.structure(), inventoryHolder);
                    infoGui.getInventoryGui().setCloseAction(
                        close ->
                        {
                            infoGui.getStructure().syncDataAsync();
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

        gui.addElement(new StaticGuiElement(
            's',
            new ItemStack(Material.KNOWLEDGE_BOOK),
            click ->
            {
                sortingMethod = sortingMethod.next();
                structures = sortingMethod.sort(structures);
                click.getGui().removeElement('g');
                addElementGroup(click.getGui());
                ((StaticGuiElement) click.getElement()).setText(
                    localizer.getMessage(sortingMethod.next().getLocalizationKeySort()),
                    localizer.getMessage(sortingMethod.getLocalizationKeySorted()));
                click.getGui().draw(click.getWhoClicked(), true, false);
                return true;
            },
            localizer.getMessage(sortingMethod.next().getLocalizationKeySort()),
            localizer.getMessage(sortingMethod.getLocalizationKeySorted())
        ));
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

    /**
     * Represents a union of a structure and its name.
     * <p>
     * This allows us to quickly draw all structures in a GUI without having to deal with locks and synchronization on
     * the structure.
     *
     * @param structure
     *     The structure.
     * @param name
     *     The name of the structure.
     */
    public record NamedStructure(AbstractStructure structure, String name)
    {
        public NamedStructure(AbstractStructure structure)
        {
            this(structure, structure.getName());
        }

        /**
         * Returns the name and UID of the structure.
         * <p>
         * See {@link IStructureConst#formatNameAndUid(String, long)}.
         *
         * @return The name and UID of the structure in the format {@code "name (uid)"}.
         */
        public String getNameAndUid()
        {
            return IStructureConst.formatNameAndUid(name, structure.getUid());
        }
    }

    private enum SortingMethod
    {
        BY_NAME("gui.main_page.button.sorted_by_name",
                "gui.main_page.button.sort_by_name",
                Comparator.comparing(entry -> entry.getValue().name())),

        BY_TYPE("gui.main_page.button.sorted_by_type",
                "gui.main_page.button.sort_by_type",
                Comparator.comparing(entry -> entry.getValue().structure().getType().getSimpleName())),

        BY_UID("gui.main_page.button.sorted_by_uid",
               "gui.main_page.button.sort_by_uid",
               Comparator.comparingLong(Long2ObjectMap.Entry::getLongKey));

        private static final SortingMethod[] VALUES = SortingMethod.values();

        @Getter
        private final String localizationKeySorted;

        @Getter
        private final String localizationKeySort;

        private final Comparator<Long2ObjectMap.Entry<NamedStructure>> comparator;

        SortingMethod(
            String localizationKeySorted, String localizationKeySort,
            Comparator<Long2ObjectMap.Entry<NamedStructure>> comparator)
        {
            this.localizationKeySorted = localizationKeySorted;
            this.localizationKeySort = localizationKeySort;
            this.comparator = comparator;
        }

        /**
         * Sorts the given structures map according to the sorting method.
         * <p>
         * The sorting method is not applied in-place!
         *
         * @param structures
         *     The structures to sort.
         * @return The sorted structures.
         */
        public Long2ObjectMap<NamedStructure> sort(Long2ObjectMap<NamedStructure> structures)
        {
            return structures.long2ObjectEntrySet().stream().sorted(comparator).collect(
                Collectors.toMap(Long2ObjectMap.Entry::getLongKey,
                                 Long2ObjectMap.Entry::getValue,
                                 (a, b) -> a,
                                 Long2ObjectLinkedOpenHashMap::new));
        }

        /**
         * Returns the next sorting method based on the definition order of the enum values.
         *
         * @return The next sorting method.
         */
        public SortingMethod next()
        {
            return VALUES[(this.ordinal() + 1) % VALUES.length];
        }
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
        MainGui newGUI(IPlayer inventoryHolder, List<NamedStructure> structures);
    }
}
