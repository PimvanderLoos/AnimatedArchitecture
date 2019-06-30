package nl.pim16aap2.bigdoors.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.spigotutil.PageType;
import nl.pim16aap2.bigdoors.util.Messages;

public class GUI
{
    static final Material   PAGESWITCHMAT  = Material.ARROW;
    static final Material   CURRDOORMAT    = Material.BOOK;
    static final Material   CHANGETIMEMAT  = Material.CLOCK;
    static final Material   NEWDOORMAT     = Material.WRITABLE_BOOK;
    static final Material   LOCKDOORMAT    = Material.GREEN_STAINED_GLASS_PANE;
    static final Material   UNLOCKDOORMAT  = Material.RED_STAINED_GLASS_PANE;
    static final Material   CONFIRMMAT     = Material.GREEN_STAINED_GLASS_PANE;
    static final Material   NOTCONFIRMMAT  = Material.RED_STAINED_GLASS_PANE;
    static final Material   TOGGLEDOORMAT  = Material.LEVER;
    static final Material   INFOMAT        = Material.BOOKSHELF;
    static final Material   DELDOORMAT     = Material.BARRIER;
    static final Material   RELOCATEPBMAT  = Material.LEATHER_BOOTS;
    static final Material   SETOPENDIRMAT  = Material.COMPASS;
    static final Material   SETBTMOVEMAT   = Material.STICKY_PISTON;
    static final Material   ADDOWNERMAT    = Material.PLAYER_HEAD;
    static final Material   REMOVEOWNERMAT = Material.SKELETON_SKULL;
    static final int        CHESTSIZE      = 45;
    static final Material[] DOORTYPES      =
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
        Material.BARRIER,      // UNUSED
        Material.BARRIER,      // UNUSED
    };

    private IGUIPage guiPage;

    private boolean isRefreshing;
    private boolean isOpen;

    private final BigDoors plugin;
    private final Player player;

    private Messages messages;
    private int page;
    private final ArrayList<DoorBase> doorBases;
    private int doorOwnerPage = 0;
    private SortType sortType = SortType.ID;
    private Inventory inventory = null;
    private final HashMap<Integer, GUIItem> items;
    private int maxPageCount;
    private DoorBase door = null;

    public GUI(final BigDoors plugin, final Player player)
    {
        isRefreshing = false;
        isOpen = true;
        this.plugin = plugin;
        this.player = player;
        messages = plugin.getMessages();

        page = 0;
        items = new HashMap<>();

        doorBases = plugin.getDatabaseManager().getDoors(player.getUniqueId(), null);
        sort();
        guiPage = new GUIPageDoorList(plugin, this);
        update();
    }

    void updateItem(int index, GUIItem guiItem)
    {
        items.put(index, guiItem);
        getInventory().setItem(index, guiItem.getItemStack());
    }

    void update()
    {
        isRefreshing = true;
        items.clear();
        maxPageCount = doorBases.size() / (CHESTSIZE - 9) + ((doorBases.size() % (CHESTSIZE - 9)) == 0 ? 0 : 1);
        guiPage.refresh();

        inventory = Bukkit.createInventory(player, CHESTSIZE, messages.getString(PageType.getMessage(guiPage.getPageType())));
        player.openInventory(inventory);
        items.forEach((k, v) -> inventory.setItem(k, v.getItemStack()));

        isRefreshing = false;
    }

    boolean isStillOwner()
    {
        if (door != null && plugin.getDatabaseManager().getPermission(player.getUniqueId().toString(), door.getDoorUID()) == -1)
        {
            doorBases.remove(door);
            door = null;
            setGUIPage(new GUIPageDoorList(plugin, this));
            return false;
        }
        return true;
    }

    void setGUIPage(IGUIPage guiPage)
    {
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
        player.closeInventory();
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

    public Player getPlayer()
    {
        return player;
    }

    DoorBase getDoor()
    {
        return door;
    }

    void setDoor(DoorBase door)
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

    DoorBase getDoor(int index)
    {
        return doorBases.get(index);
    }

    void removeSelectedDoor()
    {
        doorBases.remove(door);
        door = null;
    }

    int indexOfDoor(DoorBase door)
    {
        return doorBases.indexOf(door);
    }

    int getDoorsSize()
    {
        return doorBases.size();
    }

    void addItem(int index, GUIItem guiItem)
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

    void setSortType(SortType sortType)
    {
        this.sortType = sortType;
        sort();
    }

    void setNextSortType()
    {
        setSortType(sortType.next());
    }

    SortType getSortType()
    {
        return sortType;
    }

    protected static enum SortType
    {
        ID   ("GUI.SORT.Numerically",    Comparator.comparing(DoorBase::getDoorUID)),
        NAME ("GUI.SORT.Alphabetically", Comparator.comparing(DoorBase::getName)),
        TYPE ("GUI.SORT.Typically",      Comparator.comparing(DoorBase::getType))
        {
            @Override
            SortType next()
            {
                return values()[0];
            }
        };

        private String name;
        private Comparator<DoorBase> comparator;

        SortType(String name, Comparator<DoorBase> comparator)
        {
            this.name = name;
            this.comparator = comparator;
        }

        static String getName(SortType sortType)
        {
            return sortType.name;
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
