package nl.pim16aap2.bigdoors;

import junit.framework.Assert;
import lombok.experimental.UtilityClass;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IRestartableHolder;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
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
    {
        if (isInitialized)
            return;

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
