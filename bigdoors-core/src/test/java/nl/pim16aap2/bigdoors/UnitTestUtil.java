package nl.pim16aap2.bigdoors;

import lombok.NonNull;
import lombok.val;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DdConst;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.mockito.Mockito;

import java.util.UUID;

public class UnitTestUtil
{

    /**
     * Initializes and registers a new {@link IBigDoorsPlatform}. A {@link BasicPLogger} is also set up.
     *
     * @return The new {@link IBigDoorsPlatform}.
     */
    public static IBigDoorsPlatform initPlatform()
    {
        IBigDoorsPlatform platform = Mockito.mock(IBigDoorsPlatform.class);
        BigDoors.get().setBigDoorsPlatform(platform);
        Mockito.when(platform.getPLogger()).thenReturn(new BasicPLogger());
        return platform;
    }

    public static Messages initMessages()
    {
        val messages = Mockito.mock(Messages.class);
        Mockito.when(messages.getString(Mockito.any()))
               .thenAnswer(invocation -> invocation.getArgument(0, Message.class).name());
        Mockito.when(messages.getString(Mockito.any(), Mockito.any()))
               .thenAnswer(invocation ->
                           {
                               String ret = invocation.getArgument(0, Message.class).name();
                               if (invocation.getArguments().length == 1)
                                   return ret;

                               for (int idx = 1; idx < invocation.getArguments().length; ++idx)
                                   //noinspection StringConcatenationInLoop
                                   ret += " " + invocation.getArgument(idx, String.class);
                               return ret;
                           });
        return messages;
    }

    public static IPWorld getWorld()
    {
        val world = Mockito.mock(IPWorld.class);
        Mockito.when(world.getWorldName()).thenReturn(UUID.randomUUID().toString());
        return world;
    }

    public static IPLocation getLocation(final @NonNull Vector3DdConst vec)
    {
        return getLocation(vec.getX(), vec.getY(), vec.getZ());
    }

    public static IPLocation getLocation(final @NonNull Vector3DiConst vec)
    {
        return getLocation(vec.getX(), vec.getY(), vec.getZ());
    }

    public static IPLocation getLocation(final @NonNull Vector3DdConst vec, final @NonNull IPWorld world)
    {
        return getLocation(vec.getX(), vec.getY(), vec.getZ(), world);
    }

    public static IPLocation getLocation(final @NonNull Vector3DiConst vec, final @NonNull IPWorld world)
    {
        return getLocation(vec.getX(), vec.getY(), vec.getZ(), world);
    }

    public static IPLocation getLocation(final double x, final double y, final double z)
    {
        return getLocation(x, y, z, getWorld());
    }

    public static IPLocation getLocation(final double x, final double y, final double z, final @NonNull IPWorld world)
    {
        val loc = Mockito.mock(IPLocation.class);

        Mockito.when(loc.getWorld()).thenReturn(world);

        Mockito.when(loc.getX()).thenReturn(x);
        Mockito.when(loc.getY()).thenReturn(y);
        Mockito.when(loc.getZ()).thenReturn(z);

        Mockito.when(loc.getBlockX()).thenReturn((int) x);
        Mockito.when(loc.getBlockY()).thenReturn((int) y);
        Mockito.when(loc.getBlockZ()).thenReturn((int) z);

        Mockito.when(loc.getPosition()).thenReturn(new Vector3Di((int) x, (int) y, (int) z));

        Mockito.when(loc.getChunk()).thenReturn(new Vector2Di(((int) x) << 4, ((int) z) << 4));

        return loc;
    }
}
