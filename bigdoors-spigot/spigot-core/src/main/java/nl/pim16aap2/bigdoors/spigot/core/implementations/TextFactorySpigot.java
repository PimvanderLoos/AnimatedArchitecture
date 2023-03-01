package nl.pim16aap2.bigdoors.spigot.core.implementations;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import nl.pim16aap2.bigdoors.core.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.core.text.ColorScheme;
import nl.pim16aap2.bigdoors.core.text.ITextComponentFactory;
import nl.pim16aap2.bigdoors.core.text.Text;
import nl.pim16aap2.bigdoors.core.text.TextType;
import nl.pim16aap2.bigdoors.spigot.util.text.TextComponentFactorySpigot;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TextFactorySpigot implements ITextFactory
{
    private final ITextComponentFactory textComponentFactory;

    @Inject TextFactorySpigot()
    {
        this.textComponentFactory = new TextComponentFactorySpigot(buildColorScheme());
    }

    private ColorScheme<BaseComponent> buildColorScheme()
    {
        return ColorScheme
            .<BaseComponent>builder()
            .addStyle(TextType.ERROR, new StyleBuilder().color(ChatColor.DARK_RED).build())
            .addStyle(TextType.INFO, new StyleBuilder().color(ChatColor.AQUA).build())
            .addStyle(TextType.HIGHLIGHT, new StyleBuilder().color(ChatColor.GOLD).underlined(true).build())
            .addStyle(TextType.SUCCESS, new StyleBuilder().color(ChatColor.GREEN).build())
            .setDefaultStyle(new StyleBuilder().color(ChatColor.WHITE).build())
            .build();
    }

    @Override
    public Text newText()
    {
        return new Text(textComponentFactory);
    }

    @Accessors(chain = true, fluent = true) @Setter
    public static final class StyleBuilder
    {
        private @Nullable ChatColor color;
        private @Nullable String font;
        private @Nullable Boolean bold;
        private @Nullable Boolean italic;
        private @Nullable Boolean underlined;
        private @Nullable Boolean strikethrough;
        private @Nullable Boolean obfuscated;

        public BaseComponent build()
        {
            final TextComponent ret = new TextComponent();
            ret.setColor(color);
            ret.setFont(font);
            ret.setBold(bold);
            ret.setItalic(italic);
            ret.setUnderlined(underlined);
            ret.setStrikethrough(strikethrough);
            ret.setObfuscated(obfuscated);
            return ret;
        }
    }
}
