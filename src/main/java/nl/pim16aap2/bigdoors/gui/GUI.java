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
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.util.Messages;
import nl.pim16aap2.bigdoors.util.PageType;

public class GUI
{
    public static final Material   PAGESWITCHMAT  = Material.ARROW;
    public static final Material   CURRDOORMAT    = Material.BOOK;
    public static final Material   CHANGETIMEMAT  = Material.CLOCK;
    public static final Material   NEWDOORMAT     = Material.WRITABLE_BOOK;
    public static final Material   LOCKDOORMAT    = Material.GREEN_STAINED_GLASS_PANE;
    public static final Material   UNLOCKDOORMAT  = Material.RED_STAINED_GLASS_PANE;
    public static final Material   CONFIRMMAT     = Material.GREEN_STAINED_GLASS_PANE;
    public static final Material   NOTCONFIRMMAT  = Material.RED_STAINED_GLASS_PANE;
    public static final Material   TOGGLEDOORMAT  = Material.LEVER;
    public static final Material   INFOMAT        = Material.BOOKSHELF;
    public static final Material   DELDOORMAT     = Material.BARRIER;
    public static final Material   RELOCATEPBMAT  = Material.LEATHER_BOOTS;
    public static final Material   SETOPENDIRMAT  = Material.COMPASS;
    public static final Material   SETBTMOVEMAT   = Material.STICKY_PISTON;
    public static final Material   ADDOWNERMAT    = Material.PLAYER_HEAD;
    public static final Material   REMOVEOWNERMAT = Material.SKELETON_SKULL;
    public static final int        CHESTSIZE      = 45;
    public static final Material[] DOORTYPES      =
    {
        Material.OAK_DOOR,     // Door
        Material.OAK_TRAPDOOR, // DrawBridge
        Material.IRON_DOOR,    // Portcullis
        Material.OAK_BOAT,     // Elevator
        Material.PISTON,       // Sliding Door
        Material.PURPLE_CARPET // Flag
    };

    private IGUIPage guiPage;

    private boolean isRefreshing;
    private boolean isOpen;

    private final BigDoors plugin;
    private final Player player;

    private Messages messages;
    private int page;
    private final ArrayList<Door> doors;
    private int doorOwnerPage = 0;
    private SortType sortType = SortType.ID;
    private Inventory inventory = null;
    private final HashMap<Integer, GUIItem> items;
    private int maxPageCount;
    private Door door = null;

    public GUI(final BigDoors plugin, final Player player)
    {
        isRefreshing = false;
        isOpen = true;
        this.plugin = plugin;
        this.player = player;
        messages = plugin.getMessages();

        page = 0;
        items = new HashMap<>();

        doors = plugin.getDatabaseManager().getDoors(player.getUniqueId().toString(), null);
        sort();
        guiPage = new GUIPageDoorList(plugin, this);
        update();
    }

    public void update()
    {
        isRefreshing = true;
        items.clear();
        maxPageCount = doors.size() / (CHESTSIZE - 9) + ((doors.size() % (CHESTSIZE - 9)) == 0 ? 0 : 1);
        guiPage.refresh();

        inventory = Bukkit.createInventory(player, CHESTSIZE, messages.getString(PageType.getMessage(guiPage.getPageType())));
        player.openInventory(inventory);
        items.forEach((k, v) -> inventory.setItem(k, v.getItemStack()));

        isRefreshing = false;
    }

    public boolean isStillOwner()
    {
        if (door != null && plugin.getDatabaseManager().getPermission(player.getUniqueId().toString(), door.getDoorUID()) == -1)
        {
            doors.remove(door);
            door = null;
            setGUIPage(new GUIPageDoorList(plugin, this));
            return false;
        }
        return true;
    }

    public void setGUIPage(IGUIPage guiPage)
    {
        this.guiPage = guiPage;
        update();
    }

    public void handleInput(int interactionIDX)
    {
        if (items.containsKey(interactionIDX))
            guiPage.handleInput(interactionIDX);
    }

    public void sort()
    {
        Collections.sort(doors, SortType.getComparator(sortType));
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

    public void setRefreshing(boolean value)
    {
        isRefreshing = value;
    }

    public void resetRefreshing()
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

    public Door getDoor()
    {
        return door;
    }

    public void setDoor(Door door)
    {
        this.door = door;
    }

    public int getPage()
    {
        return page;
    }

    public void setPage(int page)
    {
        this.page = page;
    }

    public int getDoorOwnerPage()
    {
        return doorOwnerPage;
    }

    public int getMaxPageCount()
    {
        return maxPageCount;
    }

    public Door getDoor(int index)
    {
        return doors.get(index);
    }

    public void removeSelectedDoor()
    {
        doors.remove(door);
        door = null;
    }

    public int indexOfDoor(Door door)
    {
        return doors.indexOf(door);
    }

    public int getDoorsSize()
    {
        return doors.size();
    }

    public void addItem(int index, GUIItem guiItem)
    {
        items.put(index, guiItem);
    }

    public GUIItem getItem(int index)
    {
        return items.get(index);
    }

    public Inventory getInventory()
    {
        return inventory;
    }

    public void setSortType(SortType sortType)
    {
        this.sortType = sortType;
        sort();
    }

    public void setNextSortType()
    {
        setSortType(sortType.next());
    }

    public SortType getSortType()
    {
        return sortType;
    }

    protected static enum SortType
    {
        ID   ("GUI.SORT.Numerically",    Comparator.comparing(Door::getDoorUID)),
        NAME ("GUI.SORT.Alphabetically", Comparator.comparing(Door::getName)),
        TYPE ("GUI.SORT.Typically",      Comparator.comparing(Door::getType))
        {
            @Override
            public SortType next()
            {
                return values()[0];
            }
        };

        private String name;
        private Comparator<Door> comparator;

        SortType(String name, Comparator<Door> comparator)
        {
            this.name = name;
            this.comparator = comparator;
        }

        public static String getName(SortType sortType)
        {
            return sortType.name;
        }

        public static Comparator<Door> getComparator(SortType sortType)
        {
            return sortType.comparator;
        }

        public SortType next()
        {
            return values()[ordinal() + 1];
        }
    }
}
