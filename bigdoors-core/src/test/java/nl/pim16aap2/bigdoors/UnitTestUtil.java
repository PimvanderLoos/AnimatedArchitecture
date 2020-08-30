package nl.pim16aap2.bigdoors;

import junit.framework.Assert;
import lombok.experimental.UtilityClass;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IRestartableHolder;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DoorRegistry;
import nl.pim16aap2.bigdoors.storage.IStorage;
import nl.pim16aap2.bigdoors.testimplementations.TestConfigLoader;
import nl.pim16aap2.bigdoors.testimplementations.TestMessagingInterface;
import nl.pim16aap2.bigdoors.testimplementations.TestPlatform;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;

@UtilityClass
public class UnitTestUtil
{
    private boolean isInitialized = false;

    @NotNull
    public final IConfigLoader CONFIG = new TestConfigLoader();
    @NotNull
    public final File TEST_RESOURCE_FOLDER = new File("src/test/resources");
    @NotNull
    public final TestPlatform PLATFORM = new TestPlatform();
    @NotNull
    public String TEST_DIR;

    public final double EPSILON = 1E-6;

    static
    {
        BigDoors.get().setMessagingInterface(new TestMessagingInterface());
        try
        {
            // This will create a fake database manager that can be used for testing.
            Constructor<DatabaseManager> ctor = DatabaseManager.class.getDeclaredConstructor(IRestartableHolder.class);
            ctor.setAccessible(true);
            ctor.newInstance(PLATFORM);
        }
        catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e)
        {
            e.printStackTrace();
            Assert.fail();
        }

        try
        {
            TEST_DIR = PLATFORM.getDataDirectory().getCanonicalPath() + "/tests";
            System.out.println("test_dir = " + TEST_DIR);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        Assert.assertNotNull(TEST_DIR);
    }

    public void setDatabaseStorage(final @Nullable IStorage storage)
        throws NoSuchFieldException, IllegalAccessException
    {
        Field field = DatabaseManager.class.getDeclaredField("db");
        field.setAccessible(true);
        field.set(DatabaseManager.get(), storage);
    }

    public ExecutorService getDatabaseManagerThreadPool()
        throws NoSuchFieldException, IllegalAccessException
    {
        Field field = DatabaseManager.class.getDeclaredField("threadPool");
        field.setAccessible(true);
        return (ExecutorService) field.get(DatabaseManager.get());
    }

    @NotNull
    public final File LOG_FILE = new File(UnitTestUtil.TEST_DIR, "/log.txt");

    public void setupStatic()
        throws NoSuchFieldException, IllegalAccessException
    {
        if (isInitialized)
            return;

        setFakeDoorRegistry();

        System.out.println("LOG_FILE = " + LOG_FILE.toString());
        PLogger.init(LOG_FILE);
        BigDoors.get().setMessagingInterface(new TestMessagingInterface());
        BigDoors.get().setBigDoorsPlatform(PLATFORM);
        System.out.println("TEST_RESOURCE_FOLDER = " + TEST_RESOURCE_FOLDER.getAbsolutePath());
        PLATFORM.setMessages(
            new Messages(PLATFORM, new File(TEST_RESOURCE_FOLDER.getAbsolutePath()), "en_US_TEST", PLogger.get()));
        isInitialized = true;
    }

    /**
     * Stubs the maps for the {@link DoorRegistry}. No entries can be added to it or retrieved from it after this metd
     * has been called.
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public void setFakeDoorRegistry()
        throws NoSuchFieldException, IllegalAccessException
    {
        final @NotNull Field fieldDoors = DoorRegistry.class.getDeclaredField("doors");
        final @NotNull Field fieldFutureDoors = DoorRegistry.class.getDeclaredField("doors");

        final @NotNull Map<Long, CompletableFuture<Optional<AbstractDoorBase>>> fakeMap = getFakeMap();

        fieldDoors.setAccessible(true);
        fieldFutureDoors.setAccessible(true);

        fieldDoors.set(DoorRegistry.get(), fakeMap);
        fieldFutureDoors.set(DoorRegistry.get(), fakeMap);
    }

    /**
     * Creates a 'fake' map with stubbed methods so you cannot add/retrieve items to/from it.
     *
     * @param <T1> The type of the key.
     * @param <T2> The type of the value.
     * @return The 'fake' map.
     */
    public @NotNull <T1, T2> Map<T1, T2> getFakeMap()
    {
        return new HashMap<T1, T2>()
        {
            @Override
            public T2 get(Object key)
            {
                return null;
            }

            @Override
            public boolean containsKey(Object key)
            {
                return false;
            }

            @Override
            public T2 put(T1 key, T2 value)
            {
                return null;
            }

            @Override
            public void putAll(Map m)
            {
            }

            @Override
            public T2 putIfAbsent(T1 key, T2 value)
            {
                return null;
            }

            @Override
            public T2 computeIfAbsent(Object key, Function mappingFunction)
            {
                return null;
            }

            @Override
            public T2 computeIfPresent(Object key, BiFunction remappingFunction)
            {
                return null;
            }
        };
    }

    /**
     * Checks if an object and an Optional are the same or if they both don't exist/are null.
     *
     * @param obj The object to compare the optional to.
     * @param opt The Optional to compare against the object.
     * @param <T> The type of the Object and Optional.
     * @return True if the Optional holds an object that equals the object or if the object is null and the Optional is
     * empty.
     */
    public <T> boolean optionalEquals(final @Nullable T obj, final @NotNull Optional<T> opt)
    {
        return opt.map(t -> t.equals(obj)).orElseGet(() -> obj == null);
    }

    /**
     * Makes this thread wait for the logger to finish writing everything to the log file.
     */
    public void waitForLogger()
    {
        int count = 0;
        while (!PLogger.get().isEmpty())
        {
            if (count > 100) // wait no more than 1 second.
                break;
            try
            {
                count += 1;
                Thread.sleep(10L);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        // Wait a bit longer to make sure it's finished writing the file as well.
        try
        {
            Thread.sleep(20L);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        Assert.assertTrue(PLogger.get().isEmpty());
    }
}
