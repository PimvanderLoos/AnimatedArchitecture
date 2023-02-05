package nl.pim16aap2.bigdoors.spigot.util.implementations.pserver;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.commands.IPServer;
import nl.pim16aap2.bigdoors.core.text.Text;

import javax.inject.Inject;
import javax.inject.Singleton;

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
    public void sendMessage(Text text)
    {
        log.atInfo().log("%s", text.toPlainString());
    }
}
