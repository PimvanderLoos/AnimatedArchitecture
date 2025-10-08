package nl.pim16aap2.animatedarchitecture.spigot.core.comands;

import cloud.commandframework.context.CommandContext;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Record that holds the name and class of a command parameter with some utility methods.
 *
 * @param name
 *     the name of the parameter.
 * @param clz
 *     the class of the parameter.
 * @param <T>
 *     the type of the parameter.
 */
public record CommandParameterInfo<T>(String name, Class<T> clz)
{
    public static final CommandParameterInfo<StructureRetriever> PARAM_STRUCTURE_RETRIEVER =
        new CommandParameterInfo<>("structureRetriever", StructureRetriever.class);

    public static final CommandParameterInfo<PermissionLevel> PARAM_PERMISSION_LEVEL =
        new CommandParameterInfo<>("permissionLevel", PermissionLevel.class);

    public static final CommandParameterInfo<Player> PARAM_TARGET_PLAYER =
        new CommandParameterInfo<>("targetPlayer", Player.class);

    public static final CommandParameterInfo<Player> PARAM_NEW_OWNER =
        new CommandParameterInfo<>("newOwner", Player.class);

    public static final CommandParameterInfo<String> PARAM_STRUCTURE_NAME =
        new CommandParameterInfo<>("structureName", String.class);

    public static final CommandParameterInfo<Boolean> PARAM_LOCK_STATUS =
        new CommandParameterInfo<>("lockStatus", Boolean.class);

    public static final CommandParameterInfo<Boolean> PARAM_SEND_UPDATED_INFO =
        new CommandParameterInfo<>("sendUpdatedInfo", Boolean.class);

    public static final CommandParameterInfo<Boolean> PARAM_IS_OPEN =
        new CommandParameterInfo<>("isOpen", Boolean.class);

    public static final CommandParameterInfo<MovementDirection> PARAM_DIRECTION =
        new CommandParameterInfo<>("direction", MovementDirection.class);

    public static final CommandParameterInfo<StructureType> PARAM_STRUCTURE_TYPE =
        new CommandParameterInfo<>("structureType", StructureType.class);

    public static final CommandParameterInfo<Integer> PARAM_BLOCKS_TO_MOVE =
        new CommandParameterInfo<>("blocksToMove", Integer.class);

    public static final CommandParameterInfo<String> PARAM_NAME =
        new CommandParameterInfo<>("name", String.class);

    public static final CommandParameterInfo<String> PARAM_DATA =
        new CommandParameterInfo<>("data", String.class);

    public static final CommandParameterInfo<String> PARAM_STEP_NAME =
        new CommandParameterInfo<>("stepName", String.class);

    public static final CommandParameterInfo<String> PARAM_STEP_VALUE =
        new CommandParameterInfo<>("stepValue", String.class);

    /**
     * Gets the parameter from the context, or null if not present.
     *
     * @param context
     *     the command context.
     * @return the parameter value, or null if not present.
     */
    public @Nullable T getNullable(CommandContext<ICommandSender> context)
    {
        return context.getOrDefault(name, null);
    }

    /**
     * Gets the parameter from the context.
     *
     * @param context
     *     the command context.
     * @return the parameter value.
     */
    public T get(CommandContext<ICommandSender> context)
    {
        return context.get(name);
    }

    /**
     * Gets the parameter from the context, or returns the default value if not present.
     *
     * @param context
     *     the command context.
     * @param defaultValue
     *     the default value to return if the parameter is not present.
     * @return the parameter value, or the default value if not present.
     */
    public T getOrDefault(CommandContext<ICommandSender> context, T defaultValue)
    {
        return context.getOrDefault(name, defaultValue);
    }
}
