package nl.pim16aap2.bigdoors.testimplementations;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IBigDoorsToolUtil;
import nl.pim16aap2.bigdoors.api.IBlockAnalyzer;
import nl.pim16aap2.bigdoors.api.IChunkManager;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IEconomyManager;
import nl.pim16aap2.bigdoors.api.IGlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPermissionsManager;
import nl.pim16aap2.bigdoors.api.IPowerBlockRedstoneManager;
import nl.pim16aap2.bigdoors.api.IProtectionCompatManager;
import nl.pim16aap2.bigdoors.api.ISoundEngine;
import nl.pim16aap2.bigdoors.api.factories.IDoorActionEventFactory;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEvent;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.PowerBlockManager;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public final class TestPlatform implements IBigDoorsPlatform
{
    private final TestPPlayerFactory pPlayerFactory = new TestPPlayerFactory();
    private final TestPWorldFactory pWorldFactory = new TestPWorldFactory();
    private final TestPLocationFactory pLocationFactory = new TestPLocationFactory();
    private final TestConfigLoader configLoader = new TestConfigLoader();

    private static final File DATA_DIRECTORY = new File(".");
    private final Set<IRestartable> restartables = new HashSet<>();
    @Nullable
    private Messages messages;
    private final IBigDoorsToolUtil bigDoorsToolUtil = new TestBigDoorsToolUtil();

    private final IEconomyManager economyManager = new TestEconomyManager();
    private final IPermissionsManager permissionsManager = new TestPermissionsManager();
    private final IProtectionCompatManager protectionCompatManager = new TestProtectionCompatManager();
    private final IPLogger pLogger = new BasicPLogger();

    @Getter @Setter
    private DatabaseManager databaseManager;

    @Getter
    @Setter
    private PowerBlockManager powerBlockManager = null;

    public TestPlatform()
    {
        // TODO: Reinitialize everything between every test.
    }

    @Override public @NotNull File getDataDirectory()
    {
        return DATA_DIRECTORY;
    }

    @Override
    public @NotNull IPLocationFactory getPLocationFactory()
    {
        return pLocationFactory;
    }

    @Override
    public @NotNull IBigDoorsToolUtil getBigDoorsToolUtil()
    {
        return bigDoorsToolUtil;
    }

    @Override
    public @NotNull IEconomyManager getEconomyManager()
    {
        return economyManager;
    }

    @Override
    public @NotNull IPermissionsManager getPermissionsManager()
    {
        return permissionsManager;
    }

    @Override
    public @NotNull IProtectionCompatManager getProtectionCompatManager()
    {
        return protectionCompatManager;
    }

    @Override
    public @NotNull IPWorldFactory getPWorldFactory()
    {
        return pWorldFactory;
    }

    @Override
    public @NotNull IPBlockDataFactory getPBlockDataFactory()
    {
        return null;
    }

    @Override
    public @NotNull IFallingBlockFactory getFallingBlockFactory()
    {
        return null;
    }

    @Override
    public @NotNull IPPlayerFactory getPPlayerFactory()
    {
        return pPlayerFactory;
    }

    @Override
    public @NotNull IConfigLoader getConfigLoader()
    {
        return configLoader;
    }

    @Override
    public @NotNull ISoundEngine getSoundEngine()
    {
        return null;
    }

    @Override
    public @NotNull IMessagingInterface getMessagingInterface()
    {
        return null;
    }

    public void setMessages(final @NotNull Messages messages)
    {
        this.messages = messages;
    }

    @Override
    public @NotNull Messages getMessages()
    {
        return messages;
    }

    @Override
    public @NotNull IMessageable getMessageableServer()
    {
        return TestMessageableServer.get();
    }

    @Override
    public @NotNull IBlockAnalyzer getBlockAnalyzer()
    {
        return null;
    }

    @Override
    public @NotNull IPowerBlockRedstoneManager getPowerBlockRedstoneManager()
    {
        return null;
    }

    @Override
    public @NotNull IChunkManager getChunkManager()
    {
        return null;
    }

    @Override
    public @NotNull IDoorActionEventFactory getDoorActionEventFactory()
    {
        return null;
    }

    @Override
    public void callDoorActionEvent(final @NotNull IDoorEvent doorActionEvent)
    {

    }

    @Override
    public boolean isMainThread(long threadID)
    {
        return false;
    }

    @Override
    public @NotNull <T> IPExecutor<T> newPExecutor()
    {
        return null;
    }

    @Override
    public @NotNull IGlowingBlockSpawner getGlowingBlockSpawner()
    {
        return null;
    }

    @Override
    public @NonNull IPLogger getPLogger()
    {
        return pLogger;
    }

    @Override
    public void registerRestartable(final @NotNull IRestartable restartable)
    {
        restartables.add(restartable);
    }

    @Override
    public boolean isRestartableRegistered(final @NotNull IRestartable restartable)
    {
        return restartables.contains(restartable);
    }

    @Override
    public void deregisterRestartable(final @NotNull IRestartable restartable)
    {
        restartables.remove(restartable);
    }
}
