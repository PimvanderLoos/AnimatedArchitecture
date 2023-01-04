package nl.pim16aap2.bigdoors.spigot.gui;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.spigot.BigDoorsPlugin;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PPlayerSpigot;
import nl.pim16aap2.bigdoors.util.Util;

public class GUI
{
    private final BigDoorsPlugin bigDoorsPlugin;
    private final ILocalizer localizer;
    private final ITextFactory textFactory;
    private final PPlayerSpigot inventoryHolder;
    private final GUIData guiData;

    @AssistedInject//
    GUI(
        BigDoorsPlugin bigDoorsPlugin, ILocalizer localizer, ITextFactory textFactory,
        @Assisted IPPlayer inventoryHolder, @Assisted GUIData guiData)
    {
        this.bigDoorsPlugin = bigDoorsPlugin;
        this.localizer = localizer;
        this.textFactory = textFactory;
        this.inventoryHolder = Util.requireNonNull(SpigotAdapter.getPPlayerSpigot(inventoryHolder), "InventoryHolder");
        this.guiData = guiData;
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates a new GUI.
         *
         * @param inventoryHolder
         *     The player for whom to create the inventory.
         * @param guiData
         *     The {@link GUIData} to use.
         */
        GUI newGUI(IPPlayer inventoryHolder, GUIData guiData);
    }
}
