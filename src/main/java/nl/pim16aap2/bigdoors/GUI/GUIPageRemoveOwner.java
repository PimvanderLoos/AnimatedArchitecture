package nl.pim16aap2.bigdoors.GUI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.bukkit.scheduler.BukkitRunnable;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.DoorType;
import nl.pim16aap2.bigdoors.util.Messages;
import nl.pim16aap2.bigdoors.util.PageType;

public class GUIPageRemoveOwner implements IGUIPage
{
    protected final BigDoors plugin;
    protected final GUI gui;
    protected final Messages messages;
    protected int missingHeadTextures = 0;
    private int doorOwnerPage = 0;
    private int maxDoorOwnerPageCount = 0;

    private ArrayList<DoorOwner> owners;

    public GUIPageRemoveOwner(final BigDoors plugin, final GUI gui)
    {
        this.plugin = plugin;
        this.gui = gui;
        messages = plugin.getMessages();
        refresh();
    }

    @Override
    public PageType getPageType()
    {
        return PageType.REMOVEOWNER;
    }

    @Override
    public void handleInput(int interactionIDX)
    {
        if (interactionIDX == 0)
        {
            gui.setGUIPage(new GUIPageDoorInfo(plugin, gui));
            return;
        }
        else if (interactionIDX == 1)
        {
            --doorOwnerPage;
            gui.update();
        }
        else if (interactionIDX == 7)
        {
            ++doorOwnerPage;
            gui.update();
        }
        else if (interactionIDX > 8)
        {
            if (!gui.isStillOwner())
                return;
            removeOwner(gui.getItem(interactionIDX).getDoorOwner());
            if (owners.size() == 0)
            {
                gui.setGUIPage(new GUIPageDoorInfo(plugin, gui));
                return;
            }
            gui.update();
        }
    }

    protected void fillHeader()
    {
        ArrayList<String> lore = new ArrayList<>();

        gui.addItem(0, new GUIItem(GUI.PAGESWITCHMAT, messages.getString("GUI.PreviousPage"), lore, gui.getPage() + 1));
        lore.clear();

        if (doorOwnerPage != 0)
        {
            lore.add(messages.getString("GUI.ToPage") + doorOwnerPage + messages.getString("GUI.OutOf") + maxDoorOwnerPageCount);
            gui.addItem(1, new GUIItem(GUI.PAGESWITCHMAT, messages.getString("GUI.PreviousPage"), lore, doorOwnerPage));
            lore.clear();
        }

        if ((doorOwnerPage + 1) < maxDoorOwnerPageCount)
        {
            lore.add(messages.getString("GUI.ToPage") + (doorOwnerPage + 2) + messages.getString("GUI.OutOf") + maxDoorOwnerPageCount);
            gui.addItem(7, new GUIItem(GUI.PAGESWITCHMAT, messages.getString("GUI.NextPage"), lore, doorOwnerPage + 2));
            lore.clear();
        }

        lore.add(messages.getString("GUI.MoreInfoMenu") + gui.getDoor().getName());
        lore.add("This door has ID " + gui.getDoor().getDoorUID());
        lore.add(messages.getString(DoorType.getNameKey(gui.getDoor().getType())));
        gui.addItem(4, new GUIItem(GUI.CURRDOORMAT, gui.getDoor().getName() + ": " + gui.getDoor().getDoorUID(), lore, 1));
    }

    protected void fillPage()
    {
        int idx = 9;
        missingHeadTextures = 0;
        for (DoorOwner owner : owners)
        {
            GUIItem item = new GUIItem(plugin, owner, gui.getPlayer());
            if (item.missingHeadTexture())
                ++missingHeadTextures;
            gui.addItem(idx++, item);
        }
    }

    @Override
    public void refresh()
    {
        owners = plugin.getCommander().getDoorOwners(gui.getDoor().getDoorUID(), gui.getPlayer().getUniqueId());
        Collections.sort(owners, Comparator.comparing(DoorOwner::getPlayerName));
        maxDoorOwnerPageCount = owners.size() / (GUI.CHESTSIZE - 9) + ((owners.size() % (GUI.CHESTSIZE - 9)) == 0 ? 0 : 1);

        fillHeader();
        fillPage();

        if (missingHeadTextures == 0)
            return;

        // It usually takes a while for the skull textures to load.
        // This will refresh the skulls every now and then.
        // Until a texture is found, the default player texture is used.
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                if (missingHeadTextures == 0 || !gui.isOpen())
                    cancel();
                else
                    refresh();
            }
        }.runTaskTimer(plugin, 10, 20);
    }

    private void removeOwner(DoorOwner owner)
    {
        plugin.getCommander().removeOwner(owner.getDoorUID(), owner.getPlayerUUID());
        owners.remove(owners.indexOf(owner));
    }
}
