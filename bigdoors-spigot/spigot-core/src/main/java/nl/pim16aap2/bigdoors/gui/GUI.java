package nl.pim16aap2.bigdoors.gui;

import nl.pim16aap2.bigdoors.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.spigotutil.PageType;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

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
    private final Player guiHolder;
    private List<DoorBase> doorBases;
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
    private DoorBase door = null;

    public GUI(final @NotNull BigDoorsSpigot plugin, final @NotNull Player guiHolder)
    {
        isRefreshing = false;
        isOpen = true;
        this.plugin = plugin;
        this.guiHolder = guiHolder;
        messages = plugin.getMessages();

        page = 0;
        items = new ConcurrentHashMap<>();
        plugin.getDatabaseManager().getDoors(guiHolder.getUniqueId()).whenComplete(
            (optionalDoorList, throwable) ->
            {
                doorBases = optionalDoorList.orElse(new ArrayList<>());
                sort();
                guiPage = new GUIPageDoorList(plugin, this);
                BigDoorsSpigot.newMainThreadExecutor().runOnMainThread(this::update);
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
        if (!BigDoorsSpigot.onMainThread(Thread.currentThread().getId()))
            plugin.getPLogger().logException(new IllegalStateException("NOT ON THE MAIN THREAD!"));
        isRefreshing = true;
        items.clear();
        maxPageCount = doorBases.size() / (CHESTSIZE - 9) + ((doorBases.size() % (CHESTSIZE - 9)) == 0 ? 0 : 1);
        guiPage.refresh();

        inventory = Bukkit
            .createInventory(guiHolder, CHESTSIZE, messages.getString(PageType.getMessage(guiPage.getPageType())));
        guiHolder.openInventory(inventory);
        items.forEach((k, v) -> inventory.setItem(k, v.getItemStack()));

        isRefreshing = false;
    }

    // TODO: ASYNC
    boolean isStillOwner()
    {
        try
        {
            if (door != null &&
                plugin.getDatabaseManager().getPermission(guiHolder.getUniqueId().toString(), door.getDoorUID())
                      .get() == -1)
            {
                doorBases.remove(door);
                door = null;
                setGUIPage(new GUIPageDoorList(plugin, this));
                return false;
            }
        }
        catch (InterruptedException | ExecutionException e)
        {
            plugin.getPLogger().logException(e);
            return false;
        }
        return true;
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

    void sort()
    {
        Collections.sort(doorBases, SortType.getComparator(sortType));
    }

    public void close()
    {
        isOpen = false;
        guiHolder.closeInventory();
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

    public Player getGuiHolder()
    {
        return guiHolder;
    }

    DoorBase getDoor()
    {
        return door;
    }

    void setDoor(final @NotNull DoorBase door)
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
    DoorBase getDoor(final int index)
    {
        return doorBases.get(index);
    }

    void removeSelectedDoor()
    {
        doorBases.remove(door);
        door = null;
    }

    int indexOfDoor(final @NotNull DoorBase door)
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
        ID(Message.GUI_SORTING_NUMERICAL, Comparator.comparing(DoorBase::getDoorUID)),
        NAME(Message.GUI_SORTING_ALPHABETICAL, Comparator.comparing(DoorBase::getName)),
        TYPE(Message.GUI_SORTING_TYPICAL, Comparator.comparing(DoorBase::getType))
            {
                @Override
                SortType next()
                {
                    return values()[0];
                }
            };

        private Message message;
        private Comparator<DoorBase> comparator;

        SortType(Message message, Comparator<DoorBase> comparator)
        {
            this.message = message;
            this.comparator = comparator;
        }

        static Message getMessage(SortType sortType)
        {
            return sortType.message;
        }

        static Comparator<DoorBase> getComparator(SortType sortType)
        {
            return sortType.comparator;
        }

        SortType next()
        {
            return values()[ordinal() + 1];
        }
    }
}
