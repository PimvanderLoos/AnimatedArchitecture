package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public final class TestMessagingInterface implements IMessagingInterface
{
    @Override
    public void writeToConsole(final @NotNull Level level, final @NotNull String message)
    {
        System.out.println(level.toString() + ": " + message);
    }

    @Override
    public void messagePlayer(final @NotNull IPPlayer player, final @NotNull String message)
    {
        System.out.println("Recipient: [" + player.getUUID().toString() + "]. Message: " + message);
    }

    @Override
    public void sendMessageToTarget(final @NotNull Object target, final @NotNull Level level,
                                    final @NotNull String message)
    {
        System.out.println("Recipient Type: [" + target.getClass().getSimpleName() + "]. Level: [" + level.toString() +
                               "]. Message: " + message);
    }

    @Override
    public void broadcastMessage(@NotNull String message)
    {
        System.out.println(message);
    }
}
