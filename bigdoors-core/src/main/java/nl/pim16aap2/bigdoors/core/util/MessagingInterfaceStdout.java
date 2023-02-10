package nl.pim16aap2.bigdoors.core.util;

import nl.pim16aap2.bigdoors.core.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.core.api.IPlayer;

import java.util.logging.Level;


/**
 * Implementation of {@link IMessagingInterface} for Stdout. Mostly useful for debugging.
 *
 * @author Pim
 * @see IMessagingInterface
 */
public class MessagingInterfaceStdout implements IMessagingInterface
{
    private final String formattedName;

    public MessagingInterfaceStdout(String name)
    {
        formattedName = "[" + name + "] ";
    }

    @Override
    public void writeToConsole(Level level, String message)
    {
        System.out.println(formattedName + message);
    }

    @Override
    public void messagePlayer(IPlayer player, String message)
    {
        System.out.println(formattedName + " to player: \"" + player.getName() + "\": " + message);
    }

    @Override
    public void sendMessageToTarget(Object target, Level level, String message)
    {
        System.out.println(formattedName + " to a target: " + message);
    }

    @Override
    public void broadcastMessage(String message)
    {
        System.out.println(message);
    }
}
