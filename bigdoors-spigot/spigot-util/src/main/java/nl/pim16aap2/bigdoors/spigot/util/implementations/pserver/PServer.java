package nl.pim16aap2.bigdoors.spigot.util.implementations.pserver;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.commands.IPServer;
import nl.pim16aap2.bigdoors.text.Text;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.logging.Level;

/**
 * Represents the Spigot implementation of {@link IPServer}.
 *
 * @author Pim
 */
@Singleton
@Flogger
public class PServer implements IPServer
{
    @Inject
    public PServer()
    {
    }

    @Override
    public void sendMessage(String message)
    {
        log.at(Level.INFO).log("%s", message);
    }

    @Override
    public void sendMessage(Text text)
    {
        sendMessage(text.toPlainString());
    }
}
