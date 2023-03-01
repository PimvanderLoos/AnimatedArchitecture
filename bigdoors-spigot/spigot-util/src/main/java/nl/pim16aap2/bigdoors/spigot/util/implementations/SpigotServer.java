package nl.pim16aap2.bigdoors.spigot.util.implementations;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.commands.IServer;
import nl.pim16aap2.bigdoors.core.text.Text;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents the Spigot implementation of {@link IServer}.
 *
 * @author Pim
 */
@Singleton
@Flogger
public class SpigotServer implements IServer
{
    @Inject
    public SpigotServer()
    {
    }

    @Override
    public void sendMessage(Text text)
    {
        log.atInfo().log("%s", text);
    }
}
