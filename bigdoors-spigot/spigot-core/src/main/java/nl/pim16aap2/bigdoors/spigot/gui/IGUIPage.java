package nl.pim16aap2.bigdoors.spigot.gui;

import nl.pim16aap2.bigdoors.spigot.util.PageType;
import org.jetbrains.annotations.NotNull;

interface IGUIPage
{
    void handleInput(int interactionIDX);

    void refresh();

    @NotNull PageType getPageType();

    /**
     * Kills a GUI page. Any running processes must be killed.
     */
    void kill();
}
