package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Implementation of {@link IMessagingInterface} for Stdout. Mostly useful for debugging.
 *
 * @author Pim
 * @see IMessagingInterface
 */
public class MessagingInterfaceStdout implements IMessagingInterface
{
    private final @NotNull String formattedName;

    public MessagingInterfaceStdout(final @NotNull String name)
    {
        formattedName = IPLogger.formatName(name);
    }

    @Override
    public void writeToConsole(final @NotNull Level level, final @NotNull String message)
    {
        System.out.println(formattedName + message);
    }

    @Override
    public void messagePlayer(final @NotNull IPPlayer player, final @NotNull String message)
    {
        System.out.println(formattedName + " to player: \"" + player.getName() + "\": " + message);
    }

    @Override
    public void sendMessageToTarget(final @NotNull Object target, final @NotNull Level level,
                                    final @NotNull String message)
    {
        System.out.println(formattedName + " to a target: " + message);
    }

    @Override
    public void broadcastMessage(final @NotNull String message)
    {
        System.out.println(message);
    }
}
