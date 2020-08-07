package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IBlockAnalyzer;
import nl.pim16aap2.bigdoors.api.IChunkManager;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPowerBlockRedstoneManager;
import nl.pim16aap2.bigdoors.api.IRestartable;
import nl.pim16aap2.bigdoors.api.IRestartableHolder;
import nl.pim16aap2.bigdoors.api.ISoundEngine;
import nl.pim16aap2.bigdoors.api.factories.IDoorActionEventFactory;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEvent;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public final class TestPlatform implements IBigDoorsPlatform, IRestartableHolder
{
    private final TestPPlayerFactory pPlayerFactory = new TestPPlayerFactory();
    private final TestPWorldFactory pWorldFactory = new TestPWorldFactory();
    private final TestPLocationFactory pLocationFactory = new TestPLocationFactory();
    private static final File dataDirectory = new File(".");
    private final Set<IRestartable> restartables = new HashSet<>();
    @Nullable
    private Messages messages;

    public TestPlatform()
    {
        // TODO: INIT STUFF. Perhaps add a #getDataFolder() to IBigDoorsPlatform.
    }

    /** {@inheritDoc} */
    @Override
    @NotNull
    public File getDataDirectory()
    {
        return dataDirectory;
    }

    @Override
    @NotNull
    public IPLocationFactory getPLocationFactory()
    {
        return pLocationFactory;
    }

    @Override
    @NotNull
    public IPWorldFactory getPWorldFactory()
    {
        return pWorldFactory;
    }

    @Override
    @NotNull
    public IPBlockDataFactory getPBlockDataFactory()
    {
        return null;
    }

    @Override
    @NotNull
    public IFallingBlockFactory getFallingBlockFactory()
    {
        return null;
    }

    @Override
    @NotNull
    public IPPlayerFactory getPPlayerFactory()
    {
        return pPlayerFactory;
    }

    @Override
    @NotNull
    public IConfigLoader getConfigLoader()
    {
        return null;
    }

    @Override
    @NotNull
    public ISoundEngine getSoundEngine()
    {
        return null;
    }

    @Override
    @NotNull
    public IMessagingInterface getMessagingInterface()
    {
        return null;
    }

    public void setMessages(final @NotNull Messages messages)
    {
        this.messages = messages;
    }

    @Override
    @NotNull
    public Messages getMessages()
    {
        return messages;
    }

    @Override
    @NotNull
    public IMessageable getMessageableServer()
    {
        return MessageableServerTest.get();
    }

    @Override
    @NotNull
    public IBlockAnalyzer getBlockAnalyzer()
    {
        return null;
    }

    @Override
    @NotNull
    public IPowerBlockRedstoneManager getPowerBlockRedstoneManager()
    {
        return null;
    }

    @Override
    @NotNull
    public IChunkManager getChunkManager()
    {
        return null;
    }

    @Override
    @NotNull
    public IDoorActionEventFactory getDoorActionEventFactory()
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
    @NotNull
    public <T> IPExecutor<T> newPExecutor()
    {
        return null;
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
}
