package nl.pim16aap2.bigdoors.util;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.logging.IPLogger;

import java.util.logging.Level;

/**
 * Implementation of {@link IMessagingInterface} for Stdout. Mostly useful for debugging.
 *
 * @author Pim
 * @see IMessagingInterface
 */
public class MessagingInterfaceStdout implements IMessagingInterface
{
private final @NonNull String formattedName;

    public MessagingInterfaceStdout(final @NonNull String name)
    {
        formattedName = IPLogger.formatName(name);
    }

    @Override
    public void writeToConsole(final @NonNull Level level, final @NonNull String message)
    {
        System.out.println(formattedName + message);
    }

    @Override
    public void messagePlayer(final @NonNull IPPlayer player, final @NonNull String message)
    {
        System.out.println(formattedName + " to player: \"" + player.getName() + "\": " + message);
    }

    @Override
    public void sendMessageToTarget(final @NonNull Object target, final @NonNull Level level,
                                    final @NonNull String message)
    {
        System.out.println(formattedName + " to a target: " + message);
    }

    @Override
    public void broadcastMessage(final @NonNull String message)
    {
        System.out.println(message);
    }
}
