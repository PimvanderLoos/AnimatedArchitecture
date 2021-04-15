package nl.pim16aap2.bigdoors.spigot.gui;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.spigot.util.PageType;

interface IGUIPage
{
    void handleInput(int interactionIDX);

    void refresh();

    @NonNull PageType getPageType();

    /**
     * Kills a GUI page. Any running processes must be killed.
     */
    void kill();
}
