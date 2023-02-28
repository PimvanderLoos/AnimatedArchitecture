package nl.pim16aap2.bigdoors.spigot.core.implementations;

import nl.pim16aap2.bigdoors.core.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.core.text.ColorScheme;
import nl.pim16aap2.bigdoors.core.text.ITextComponentFactory;
import nl.pim16aap2.bigdoors.core.text.Text;
import nl.pim16aap2.bigdoors.core.text.TextType;
import nl.pim16aap2.bigdoors.spigot.util.text.TextComponentFactorySpigot;
import org.bukkit.ChatColor;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TextFactorySpigot implements ITextFactory
{
    private final ITextComponentFactory textComponentFactory;
    private ColorScheme colorScheme;

    @Inject
    public TextFactorySpigot(TextComponentFactorySpigot textComponentFactory)
    {
        this.colorScheme =
            ColorScheme.builder()
                       .setDefaultDisable(ChatColor.RESET.toString())
                       .addStyle(TextType.ERROR, ChatColor.DARK_RED.toString())
                       .addStyle(TextType.INFO, ChatColor.AQUA.toString())
                       .addStyle(TextType.HIGHLIGHT, ChatColor.GOLD.toString() + ChatColor.UNDERLINE)
                       .addStyle(TextType.SUCCESS, ChatColor.GREEN.toString())
                       .build();
        this.textComponentFactory = textComponentFactory;
    }

    @Override
    public Text newText()
    {
        return new Text(colorScheme, textComponentFactory);
    }

    @SuppressWarnings("unused")
    public synchronized void updateColorScheme(ColorScheme colorScheme)
    {
        this.colorScheme = colorScheme;
    }
}
