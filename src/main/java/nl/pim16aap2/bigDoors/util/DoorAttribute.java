package nl.pim16aap2.bigDoors.util;

public enum DoorAttribute
{
    LOCK                (2, "bigdoors.user.lock", "bigdoors.admin.bypass.lock"),
    TOGGLE              (2, "bigdoors.user.toggledoor", "bigdoors.admin.bypass.toggle"),
    INFO                (2, "bigdoors.user.doorinfo", "bigdoors.admin.bypass.info"),
    DELETE              (0, "bigdoors.user.delete", "bigdoors.admin.bypass.delete"),
    RELOCATEPOWERBLOCK  (1, "bigdoors.user.relocatepowerblock", "bigdoors.admin.bypass.relocatepowerblock"),
    CHANGETIMER         (1, "bigdoors.user.setautoclosetime", "bigdoors.admin.bypass.changetimer"),
    DIRECTION_STRAIGHT  (1, "bigdoors.user.direction", "bigdoors.admin.bypass.direction"),
    DIRECTION_ROTATE    (1, DIRECTION_STRAIGHT.userPermission, DIRECTION_STRAIGHT.adminPermission),
    BLOCKSTOMOVE        (1, "bigdoors.user.setblockstomove", "bigdoors.admin.bypass.blockstomove"),
    ADDOWNER            (0, "bigdoors.user.addowner", "bigdoors.admin.bypass.addowner"),
    REMOVEOWNER         (0, "bigdoors.user.removeowner", "bigdoors.admin.bypass.removeowner");

    private String userPermission, adminPermission;
    private int permissionLevel;

    private DoorAttribute(int permissionLevel, String userPermission, String adminPermission)
    {
        this.permissionLevel = permissionLevel;
        this.adminPermission = adminPermission;
        this.userPermission = userPermission;
    }

    public static int getPermissionLevel(DoorAttribute atr)
    {
        return atr.permissionLevel;
    }
    
    public static String getUserPermission(DoorAttribute atr)
    {
        return atr.userPermission;
    }
    
    public static String getAdminPermission(DoorAttribute atr)
    {
        return atr.adminPermission;
    }
}
