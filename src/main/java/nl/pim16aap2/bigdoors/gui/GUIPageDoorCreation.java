package nl.pim16aap2.bigdoors.gui;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.util.Messages;
import nl.pim16aap2.bigdoors.util.PageType;

// UNIMPLEMENTED!
@Deprecated
public class GUIPageDoorCreation implements IGUIPage
{
    protected final BigDoors plugin;
    protected final GUI gui;
    protected final Messages messages;

    @Deprecated
    public GUIPageDoorCreation(final BigDoors plugin, final GUI gui)
    {
        this.plugin = plugin;
        this.gui = gui;
        messages = plugin.getMessages();
        refresh();
    }

    @Override
    public PageType getPageType()
    {
        return PageType.DOORCREATION;
    }

    @Override
    public void handleInput(int interactionIDX)
    {

    }

    @Override
    public void refresh()
    {

    }
}
