package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.IGuiFactory;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
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
    private final IGuiFactory guiFactory;
    private final @Nullable IPPlayer source;

    @AssistedInject //
    Menu(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        IGuiFactory guiFactory, @Assisted @Nullable IPPlayer source)
    {
        super(commandSender, localizer, textFactory);
        this.guiFactory = guiFactory;
        this.source = source;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.MENU;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    protected CompletableFuture<?> executeCommand(PermissionsStatus permissions)
    {
        // You need the bypass permission to open menus that aren't your own.
        if (!permissions.hasAdminPermission() && source != null && !getCommandSender().equals(source))
        {
            getCommandSender().sendError(textFactory, localizer.getMessage("commands.menu.no_permission_for_others"));
            return CompletableFuture.completedFuture(null);
        }

        getCommandSender().getPlayer().ifPresent(player -> guiFactory.newGUI(player, source));
        return CompletableFuture.completedFuture(null);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link Menu} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for opening the menu.
         *     <p>
         *     This is the entity to which the menu will be shown, and as such this must be an {@link IPPlayer} (menus
         *     aren't supported for servers/command blocks).
         * @param source
         *     The {@link IPPlayer} whose movables will be used.
         *     <p>
         *     When this is null (default), the command sender's own movables will be used.
         * @return See {@link BaseCommand#run()}.
         */
        Menu newMenu(ICommandSender commandSender, @Nullable IPPlayer source);

        /**
         * See {@link #newMenu(ICommandSender, IPPlayer)}.
         */
        default Menu newMenu(ICommandSender commandSender)
        {
            return newMenu(commandSender, null);
        }
    }
}
