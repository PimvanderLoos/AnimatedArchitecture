package nl.pim16aap2.bigdoors.spigot.util.text;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.jetbrains.annotations.Nullable;

public final class ClickableTextDecorator implements ITextDecoratorSpigot
{
    private final String command;
    private final @Nullable String hoverMessage;

    public ClickableTextDecorator(String command, @Nullable String hoverMessage)
    {
        this.command = command;
        this.hoverMessage = hoverMessage;
    }

    private void decorateBaseComponent(BaseComponent component)
    {
        if (component instanceof net.md_5.bungee.api.chat.TextComponent textComponent &&
            textComponent.getText().isBlank())
            return;

        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        if (hoverMessage != null)
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverMessage)));
    }

    @Override
    public void decorateComponents(BaseComponent[] components)
    {
        for (final BaseComponent component : components)
            decorateBaseComponent(component);
    }
}
