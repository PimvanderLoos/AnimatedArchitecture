package nl.pim16aap2.bigdoors.spigot.commands;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum CommandData
{
    BDM("bdm", "bigdoors.user", null),
    BIGDOORS("bigdoors", "bigdoors.user", null),

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
    CLOSE("close", CommandData.TOGGLE.permission, CommandData.BIGDOORS),
    VERSION("version", "bigdoors.admin.version", CommandData.BIGDOORS);

    private final String commandName;
    private final String permission;
    private final CommandData superCommand;

    CommandData(final @NotNull String commandName, final @NotNull String permission,
                final @Nullable CommandData superCommand)
    {
        this.superCommand = superCommand;
        this.permission = permission;
        this.commandName = commandName;
    }

    public static @NotNull String getPermission(final @NotNull CommandData command)
    {
        return command.permission;
    }

    public static @Nullable CommandData getSuperCommand(final @NotNull CommandData command)
    {
        return command.superCommand;
    }

    public static @NotNull String getCommandName(final @NotNull CommandData command)
    {
        return command.commandName;
    }
}
