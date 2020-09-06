package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.api.factories.IDoorActionEventFactory;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEvent;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public interface IBigDoorsPlatform extends IRestartableHolder
{
    /**
     * Gets the directory where all data will stored.
     *
     * @return The directory where all data will stored.
     */
    @NotNull File getDataDirectory();

    /**
     * Gets the instance of the {@link IPLocationFactory} for this platform.
     *
     * @return The instance of the {@link IPLocationFactory} for this platform.
     */
    @NotNull IPLocationFactory getPLocationFactory();

    /**
     * Gets the instance of the {@link IBigDoorsToolUtil} for this platform.
     *
     * @return The instance of the {@link IBigDoorsToolUtil} for this platform.
     */
    @NotNull IBigDoorsToolUtil getBigDoorsToolUtil();

    /**
     * Gets the instance of the {@link IEconomyManager} for this platform.
     *
     * @return The instance of the {@link IEconomyManager} for this platform.
     */
    @NotNull IEconomyManager getEconomyManager();

    /**
     * Gets the instance of the {@link IPermissionsManager} for this platform.
     *
     * @return The instance of the {@link IPermissionsManager} for this platform.
     */
    @NotNull IPermissionsManager getPermissionsManager();

    /**
     * Gets the instance of the {@link IProtectionCompatManager} for this platform.
     *
     * @return The instance of the {@link IProtectionCompatManager} for this platform.
     */
    @NotNull IProtectionCompatManager getProtectionCompatManager();

    /**
     * Gets the instance of the {@link IPWorldFactory} for this platform.
     *
     * @return The instance of the {@link IPWorldFactory} for this platform.
     */
    @NotNull IPWorldFactory getPWorldFactory();

    /**
     * Gets the instance of the {@link IPBlockDataFactory} for this platform.
     *
     * @return The instance of the {@link IPBlockDataFactory} for this platform.
     */
    @NotNull IPBlockDataFactory getPBlockDataFactory();

    /**
     * Gets the instance of the {@link IFallingBlockFactory} for this platform.
     *
     * @return The instance of the {@link IFallingBlockFactory} for this platform.
     */
    @NotNull IFallingBlockFactory getFallingBlockFactory();

    /**
     * Gets the instance of the {@link IPPlayerFactory} for this platform.
     *
     * @return The instance of the {@link IPPlayerFactory} for this platform.
     */
    @NotNull IPPlayerFactory getPPlayerFactory();

    /**
     * Gets the instance of the {@link IConfigLoader} for this platform.
     *
     * @return The instance of the {@link IConfigLoader} for this platform.
     */
    @NotNull IConfigLoader getConfigLoader();

    /**
     * Gets the instance of the {@link ISoundEngine} for this platform.
     *
     * @return The instance of the {@link ISoundEngine} for this platform.
     */
    @NotNull ISoundEngine getSoundEngine();

    /**
     * Gets the instance of the {@link IMessagingInterface} for this platform.
     *
     * @return The instance of the {@link IMessagingInterface} for this platform.
     */
    @NotNull IMessagingInterface getMessagingInterface();

    /**
     * Gets the instance of the {@link Messages} for this platform.
     *
     * @return The instance of the {@link Messages} for this platform.
     */
    @NotNull Messages getMessages();

    /**
     * Gets the implementation of a {@link IMessageable} for the server.
     *
     * @return The implementation of a {@link IMessageable} for the server.
     */
    @NotNull IMessageable getMessageableServer();

    /**
     * Gets the instance of the {@link IBlockAnalyzer} for this platform.
     *
     * @return The instance of the {@link IBlockAnalyzer} for this platform.
     */
    @NotNull IBlockAnalyzer getBlockAnalyzer();

    /**
     * Gets the instance of the {@link IPowerBlockRedstoneManager} for this platform.
     *
     * @return The instance of the {@link IPowerBlockRedstoneManager} for this platform.
     */
    @NotNull IPowerBlockRedstoneManager getPowerBlockRedstoneManager();

    /**
     * Gets the instance of the {@link IChunkManager} for this platform.
     *
     * @return The instance of the {@link IChunkManager} for this platform.
     */
    @NotNull IChunkManager getChunkManager();

    /**
     * Gets the instance of the {@link IDoorActionEventFactory} for this platform.
     *
     * @return The instance of the {@link IDoorActionEventFactory} for this platform.
     */
    @NotNull IDoorActionEventFactory getDoorActionEventFactory();

    /**
     * Calls a {@link IDoorEvent}.
     *
     * @param doorActionEvent The {@link IDoorEvent} to call.
     */
    void callDoorActionEvent(final @NotNull IDoorEvent doorActionEvent);

    /**
     * Checks if a thread is the main thread.
     *
     * @param threadID The ID of the thread to compare.
     * @return True if the thread is the main thread.
     */
    boolean isMainThread(final long threadID);

    /**
     * Constructs a new {@link IPExecutor}.
     *
     * @return A new {@link IPExecutor}.
     */
    @NotNull <T> IPExecutor<T> newPExecutor();

    /**
     * Gets the {@link IGlowingBlockSpawner} for the current platform.
     *
     * @return The {@link IGlowingBlockSpawner} for the current platform.
     */
    @NotNull IGlowingBlockSpawner getGlowingBlockSpawner();
}
