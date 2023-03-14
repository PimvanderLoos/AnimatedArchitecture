package nl.pim16aap2.animatedarchitecture.core.commands;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Represents the various commands that are used along with their user/admin permission nodes.
 *
 * @author Pim
 */
@ToString(doNotUseGetters = true)
@EqualsAndHashCode
public final class CommandDefinition
{
    /**
     * The prefix of all permissions that a regular user should have access to for normal usage.
     * <p>
     * This will include stuff like creating new structures and managing structures that you own.
     */
    public static final String PREFIX_USER = "animatedarchitecture.user.";

    /**
     * The prefix of all permissions for admin-related actions.
     * <p>
     * This should include stuff like affecting server settings and managing structures owned by other players.
     */
    public static final String PREFIX_ADMIN = "animatedarchitecture.admin.";

    public static final CommandDefinition ADD_OWNER =
        new CommandDefinition("ADD_OWNER",
                              PREFIX_USER + "addowner",
                              PREFIX_ADMIN + "bypass.addowner");

    public static final CommandDefinition MENU =
        new CommandDefinition("MENU",
                              PREFIX_USER + "base",
                              PREFIX_ADMIN + "bypass.menu");

    public static final CommandDefinition CANCEL =
        new CommandDefinition("CANCEL",
                              PREFIX_USER + "base",
                              null);

    public static final CommandDefinition CONFIRM =
        new CommandDefinition("CONFIRM",
                              PREFIX_USER + "base",
                              null);

    public static final CommandDefinition DEBUG =
        new CommandDefinition("DEBUG",
                              null,
                              PREFIX_ADMIN + "debug");

    public static final CommandDefinition DELETE =
        new CommandDefinition("DELETE",
                              PREFIX_USER + "delete",
                              PREFIX_ADMIN + "bypass.delete");

    public static final CommandDefinition INFO =
        new CommandDefinition("INFO",
                              PREFIX_USER + "info",
                              PREFIX_ADMIN + "bypass.info");

    public static final CommandDefinition INSPECT_POWER_BLOCK =
        new CommandDefinition("INSPECT_POWER_BLOCK",
                              PREFIX_USER + "inspect",
                              PREFIX_ADMIN + "bypass.inspect");

    public static final CommandDefinition LIST_STRUCTURES =
        new CommandDefinition("LIST_STRUCTURES",
                              PREFIX_USER + "liststructures",
                              PREFIX_ADMIN + "bypass.liststructures");

    public static final CommandDefinition LOCK =
        new CommandDefinition("LOCK",
                              PREFIX_USER + "lock",
                              PREFIX_ADMIN + "bypass.lock");

    public static final CommandDefinition MOVE_POWER_BLOCK =
        new CommandDefinition("MOVE_POWER_BLOCK",
                              PREFIX_USER + "movepowerblock",
                              PREFIX_ADMIN + "bypass.movepowerblock");

    public static final CommandDefinition NEW_STRUCTURE =
        new CommandDefinition("NEW_STRUCTURE",
                              PREFIX_USER + "newstructure",
                              null);

    public static final CommandDefinition REMOVE_OWNER =
        new CommandDefinition("REMOVE_OWNER",
                              PREFIX_USER + "removeowner",
                              PREFIX_ADMIN + "bypass.removeowner");

    public static final CommandDefinition RESTART =
        new CommandDefinition("RESTART",
                              null,
                              PREFIX_ADMIN + "restart");

    public static final CommandDefinition SET_BLOCKS_TO_MOVE =
        new CommandDefinition("SET_BLOCKS_TO_MOVE",
                              PREFIX_USER + "base",
                              PREFIX_ADMIN + "bypass.setblockstomove");

    public static final CommandDefinition SET_NAME =
        new CommandDefinition("SET_NAME",
                              PREFIX_USER + "setname",
                              null);

    public static final CommandDefinition SET_OPEN_STATUS =
        new CommandDefinition("SET_OPEN_STATUS",
                              PREFIX_USER + "base",
                              PREFIX_ADMIN + "bypass.setopenstatus");

    public static final CommandDefinition SET_OPEN_DIRECTION =
        new CommandDefinition("SET_OPEN_DIRECTION",
                              PREFIX_USER + "base",
                              PREFIX_ADMIN + "bypass.setrotation");

    public static final CommandDefinition SPECIFY =
        new CommandDefinition("SPECIFY",
                              PREFIX_USER + "base",
                              null);

    public static final CommandDefinition UPDATE_CREATOR =
        new CommandDefinition("UPDATE_CREATOR",
                              PREFIX_USER + "base",
                              null);

    public static final CommandDefinition STOP_STRUCTURES =
        new CommandDefinition("STOP_STRUCTURES",
                              null,
                              PREFIX_ADMIN + "stopstructures");

    public static final CommandDefinition VERSION =
        new CommandDefinition("VERSION",
                              null,
                              PREFIX_ADMIN + "version");

    public static final CommandDefinition TOGGLE =
        new CommandDefinition("TOGGLE",
                              PREFIX_USER + "toggle",
                              PREFIX_ADMIN + "bypass.toggle");

    @Getter
    private final String name;
    @Getter
    private final Optional<String> userPermission;
    @Getter
    private final Optional<String> adminPermission;

    public CommandDefinition(String name, @Nullable String userPermission, @Nullable String adminPermission)
    {
        this.name = name;
        this.userPermission = Optional.ofNullable(userPermission);
        this.adminPermission = Optional.ofNullable(adminPermission);
    }

    /**
     * Retrieves the lowest permissions level of the current command.
     *
     * @return The lowest permission level available. If the command has both a user and an admin permission, the user
     * permission will be returned. If it only has an admin permission, that will be returned instead.
     *
     * @throws NullPointerException
     *     When this command has neither a user permission nor an admin permission.
     */
    public String getLowestPermission()
    {
        return Util.requireNonNull(userPermission.orElseGet(() -> adminPermission.orElse(null)),
                                   "Minimum permission for command " + this.name);
    }
}
