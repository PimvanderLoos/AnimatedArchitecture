public enum Command
{
    // cmd enum, Command strings, Permission node, Console, Area-specific,
    BIGDOORS {{"bigdoors", "bdm"}, "bigdoors.user.gui", false, false},
    BDRESTART {{"bdrestart"}, "bigdoors.admin.restart", true, false},
    NAMEDOOR {{"namedoor"}, ""}


    private static HashMap<String, Command> map = new HashMap<String, Command>();
}