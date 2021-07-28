package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the menu command, which is used to open the menu for a user.
 *
 * @author Pim
 */
@ToString
public class Menu extends BaseCommand
{
    private final @Nullable IPPlayer target;

    protected Menu(final @NotNull ICommandSender commandSender, final @Nullable IPPlayer target)
    {
        super(commandSender);
        this.target = target;
    }

    /**
     * Runs the {@link Menu} command.
     *
     * @param commandSender The {@link ICommandSender} responsible for opening the menu.
     *                      <p>
     *                      This is the entity to which the menu will be shown, and as such this must be an {@link
     *                      IPPlayer} (menus aren't supported for servers/command blocks).
     * @param target        The {@link IPPlayer} whose doors will be used.
     *                      <p>
     *                      When this is null (default), the command sender's own doors will be used.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NotNull CompletableFuture<Boolean> run(final @NotNull ICommandSender commandSender,
                                                          final @Nullable IPPlayer target)
    {
        return new Menu(commandSender, target).run();
    }

    /**
     * Runs the {@link Menu} command.
     * <p>
     * See {@link #run(ICommandSender, IPPlayer)}.
     *
     * @return See {@link BaseCommand#run()}.
     */
    public static @NotNull CompletableFuture<Boolean> run(final @NotNull ICommandSender commandSender)
    {
        return run(commandSender, null);
    }

    @Override
    public @NotNull CommandDefinition getCommand()
    {
        return CommandDefinition.MENU;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    protected @NotNull CompletableFuture<Boolean> executeCommand(final @NotNull BooleanPair permissions)
    {
        // You need the bypass permission to open menus that aren't your own.
        if (!permissions.second && !getCommandSender().equals(target))
            return CompletableFuture.completedFuture(false);

        throw new UnsupportedOperationException("This command has not yet been implemented!");
    }
}
