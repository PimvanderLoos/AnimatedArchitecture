package nl.pim16aap2.bigdoors.GUI;

import nl.pim16aap2.bigdoors.util.PageType;

public interface IGUIPage
{
    public void handleInput(int interactionIDX);

    public void refresh();

    public PageType getPageType();
}
