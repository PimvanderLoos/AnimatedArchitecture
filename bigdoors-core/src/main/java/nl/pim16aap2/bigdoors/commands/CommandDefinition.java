package nl.pim16aap2.bigdoors.commands;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Represents the various commands that are used along with their user/admin permission nodes.
 *
 * @author Pim
 */
@ToString
public enum CommandDefinition
{
    ADD_OWNER("bigdoors.user.addowner", "bigdoors.admin.bypass.addowner"),
    MENU("bigdoors.user.base", "bigdoors.admin.bypass.menu"),
    CANCEL("bigdoors.user.base", null),
    CONFIRM("bigdoors.user.base", null),
    DEBUG(null, "bigdoors.admin.debug"),
    DELETE("bigdoors.user.delete", "bigdoors.admin.bypass.delete"),
    INFO("bigdoors.user.info", "bigdoors.admin.bypass.info"),
    INSPECT_POWERBLOCK("bigdoors.user.inspect", "bigdoors.admin.bypass.inspect"),
    LIST_DOORS("bigdoors.user.listdoors", "bigdoors.admin.bypass.listdoors"),
    MOVE_POWERBLOCK("bigdoors.user.movepowerblock", "bigdoors.admin.bypass.movepowerblock"),
    NEW_DOOR("bigdoors.user.newdoor", null),
    REMOVE_OWNER("bigdoors.user.removeowner", "bigdoors.admin.bypass.removeowner"),
    RESTART(null, "bigdoors.admin.restart"),
    SET_AUTO_CLOSE_TIME("bigdoors.user.setautoclosetime", "bigdoors.admin.bypass.setautoclosetime"),
    SET_BLOCKS_TO_MOVE("bigdoors.user.base", "bigdoors.admin.bypass.setblockstomove"),
    SET_NAME("bigdoors.user.setname", null),
    SET_OPEN_DIR("bigdoors.user.base", "bigdoors.admin.bypass.setrotation"),
    SPECIFY("bigdoors.user.base", null),
    STOP_DOORS(null, "bigdoors.admin.stopdoors"),
    VERSION(null, "bigdoors.admin.version"),
    TOGGLE("bigdoors.user.toggle", "bigdoors.admin.bypass.toggle"),
    ;

    @Getter
    private final @NonNull Optional<String> userPermission;
    @Getter
    private final @NonNull Optional<String> adminPermission;

    CommandDefinition(final @Nullable String userPermission, final @Nullable String adminPermission)
    {
        this.userPermission = Optional.ofNullable(userPermission);
        this.adminPermission = Optional.ofNullable(adminPermission);
    }
}
