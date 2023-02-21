package nl.pim16aap2.animatedarchitecture.spigot.util.text;

import lombok.EqualsAndHashCode;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a text decorator for the Spigot platform that decorates text with links that execute a command when
 * clicked.
 */
@EqualsAndHashCode
public final class ClickableTextDecorator implements ITextDecoratorSpigot
{
    /**
     * The command to execute when the text is clicked.
     */
    private final String command;

    /**
     * The optional message to show when hovering over the text.
     */
    private final @Nullable String hoverMessage;

    public ClickableTextDecorator(String command, @Nullable String hoverMessage)
    {
        this.command = command;
        this.hoverMessage = hoverMessage;
    }

    @Override
    public void decorateComponent(BaseComponent component)
    {
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        if (hoverMessage != null)
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverMessage)));
    }
}
