package nl.pim16aap2.bigdoors.spigot.commands;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public enum CommandData
{
    BDM("bdm", "bigdoors.user.base", null),
    BIGDOORS("bigdoors", "bigdoors.user.base", null),

    ADDOWNER("AddOwner", "bigdoors.user.addowner", CommandData.BIGDOORS),
    CANCEL("cancel", CommandData.BDM.permission, CommandData.BIGDOORS),
    CONFIRM("confirm", CommandData.BDM.permission, CommandData.BIGDOORS),
    MOVEPOWERBLOCK("movepowerblock", "bigdoors.user.movepowerblock", CommandData.BIGDOORS),
    DEBUG("debug", "bigdoorsdebug.iknowishouldnotusethis", CommandData.BIGDOORS),
    DELETE("delete", "bigdoors.user.delete", CommandData.BIGDOORS),
    FILLDOOR("filldoor", "bigdoors.admin.filldoor", CommandData.BIGDOORS),
    INFO("info", CommandData.BDM.permission, CommandData.BIGDOORS),
    INSPECTPOWERBLOCK("inspectpowerblock", "bigdoors.user.inspectpowerblock", CommandData.BIGDOORS),
    LISTDOORS("listdoors", "bigdoors.user.listdoors", CommandData.BIGDOORS),
    LISTPLAYERDOORS("listplayerdoors", "bigdoors.admin.listplayerdoors", CommandData.BIGDOORS),
    MENU("menu", CommandData.BDM.permission, CommandData.BIGDOORS),
    NEW("new", CommandData.BDM.permission, CommandData.BIGDOORS),
    REMOVEOWNER("removeowner", "bigdoors.user.removeowner", CommandData.BIGDOORS),
    RESTART("restart", "bigdoors.admin.restart", CommandData.BIGDOORS),
    SETAUTOCLOSETIME("setautoclosetime", "bigdoors.user.setautoclosetime", CommandData.BIGDOORS),
    SETBLOCKSTOMOVE("setblockstomove", "bigdoors.user.setblockstomove", CommandData.BIGDOORS),
    SETNAME("setname", CommandData.NEW.permission, CommandData.NEW.superCommand),
    SETROTATION("setrotation", CommandData.BDM.permission, CommandData.BIGDOORS),
    STOPDOORS("stopdoors", "bigdoors.admin.stopdoors", CommandData.BIGDOORS),
    TOGGLE("toggle", "bigdoors.user.toggle", CommandData.BIGDOORS),
    OPEN("open", CommandData.TOGGLE.permission, CommandData.BIGDOORS),
    IDENTIFY("specify", CommandData.BDM.permission, CommandData.BIGDOORS),
    CLOSE("close", CommandData.TOGGLE.permission, CommandData.BIGDOORS),
    VERSION("version", "bigdoors.admin.version", CommandData.BIGDOORS);

    private final String commandName;
    private final String permission;
    private final CommandData superCommand;

    CommandData(final @NonNull String commandName, final @NonNull String permission,
                final @Nullable CommandData superCommand)
    {
        this.superCommand = superCommand;
        this.permission = permission;
        this.commandName = commandName;
    }

    public static @NonNull String getPermission(final @NonNull CommandData command)
    {
        return command.permission;
    }

    public static @Nullable CommandData getSuperCommand(final @NonNull CommandData command)
    {
        return command.superCommand;
    }

    public static @NonNull String getCommandName(final @NonNull CommandData command)
    {
        return command.commandName;
    }
}
