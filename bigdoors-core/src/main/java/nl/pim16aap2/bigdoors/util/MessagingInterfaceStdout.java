package nl.pim16aap2.bigdoors.util;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;
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

    public MessagingInterfaceStdout(final @NotNull String name)
    {
        formattedName = PLogger.formatName(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToConsole(final @NotNull Level level, final @NotNull String message)
    {
        System.out.println(formattedName + message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messagePlayer(final @NotNull UUID playerUUID, final @NotNull String message)
    {
        System.out.println(formattedName + " to player: \"" + playerUUID + "\": " + message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendMessageToTarget(final @NotNull Object target, final @NotNull Level level,
                                    final @NotNull String message)
    {
        System.out.println(formattedName + " to a target: " + message);
    }
}
