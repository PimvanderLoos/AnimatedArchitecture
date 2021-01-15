package nl.pim16aap2.bigdoors;

import lombok.experimental.UtilityClass;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IRestartableHolder;
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
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

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
            Assertions.fail();
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
        Assertions.assertNotNull(TEST_DIR);
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
     * Stubs the map for the {@link DoorRegistry}. No entries can be added to it or retrieved from it after this method
     * has been called.
     */
    public void setFakeDoorRegistry()
    {
        DoorRegistry.get().init(0, 1, 0, Duration.ZERO, false);
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
                Thread.currentThread().interrupt();
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
            Thread.currentThread().interrupt();
        }

        Assertions.assertTrue(PLogger.get().isEmpty());
    }
}
