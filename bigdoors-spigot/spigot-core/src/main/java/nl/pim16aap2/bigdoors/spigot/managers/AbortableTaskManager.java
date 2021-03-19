package nl.pim16aap2.bigdoors.spigot.managers;

import com.google.common.base.Preconditions;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandAddOwner;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandRemoveOwner;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandSetAutoCloseTime;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandSetBlocksToMove;
import nl.pim16aap2.bigdoors.spigot.util.AbortableTask;
import nl.pim16aap2.bigdoors.spigot.waitforcommand.WaitForAddOwner;
import nl.pim16aap2.bigdoors.spigot.waitforcommand.WaitForRemoveOwner;
import nl.pim16aap2.bigdoors.spigot.waitforcommand.WaitForSetBlocksToMove;
import nl.pim16aap2.bigdoors.spigot.waitforcommand.WaitForSetTime;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public final class AbortableTaskManager
{
    private static AbortableTaskManager INSTANCE;
    @NotNull
    private final BigDoorsSpigot plugin;

    private AbortableTaskManager(final @NotNull BigDoorsSpigot plugin)
    {
        this.plugin = plugin;
    }

    /**
     * Initializes the {@link AbortableTaskManager}. If it has already been initialized, it'll return that instance
     * instead.
     *
     * @param plugin The spigot core.
     * @return The instance of this {@link AbortableTaskManager}.
     */
    public static @NotNull AbortableTaskManager init(final @NotNull BigDoorsSpigot plugin)
    {
        return (INSTANCE == null) ? INSTANCE = new AbortableTaskManager(plugin) : INSTANCE;
    }

    /**
     * Gets the instance of the {@link AbortableTaskManager} if it exists.
     *
     * @return The instance of the {@link AbortableTaskManager}.
     */
    public static @NotNull AbortableTaskManager get()
    {
        Preconditions.checkState(INSTANCE != null,
                                 "Instance has not yet been initialized. Be sure #init() has been invoked");
        return INSTANCE;
    }

    /**
     * Starts the timer for an {@link AbortableTask}. The {@link AbortableTask} will be aborted after the provided
     * amount of time (in seconds).
     *
     * @param abortableTask The {@link AbortableTask}.
     * @param time          The amount of time (in seconds).
     */
    public void startTimerForAbortableTask(final @NotNull AbortableTask abortableTask, int time)
    {
        BukkitTask task = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                abortableTask.abort(false);
            }
        }.runTaskLater(plugin, time);
        abortableTask.setTask(task);
    }

    /**
     * Starts the {@link WaitForSetTime} process for a given player and {@link AbstractDoorBase}.
     *
     * @param player The player.
     * @param door   The {@link AbstractDoorBase}.
     */
    public void startTimerSetter(final @NotNull Player player, final @NotNull AbstractDoorBase door)
    {
        startTimerForAbortableTask(
            new WaitForSetTime(plugin, (SubCommandSetAutoCloseTime) plugin.getCommand(CommandData.SETAUTOCLOSETIME),
                               player, door), 20 * 20);
    }

    /**
     * Starts the {@link WaitForSetBlocksToMove} process for a given player and {@link AbstractDoorBase}.
     *
     * @param player The player.
     * @param door   The {@link AbstractDoorBase}.
     */
    public void startBlocksToMoveSetter(final @NotNull Player player, final @NotNull AbstractDoorBase door)
    {
        startTimerForAbortableTask(new WaitForSetBlocksToMove(plugin, (SubCommandSetBlocksToMove) plugin
            .getCommand(CommandData.SETBLOCKSTOMOVE), player, door), 20 * 20);
    }

    /**
     * Starts the {@link WaitForAddOwner} process for a given player and {@link AbstractDoorBase}.
     *
     * @param player The player.
     * @param door   The {@link AbstractDoorBase}.
     */
    public void startAddOwner(final @NotNull Player player, final @NotNull AbstractDoorBase door)
    {
        startTimerForAbortableTask(
            new WaitForAddOwner(plugin, (SubCommandAddOwner) plugin.getCommand(CommandData.ADDOWNER), player, door),
            20 * 20);
    }

    /**
     * Starts the {@link WaitForRemoveOwner} process for a given player and {@link AbstractDoorBase}.
     *
     * @param player The player.
     * @param door   The {@link AbstractDoorBase}.
     */
    public void startRemoveOwner(final @NotNull Player player, final @NotNull AbstractDoorBase door)
    {
        startTimerForAbortableTask(
            new WaitForRemoveOwner(plugin, (SubCommandRemoveOwner) plugin.getCommand(CommandData.REMOVEOWNER),
                                   player, door), 20 * 20);
    }
}
