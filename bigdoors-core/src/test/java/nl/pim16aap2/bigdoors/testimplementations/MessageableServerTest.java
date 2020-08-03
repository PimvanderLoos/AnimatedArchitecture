package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.api.IMessageable;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class MessageableServerTest implements IMessageable
{
    public static final MessageableServerTest INSTANCE = new MessageableServerTest();

    private MessageableServerTest()
    {
    }

    public static MessageableServerTest get()
    {
        return INSTANCE;
    }

    @Override
    public void sendMessage(final @NotNull Level level, final @NotNull String message)
    {
        System.out.println(level.toString() + ": " + message);
    }
}
