package nl.pim16aap2.bigdoors.commands;

import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public enum CommandDefinition
{
    ADD_OWNER("bigdoors.user.addowner", "bigdoors.admin.addowner"),
    ;

    @Getter
    private final @NonNull String permission;
    @Getter
    private final @NonNull Optional<String> adminPermission;

    CommandDefinition(@NonNull String permission)
    {
        this(permission, null);
    }

    CommandDefinition(@NonNull String permission, @Nullable String adminPermission)
    {
        this.permission = permission;
        this.adminPermission = Optional.ofNullable(adminPermission);
    }
}
