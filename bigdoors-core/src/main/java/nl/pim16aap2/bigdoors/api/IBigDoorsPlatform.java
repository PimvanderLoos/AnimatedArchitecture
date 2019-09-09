package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.api.factories.IDoorActionEventFactory;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorActionEvent;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import org.jetbrains.annotations.NotNull;

public interface IBigDoorsPlatform
{
    /**
     * Gets the instance of the {@link IPLocationFactory} for this platform.
     *
     * @return The instance of the {@link IPLocationFactory} for this platform.
     */
    @NotNull
    IPLocationFactory getPLocationFactory();

    /**
     * Gets the instance of the {@link IPWorldFactory} for this platform.
     *
     * @return The instance of the {@link IPWorldFactory} for this platform.
     */
    @NotNull
    IPWorldFactory getPWorldFactory();

    /**
     * Gets the instance of the {@link IPBlockDataFactory} for this platform.
     *
     * @return The instance of the {@link IPBlockDataFactory} for this platform.
     */
    @NotNull
    IPBlockDataFactory getPBlockDataFactory();

    /**
     * Gets the instance of the {@link IFallingBlockFactory} for this platform.
     *
     * @return The instance of the {@link IFallingBlockFactory} for this platform.
     */
    @NotNull
    IFallingBlockFactory getFallingBlockFactory();

    /**
     * Gets the instance of the {@link IPPlayerFactory} for this platform.
     *
     * @return The instance of the {@link IPPlayerFactory} for this platform.
     */
    @NotNull
    IPPlayerFactory getPPlayerFactory();

    /**
     * Gets the instance of the {@link IConfigLoader} for this platform.
     *
     * @return The instance of the {@link IConfigLoader} for this platform.
     */
    @NotNull
    IConfigLoader getConfigLoader();

    /**
     * Gets the instance of the {@link ISoundEngine} for this platform.
     *
     * @return The instance of the {@link ISoundEngine} for this platform.
     */
    @NotNull
    ISoundEngine getSoundEngine();

    /**
     * Gets the instance of the {@link IMessagingInterface} for this platform.
     *
     * @return The instance of the {@link IMessagingInterface} for this platform.
     */
    @NotNull
    IMessagingInterface getMessagingInterface();

    /**
     * Gets the instance of the {@link Messages} for this platform.
     *
     * @return The instance of the {@link Messages} for this platform.
     */
    @NotNull
    Messages getMessages();

    /**
     * Gets the instance of the {@link IBlockAnalyzer} for this platform.
     *
     * @return The instance of the {@link IBlockAnalyzer} for this platform.
     */
    @NotNull
    IBlockAnalyzer getBlockAnalyzer();

    /**
     * Gets the instance of the {@link IChunkManager} for this platform.
     *
     * @return The instance of the {@link IChunkManager} for this platform.
     */
    @NotNull
    IChunkManager getChunkManager();

    /**
     * Gets the instance of the {@link IDoorActionEventFactory} for this platform.
     *
     * @return The instance of the {@link IDoorActionEventFactory} for this platform.
     */
    @NotNull
    IDoorActionEventFactory getDoorActionEventFactory();

    /**
     * Calls a {@link IDoorActionEvent}.
     *
     * @param doorActionEvent The {@link IDoorActionEvent} to call.
     */
    void callDoorActionEvent(final @NotNull IDoorActionEvent doorActionEvent);

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
}
