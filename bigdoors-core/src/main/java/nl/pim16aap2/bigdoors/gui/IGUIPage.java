package nl.pim16aap2.bigdoors.gui;

import nl.pim16aap2.bigdoors.spigotutil.PageType;
import org.jetbrains.annotations.NotNull;

interface IGUIPage
{
    void handleInput(int interactionIDX);

    void refresh();

    @NotNull PageType getPageType();
}
