package nl.pim16aap2.animatedarchitecture.spigot.util.implementations;

import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.IServer;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.localization.PersonalizedLocalizer;
import nl.pim16aap2.animatedarchitecture.core.text.Text;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents the Spigot implementation of {@link IServer}.
 */
@Singleton
@Flogger
@Getter
public class SpigotServer implements IServer
{
    private final ITextFactory textFactory;
    private final PersonalizedLocalizer personalizedLocalizer;

    @Inject
    SpigotServer(ILocalizer localizer, ITextFactory textFactory)
    {
        this.textFactory = textFactory;
        this.personalizedLocalizer = new PersonalizedLocalizer(localizer, null);
    }

    @Override
    public void sendMessage(Text text)
    {
        log.atInfo().log("%s", text);
    }
}
