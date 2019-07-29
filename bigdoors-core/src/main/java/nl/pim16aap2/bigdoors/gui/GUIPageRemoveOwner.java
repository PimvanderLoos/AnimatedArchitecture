package nl.pim16aap2.bigdoors.gui;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.PageType;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GUIPageRemoveOwner implements IGUIPage
{
    protected final BigDoors plugin;
    protected final GUI gui;
    protected final Messages messages;
    protected int missingHeadTextures = 0;
    private int doorOwnerPage = 0;
    private int maxDoorOwnerPageCount = 0;

    private List<DoorOwner> owners;

    protected GUIPageRemoveOwner(final BigDoors plugin, final GUI gui)
    {
        this.plugin = plugin;
        this.gui = gui;
        messages = plugin.getMessages();
        refresh();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageType getPageType()
    {
        return PageType.REMOVEOWNER;
    }

    /**
     * {@inheritDoc}
     */
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
        List<String> lore = new ArrayList<>();
        lore.add(plugin.getMessages().getString(Message.GUI_DESCRIPTION_PREVIOUSPAGE,
                                                Integer.toString(gui.getPage() + 1),
                                                Integer.toString(gui.getPage()),
                                                Integer.toString(gui.getMaxPageCount())));
        gui.addItem(0, new GUIItem(GUI.PAGESWITCHMAT,
                                   plugin.getMessages().getString(Message.GUI_BUTTON_PREVIOUSPAGE), lore,
                                   Math.max(1, gui.getPage())));
        lore.clear();

        if (doorOwnerPage != 0)
        {
            lore.add(plugin.getMessages().getString(Message.GUI_DESCRIPTION_PREVIOUSPAGE,
                                                    Integer.toString(doorOwnerPage + 2),
                                                    Integer.toString(doorOwnerPage),
                                                    Integer.toString(maxDoorOwnerPageCount)));
            gui.addItem(0, new GUIItem(GUI.PAGESWITCHMAT,
                                       plugin.getMessages().getString(Message.GUI_BUTTON_PREVIOUSPAGE), lore,
                                       doorOwnerPage));
            lore.clear();
        }

        if ((doorOwnerPage + 1) < maxDoorOwnerPageCount)
        {
            lore.add(plugin.getMessages().getString(Message.GUI_DESCRIPTION_NEXTPAGE,
                                                    Integer.toString(doorOwnerPage + 2),
                                                    Integer.toString(doorOwnerPage),
                                                    Integer.toString(maxDoorOwnerPageCount)));
            gui.addItem(7, new GUIItem(GUI.PAGESWITCHMAT, messages.getString(Message.GUI_BUTTON_NEXTPAGE), lore,
                                       doorOwnerPage + 2));
            lore.clear();
        }

        lore.add(messages.getString(Message.GUI_DESCRIPTION_INFO, gui.getDoor().getName()));
        lore.add(messages.getString(Message.GUI_DESCRIPTION_DOORID, Long.toString(gui.getDoor().getDoorUID())));
        lore.add(messages.getString(DoorType.getMessage(gui.getDoor().getType())));
        gui.addItem(4,
                    new GUIItem(GUI.CURRDOORMAT, gui.getDoor().getName() + ": " + gui.getDoor().getDoorUID(), lore, 1));
    }

    protected void fillPage()
    {
        int idx = 9;
        missingHeadTextures = 0;
        for (DoorOwner owner : owners)
        {
            GUIItem item = new GUIItem(plugin.getHeadManager(), owner);
            if (item.missingHeadTexture())
                ++missingHeadTextures;
            gui.addItem(idx++, item);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh()
    {
        owners = plugin.getDatabaseManager().getDoorOwners(gui.getDoor().getDoorUID());
        Collections.sort(owners, Comparator.comparing(DoorOwner::getPlayerName));
        maxDoorOwnerPageCount =
            owners.size() / (GUI.CHESTSIZE - 9) + ((owners.size() % (GUI.CHESTSIZE - 9)) == 0 ? 0 : 1);

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
        plugin.getDatabaseManager().removeOwner(owner.getDoorUID(), owner.getPlayerUUID());
        owners.remove(owners.indexOf(owner));
    }
}
