package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;


import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.PersonalizedLocalizer;
import nl.pim16aap2.animatedarchitecture.core.text.ColorScheme;
import nl.pim16aap2.animatedarchitecture.core.text.ITextComponentFactory;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.spigot.util.text.TextComponentFactorySpigot;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of {@link ITextFactory} for the Spigot platform.
 */
@Singleton
public class TextFactorySpigot implements ITextFactory
{
    private final ITextComponentFactory textComponentFactory;

    @Inject
    public TextFactorySpigot()
    {
        this.textComponentFactory = new TextComponentFactorySpigot(buildColorScheme());
    }

    private ColorScheme<BaseComponent> buildColorScheme()
    {
        return ColorScheme
            .<BaseComponent>builder()
            .addStyle(TextType.ERROR, new StyleBuilder().color(ChatColor.DARK_RED).build())
            .addStyle(TextType.INFO, new StyleBuilder().color(ChatColor.GRAY).build())
            .addStyle(TextType.HIGHLIGHT, new StyleBuilder().color(ChatColor.GOLD).build())
            .addStyle(TextType.CLICKABLE, new StyleBuilder().color(ChatColor.AQUA).underlined(true).build())
            .addStyle(TextType.CLICKABLE_CONFIRM, new StyleBuilder().color(ChatColor.GREEN).underlined(true).build())
            .addStyle(TextType.CLICKABLE_REFUSE, new StyleBuilder().color(ChatColor.RED).underlined(true).build())
            .addStyle(TextType.SUCCESS, new StyleBuilder().color(ChatColor.DARK_GREEN).build())
            .setDefaultStyle(new StyleBuilder().color(ChatColor.WHITE).build())
            .build();
    }

    @Override
    public Text newText(@Nullable PersonalizedLocalizer personalizedLocalizer)
    {
        return new Text(textComponentFactory, personalizedLocalizer);
    }

    @Accessors(chain = true, fluent = true)
    @Setter
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
