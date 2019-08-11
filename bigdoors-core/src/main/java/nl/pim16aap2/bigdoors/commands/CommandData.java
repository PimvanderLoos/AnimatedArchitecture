package nl.pim16aap2.bigdoors.commands;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum CommandData
{
    BDM("bdm", "bigdoors.user", null),
    BIGDOORS("bigdoors", "bigdoors.user", null),

    ADDOWNER("AddOwner", "bigdoors.user.addowner", CommandData.BIGDOORS),
    CANCEL("cancel", "bigdoors.user", CommandData.BIGDOORS),
    MOVEPOWERBLOCK("movepowerblock", "bigdoors.user.movepowerblock", CommandData.BIGDOORS),
    CLOSE("close", "bigdoors.user.close", CommandData.BIGDOORS),
    DEBUG("debug", "bigdoorsdebug.iknowishouldnotusethis", CommandData.BIGDOORS),
    DELETE("delete", "bigdoors.user.delete", CommandData.BIGDOORS),
    FILLDOOR("filldoor", "bigdoors.admin.filldoor", CommandData.BIGDOORS),
    INFO("info", "bigdoors.user.info", CommandData.BIGDOORS),
    INSPECTPOWERBLOCK("inspectpowerblock", "bigdoors.user.inspectpowerblock", CommandData.BIGDOORS),
    LISTDOORS("listdoors", "bigdoors.user.listdoors", CommandData.BIGDOORS),
    LISTPLAYERDOORS("listplayerdoors", "bigdoors.admin.listplayerdoors", CommandData.BIGDOORS),
    MENU("menu", CommandData.BDM.permission, CommandData.BIGDOORS),
    NEW("new", "bigdoors.user.new", CommandData.BIGDOORS),
    OPEN("open", "bigdoors.user.open", CommandData.BIGDOORS),
    REMOVEOWNER("removeowner", "bigdoors.user.removeowner", CommandData.BIGDOORS),
    RESTART("restart", "bigdoors.admin.restart", CommandData.BIGDOORS),
    SETAUTOCLOSETIME("setautoclosetime", "bigdoors.user.setautoclosetime", CommandData.BIGDOORS),
    SETBLOCKSTOMOVE("setblockstomove", "bigdoors.user.setblockstomove", CommandData.BIGDOORS),
    SETNAME("setname", CommandData.NEW.permission, CommandData.NEW.superCommand),
    SETROTATION("setrotation", "bigdoors.user.setrotation", CommandData.BIGDOORS),
    STOPDOORS("stopdoors", "bigdoors.admin.stopdoors", CommandData.BIGDOORS),
    TOGGLE("toggle", "bigdoors.user.toggle", CommandData.BIGDOORS),
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

    @NotNull
    public static String getPermission(final @NotNull CommandData command)
    {
        return command.permission;
    }

    @Nullable
    public static CommandData getSuperCommand(final @NotNull CommandData command)
    {
        return command.superCommand;
    }

    @NotNull
    public static String getCommandName(final @NotNull CommandData command)
    {
        return command.commandName;
    }
}
