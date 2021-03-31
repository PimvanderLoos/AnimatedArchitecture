package nl.pim16aap2.bigdoors.spigot.gui;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.util.PageType;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class GUI
{
    static final Material PAGESWITCHMAT = Material.ARROW;
    static final Material CURRDOORMAT = Material.BOOK;
    static final Material CHANGETIMEMAT = Material.CLOCK;
    static final Material NEWDOORMAT = Material.WRITABLE_BOOK;
    static final Material LOCKDOORMAT = Material.GREEN_STAINED_GLASS_PANE;
    static final Material UNLOCKDOORMAT = Material.RED_STAINED_GLASS_PANE;
    static final Material CONFIRMMAT = Material.GREEN_STAINED_GLASS_PANE;
    static final Material NOTCONFIRMMAT = Material.RED_STAINED_GLASS_PANE;
    static final Material TOGGLEDOORMAT = Material.LEVER;
    static final Material INFOMAT = Material.BOOKSHELF;
    static final Material DELDOORMAT = Material.BARRIER;
    static final Material RELOCATEPBMAT = Material.LEATHER_BOOTS;
    static final Material SETOPENDIRMAT = Material.COMPASS;
    static final Material SETBTMOVEMAT = Material.STICKY_PISTON;
    static final Material ADDOWNERMAT = Material.PLAYER_HEAD;
    static final Material REMOVEOWNERMAT = Material.SKELETON_SKULL;
    static final int CHESTSIZE = 45;
    static final Material[] DOORTYPES =
        {
            Material.OAK_DOOR,     // Door
            Material.OAK_TRAPDOOR, // DrawBridge
            Material.OAK_FENCE,    // Portcullis
            Material.OAK_BOAT,     // Elevator
            Material.PISTON,       // Sliding Door
            Material.BLUE_BANNER,  // Flag
            Material.MINECART,     // Garage door
            Material.ELYTRA,       // Windmill
            Material.END_CRYSTAL,  // Revolving door
            Material.CLOCK,        // Clock
            Material.BARRIER,      // UNUSED
        };
    private final BigDoorsSpigot plugin;
    private final IPPlayer guiHolder;
    private List<AbstractDoorBase> doorBases;
    private final Map<Integer, GUIItem> items;
    private IGUIPage guiPage;
    private boolean isRefreshing;
    private boolean isOpen;
    private Messages messages;
    private int page;
    private int doorOwnerPage = 0;
    private SortType sortType = SortType.ID;
    private Inventory inventory = null;
    private int maxPageCount;
    private AbstractDoorBase door = null;

    public GUI(final @NotNull BigDoorsSpigot plugin, final @NotNull IPPlayer guiHolder)
    {
        isRefreshing = false;
        isOpen = true;
        this.plugin = plugin;
        this.guiHolder = guiHolder;
        messages = plugin.getMessages();

        page = 0;
        items = new ConcurrentHashMap<>();
        BigDoors.get().getDatabaseManager().getDoors(guiHolder.getUUID()).whenComplete(
            (doorList, throwable) ->
            {
                doorBases = new ArrayList<>(doorList.size());
                doorList.forEach(
                    doorEntry ->
                    {
                        if (BigDoors.get().getDoorTypeManager().isDoorTypeEnabled(doorEntry.getDoorType()))
                            doorBases.add(doorEntry);
                    });
                sort();
                guiPage = new GUIPageDoorList(plugin, this);
                BigDoors.get().getPlatform().getPExecutor().runOnMainThread(this::update);
            });
    }

    void updateItem(int index, @NotNull Optional<GUIItem> guiItem)
    {
        guiItem.ifPresent(I -> setItem(index, I));
    }

    void setItem(int index, @NotNull GUIItem guiItem)
    {
        items.put(index, guiItem);
        getInventory().setItem(index, guiItem.getItemStack());
    }

    void update()
    {
        if (!BigDoors.get().getPlatform().isMainThread(Thread.currentThread().getId()))
            plugin.getPLogger().logThrowable(new IllegalStateException("NOT ON THE MAIN THREAD!"));
        isRefreshing = true;
        items.clear();
        maxPageCount = doorBases.size() / (CHESTSIZE - 9) + ((doorBases.size() % (CHESTSIZE - 9)) == 0 ? 0 : 1);
        guiPage.refresh();

        Player bukkitPlayer = SpigotAdapter.getBukkitPlayer(guiHolder);
        if (bukkitPlayer == null)
        {
            BigDoors.get().getPLogger()
                    .logThrowable(new NullPointerException("Player \"" + guiHolder.toString() + "\" is null!"));
            return;
        }

        inventory = Bukkit
            .createInventory(bukkitPlayer, CHESTSIZE,
                             messages.getString(PageType.getMessage(guiPage.getPageType())));
        bukkitPlayer.openInventory(inventory);
        items.forEach((k, v) -> inventory.setItem(k, v.getItemStack()));

        isRefreshing = false;
    }

    boolean isStillOwner()
    {
        if (door == null)
            return false;

        if (door.getDoorOwner(guiHolder).isPresent())
            return true;

        doorBases.remove(door);
        door = null;
        setGUIPage(new GUIPageDoorList(plugin, this));
        return false;
    }

    void setGUIPage(final @NotNull IGUIPage guiPage)
    {
        guiPage.kill();
        this.guiPage = guiPage;
        update();
    }

    public void handleInput(int interactionIDX)
    {
        if (items.containsKey(interactionIDX))
            guiPage.handleInput(interactionIDX);
    }

    private void sort()
    {
        doorBases.sort(SortType.getComparator(sortType));
    }

    public void close()
    {
        isOpen = false;
        SpigotAdapter.getBukkitPlayer(guiHolder).closeInventory();
        plugin.removeGUIUser(this);
    }

    public boolean isRefreshing()
    {
        return isRefreshing;
    }

    void setRefreshing(boolean value)
    {
        isRefreshing = value;
    }

    void resetRefreshing()
    {
        isRefreshing = false;
    }

    public boolean isOpen()
    {
        return isOpen;
    }

    public IPPlayer getGuiHolder()
    {
        return guiHolder;
    }

    AbstractDoorBase getDoor()
    {
        return door;
    }

    void setDoor(final @NotNull AbstractDoorBase door)
    {
        this.door = door;
    }

    int getPage()
    {
        return page;
    }

    void setPage(int page)
    {
        this.page = page;
    }

    int getDoorOwnerPage()
    {
        return doorOwnerPage;
    }

    int getMaxPageCount()
    {
        return maxPageCount;
    }

    // TODO: OPTIONAL!
    AbstractDoorBase getDoor(final int index)
    {
        return doorBases.get(index);
    }

    void removeSelectedDoor()
    {
        doorBases.remove(door);
        door = null;
    }

    int indexOfDoor(final @NotNull AbstractDoorBase door)
    {
        return doorBases.indexOf(door);
    }

    int getDoorsSize()
    {
        return doorBases.size();
    }

    void addItem(final int index, final @NotNull GUIItem guiItem)
    {
        items.put(index, guiItem);
    }

    GUIItem getItem(int index)
    {
        return items.get(index);
    }

    Inventory getInventory()
    {
        return inventory;
    }

    void setNextSortType()
    {
        setSortType(sortType.next());
    }

    SortType getSortType()
    {
        return sortType;
    }

    void setSortType(final @NotNull SortType sortType)
    {
        this.sortType = sortType;
        sort();
    }

    protected static enum SortType
    {
        ID(Message.GUI_SORTING_NUMERICAL, Comparator.comparing(AbstractDoorBase::getDoorUID)),
        NAME(Message.GUI_SORTING_ALPHABETICAL, Comparator.comparing(AbstractDoorBase::getName)),
        TYPE(Message.GUI_SORTING_TYPICAL, Comparator.comparing(abstractDoorBase ->
                                                                   abstractDoorBase.getDoorType().getSimpleName()))
            {
                @Override
                SortType next()
                {
                    return values()[0];
                }
            };

        private Message message;
        private Comparator<AbstractDoorBase> comparator;

        SortType(Message message, Comparator<AbstractDoorBase> comparator)
        {
            this.message = message;
            this.comparator = comparator;
        }

        static Message getMessage(SortType sortType)
        {
            return sortType.message;
        }

        static Comparator<AbstractDoorBase> getComparator(SortType sortType)
        {
            return sortType.comparator;
        }

        SortType next()
        {
            return values()[ordinal() + 1];
        }
    }
}
