package nl.pim16aap2.bigdoors.spigot.util.text;

import nl.pim16aap2.bigdoors.core.text.ITextComponentFactory;
import nl.pim16aap2.bigdoors.core.text.TextComponent;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of the text component factory for the Spigot platform.
 */
@Singleton
public final class TextComponentFactorySpigot implements ITextComponentFactory
{
    @Inject TextComponentFactorySpigot()
    {
    }

    @Override
    public TextComponent updateComponentWithCommand(TextComponent textComponent, String command, @Nullable String info)
    {
        return textComponent.withDecorators(new ClickableTextDecorator(command, info));
    }
}
