package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;
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
    final @Nullable IPPlayer target;

    /**
     * Opens the menu for the commandsender, showing the doors of the target if one is provided.
     * <p>
     * If no target is provided, the commandsender itself will be used as the target.
     *
     * @param commandSender
     * @param target
     */
    public Menu(final @NonNull ICommandSender commandSender, final @Nullable IPPlayer target)
    {
        super(commandSender);
        this.target = target;
    }

    public Menu(final @NonNull ICommandSender commandSender)
    {
        this(commandSender, null);
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.MENU;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> executeCommand(final @NonNull BooleanPair permissions)
    {
        throw new UnsupportedOperationException("This command has not yet been implemented!");
    }
}
