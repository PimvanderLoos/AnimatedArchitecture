package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a command and its execution.
 */
public interface ICommand
{
    /**
     * Executes a command.
     *
     * @param sender The {@link CommandSender} that executed the command.
     * @param cmd    The command.
     * @param label  The label of the command.
     * @param args   The arguments of the command.
     * @return True if execution of the command was successful.
     *
     * @throws CommandSenderNotPlayerException  When the {@link CommandSender} should have been a player, but wasn't.
     * @throws CommandPermissionException       When the {@link CommandSender} does not have the required permissions to
     *                                          execute the command.
     * @throws IllegalArgumentException         When at least one argument was illegal.
     * @throws CommandPlayerNotFoundException   When a {@link Player} specified in the arguments was not found.
     * @throws CommandActionNotAllowedException When the action associated with the command was not allowed.
     */
    boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd, final @NotNull String label,
                      final @NotNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, IllegalArgumentException,
               CommandPlayerNotFoundException, CommandActionNotAllowedException;

    /**
     * Gets the help message of the command if the {@link CommandSender} has access to it.
     *
     * @param sender The {@link CommandSender}.
     * @return The help message of the command if the {@link CommandSender} has access to it.
     */
    @NotNull
    String getHelp(final @NotNull CommandSender sender);

    /**
     * Gets the permission node of the command.
     *
     * @return The permission node of the command.
     */
    @NotNull
    String getPermission();

    /**
     * Gets the name node of the command.
     *
     * @return The name node of the command.
     */
    @NotNull
    String getName();

    /**
     * Gets the minimum number of arguments of the command.
     *
     * @return The minimum number of arguments of the command.
     */
    int getMinArgCount();

    /**
     * Gets the {@link CommandData} of the command.
     *
     * @return The {@link CommandData} of the command.
     */
    @NotNull
    CommandData getCommandData();
}
