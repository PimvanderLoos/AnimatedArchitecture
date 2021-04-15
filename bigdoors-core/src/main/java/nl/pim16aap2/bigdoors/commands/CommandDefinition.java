package nl.pim16aap2.bigdoors.commands;

import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public enum CommandDefinition
{
    ADD_OWNER("bigdoors.user.addowner", "bigdoors.admin.bypass.addowner"),
    MENU("bigdoors.user.base", "bigdoors.admin.bypass.menu"),
    CANCEL("bigdoors.user.base", null),
    CONFIRM("bigdoors.user.base", null),
    DELETE("bigdoors.user.delete", "bigdoors.admin.bypass.delete"),
    FILL(null, "bigdoors.admin.fill"),
    INFO("bigdoors.user.info", "bigdoors.admin.bypass.info"),
    INSPECTPOWERBLOCK("bigdoors.user.inspect", "bigdoors.admin.bypass.inspect"),
    LISTDOORS("bigdoors.user.listdoors", "bigdoors.admin.bypass.listdoors"),
    MOVEPOWERBLOCK("bigdoors.user.movepowerblock", "bigdoors.admin.bypass.movepowerblock"),
    NEWDOOR("bigdoors.user.newdoor", null),
    REMOVEOWNER("bigdoors.user.removeowner", "bigdoors.admin.bypass.removeowner"),
    RESTART(null, "bigdoors.admin.restart"),
    SETAUTOCLOSETIME("bigdoors.user.setautoclosetime", "bigdoors.admin.bypass.setautoclosetime"),
    SETBLOCKSTOMOVE("bigdoors.user.base", "bigdoors.admin.bypass.setblockstomove"),
    SETNAME("bigdoors.user.setname", null),
    SETROTATION("bigdoors.user.base", "bigdoors.admin.bypass.setrotation"),
    SPECIFY("bigdoors.user.base", null),
    STOPDOORS(null, "bigdoors.admin.stopdoors"),
    VERSION(null, "bigdoors.admin.version"),

    TOGGLE("bigdoors.user.toggle", "bigdoors.admin.bypass.toggle"),
    CLOSE(CommandDefinition.TOGGLE.userPermission.orElse(null), CommandDefinition.TOGGLE.adminPermission.orElse(null)),
    OPEN(CommandDefinition.TOGGLE.userPermission.orElse(null), CommandDefinition.TOGGLE.adminPermission.orElse(null)),
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
