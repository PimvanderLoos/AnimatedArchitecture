package nl.pim16aap2.bigdoors.commands;

import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Represents the various commands that are used along with their user/admin permission nodes.
 *
 * @author Pim
 */
@ToString
public final class CommandDefinition
{
    private static final String USER = "bigdoors.user.";
    private static final String ADMIN = "bigdoors.admin.";

    public static final CommandDefinition ADD_OWNER = new CommandDefinition("ADD_OWNER",
                                                                            USER + "addowner",
                                                                            ADMIN + "bypass.addowner");
    public static final CommandDefinition MENU = new CommandDefinition("MENU",
                                                                       USER + "base",
                                                                       ADMIN + "bypass.menu");
    public static final CommandDefinition CANCEL = new CommandDefinition("CANCEL",
                                                                         USER + "base",
                                                                         null);
    public static final CommandDefinition CONFIRM = new CommandDefinition("CONFIRM",
                                                                          USER + "base",
                                                                          null);
    public static final CommandDefinition DEBUG = new CommandDefinition("DEBUG",
                                                                        null,
                                                                        ADMIN + "debug");
    public static final CommandDefinition DELETE = new CommandDefinition("DELETE",
                                                                         USER + "delete",
                                                                         ADMIN + "bypass.delete");
    public static final CommandDefinition INFO = new CommandDefinition("INFO",
                                                                       USER + "info",
                                                                       ADMIN + "bypass.info");
    public static final CommandDefinition INSPECT_POWERBLOCK = new CommandDefinition("INSPECT_POWERBLOCK",
                                                                                     USER + "inspect",
                                                                                     ADMIN + "bypass.inspect");
    public static final CommandDefinition LIST_DOORS = new CommandDefinition("LIST_DOORS",
                                                                             USER + "listdoors",
                                                                             ADMIN + "bypass.listdoors");
    public static final CommandDefinition LOCK = new CommandDefinition("LOCK",
                                                                       USER + "lock",
                                                                       ADMIN + "bypass.lock");
    public static final CommandDefinition MOVE_POWERBLOCK = new CommandDefinition("MOVE_POWERBLOCK",
                                                                                  USER + "movepowerblock",
                                                                                  ADMIN + "bypass.movepowerblock");
    public static final CommandDefinition NEW_DOOR = new CommandDefinition("NEW_DOOR",
                                                                           USER + "newdoor",
                                                                           null);
    public static final CommandDefinition REMOVE_OWNER = new CommandDefinition("REMOVE_OWNER",
                                                                               USER + "removeowner",
                                                                               ADMIN + "bypass.removeowner");
    public static final CommandDefinition RESTART = new CommandDefinition("RESTART",
                                                                          null,
                                                                          ADMIN + "restart");
    public static final CommandDefinition SET_AUTO_CLOSE_TIME = new CommandDefinition("SET_AUTO_CLOSE_TIME",
                                                                                      USER + "setautoclosetime",
                                                                                      ADMIN +
                                                                                          "bypass.setautoclosetime");
    public static final CommandDefinition SET_BLOCKS_TO_MOVE = new CommandDefinition("SET_BLOCKS_TO_MOVE",
                                                                                     USER + "base",
                                                                                     ADMIN + "bypass.setblockstomove");
    public static final CommandDefinition SET_NAME = new CommandDefinition("SET_NAME",
                                                                           USER + "setname",
                                                                           null);
    public static final CommandDefinition SET_OPEN_DIR = new CommandDefinition("SET_OPEN_DIR",
                                                                               USER + "base",
                                                                               ADMIN + "bypass.setrotation");
    public static final CommandDefinition SPECIFY = new CommandDefinition("SPECIFY",
                                                                          USER + "base",
                                                                          null);
    public static final CommandDefinition STOP_DOORS = new CommandDefinition("STOP_DOORS",
                                                                             null,
                                                                             ADMIN + "stopdoors");
    public static final CommandDefinition VERSION = new CommandDefinition("VERSION",
                                                                          null,
                                                                          ADMIN + "version");
    public static final CommandDefinition TOGGLE = new CommandDefinition("TOGGLE",
                                                                         USER + "toggle",
                                                                         ADMIN + "bypass.toggle");

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
}
