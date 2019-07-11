package nl.pim16aap2.bigdoors.util.messages;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a localizable message.
 *
 * @author Pim
 */
public enum Message
{
    DOORTYPE_BIGDOOR ("DOORTYPE.BIGDOOR"),
    DOORTYPE_DRAWBRIDGE ("DOORTYPE.DRAWBRIDGE"),
    DOORTYPE_PORTCULLIS ("DOORTYPE.PORTCULLIS"),
    DOORTYPE_ELEVATOR ("DOORTYPE.ELEVATOR"),
    DOORTYPE_SLIDINGDOOR ("DOORTYPE.SLIDINGDOOR"),
    DOORTYPE_FLAG ("DOORTYPE.FLAG"),
    DOORTYPE_GARAGEDOOR ("DOORTYPE.GARAGEDOOR"),
    DOORTYPE_WINDMILL ("DOORTYPE.WINDMILL"),
    DOORTYPE_REVOLVINGDOOR ("DOORTYPE.REVOLVINGDOOR"),
    DOORTYPE_CLOCK ("DOORTYPE.CLOCK"),

    ERROR_TOOMANYDOORSOWNED (""),
    ERROR_INVALIDDOORNAME (""),
    ERROR_INVALIDDOORID (""),
    ERROR_DOORTYPEDISABLED (""),
    ERROR_DOORALREADYCLOSED (""),
    ERROR_DOORALREADYOPEN (""),
    ERROR_TOGGLEFAILURE (""),
    ERROR_NOOPENDIRECTION (""),
    ERROR_DOORISBUSY (""),
    ERROR_NOTOGGLEPERMISSION (""),
    ERROR_NODOORSFOUND (""),
    ERROR_TOOMANYDOORSFOUND (""),
    ERROR_PLAYERISBUSY (""),
    ERROR_PLAYERISNOTBUSY (""),
    ERROR_INVALIDINPUT (""),
    ERROR_PLAYERNOTFOUND (""),
    ERROR_GENERALERROR (""),
    ERROR_NOPERMISSIONFORDOORTYPE (""),
    ERROR_NOPERMISSIONFORCOMMAND (""),
    ERROR_NOPERMISSIONFORLOCATION (""),
    ERROR_NOPERMISSIONFORACTION (""),
    ERROR_COMMAND_INVALIDPERMISSIONVALUE (""),
    ERROR_COMMAND_NOTAPLAYER (""),
    ERROR_COMMAND_NOTFOUND (""),

    GUI_PAGE_DOORLIST (""),
    GUI_PAGE_SUBMENU (""),
    GUI_PAGE_CONFIRM (""),
    GUI_PAGE_NEWDOORS (""),
    GUI_PAGE_REMOVEOWNER (""),

    GUI_SORTING_ALPHABETICAL (""),
    GUI_SORTING_NUMERICAL (""),
    GUI_SORTING_TYPICAL (""),

    GUI_BUTTON_SORT (""),
    GUI_BUTTON_NEXTPAGE (""),
    GUI_BUTTON_PREVIOUSPAGE (""),
    GUI_BUTTON_NEW (""),
    GUI_BUTTON_LOCK (""),
    GUI_BUTTON_UNLOCK (""),
    GUI_BUTTON_TOGGLE (""),
    GUI_BUTTON_INFO (""),
    GUI_BUTTON_RELOCATEPB (""),
    GUI_BUTTON_DELETE_DOOR (""),
    GUI_BUTTON_DELETE_DOOR_CONFIRM (""),
    GUI_BUTTON_DELETE_DOOR_CANCEL (""),
    GUI_BUTTON_BLOCKSTOMOVE (""),
    GUI_BUTTON_DIRECTION (""),
    GUI_BUTTON_TIMER (""),
    GUI_BUTTON_OWNER_ADD (""),
    GUI_BUTTON_OWNER_DELETE (""),

    GUI_DESCRIPTION_INITIATION (""),
    GUI_DESCRIPTION_DOORID (""),
    GUI_DESCRIPTION_DELETE_DOOR (""),
    GUI_DESCRIPTION_BLOCKSTOMOVE (""),
    GUI_DESCRIPTION_TIMER_SET (""),
    GUI_DESCRIPTION_TIMER_NOTSET (""),
    GUI_DESCRIPTION_DIRECTION (""),

    GENERAL_DIRECTION_CLOCKWISE (""),
    GENERAL_DIRECTION_COUNTERCLOCKWISE (""),
    GENERAL_DIRECTION_NORTH (""),
    GENERAL_DIRECTION_EAST (""),
    GENERAL_DIRECTION_SOUTH (""),
    GENERAL_DIRECTION_WEST (""),
    GENERAL_DIRECTION_UP (""),
    GENERAL_DIRECTION_DOWN (""),
    GENERAL_DIRECTION_NONE (""),

    COMMAND_TIMEOUTORFAIL (""),
    COMMAND_ADDOWNER_INIT (""),
    COMMAND_ADDOWNER_SUCCESS (""),
    COMMAND_ADDOWNER_FAIL (""),
    COMMAND_REMOVEOWNER_INIT (""),
    COMMAND_REMOVEOWNER_SUCCESS (""),
    COMMAND_REMOVEOWNER_FAIL (""),
    COMMAND_REMOVEOWNER_LIST (""),
    COMMAND_SETTIME_INIT (""),
    COMMAND_SETTIME_SUCCESS (""),
    COMMAND_SETTIME_DISABLED (""),
    COMMAND_BLOCKSTOMOVE_INIT (""),
    COMMAND_BLOCKSTOMOVE_SUCCESS (""),
    COMMAND_BLOCKSTOMOVE_DISABLED (""),
    COMMAND_DELETE_SUCCESS (""),

    CREATOR_GENERAL_GIVENAME (""),
    CREATOR_GENERAL_INVALIDPOINT (""),
    CREATOR_GENERAL_INVALIDROTATIONPOINT (""),
    CREATOR_GENERAL_INVALIDROTATIONDIRECTION (""),
    CREATOR_GENERAL_INSUFFICIENTFUNDS (""),
    CREATOR_GENERAL_MONEYWITHDRAWN (""),
    CREATOR_GENERAL_TIMEOUT (""),
    CREATOR_GENERAL_CANCELLED (""),
    CREATOR_GENERAL_STICKNAME (""),
    CREATOR_GENERAL_AREATOOBIG (""),
    CREATOR_GENERAL_INIT (""),

    CREATOR_PBRELOCATOR_STICKLORE (""),
    CREATOR_PBRELOCATOR_INIT (""),
    CREATOR_PBRELOCATOR_SUCCESS (""),
    CREATOR_PBRELOCATOR_LOCATIONINUSE (""),

    CREATOR_PBINSPECTOR_STICKLORE (""),
    CREATOR_PBINSPECTOR_INIT (""),

    CREATOR_PORTCULLIS_STICKLORE (""),
    CREATOR_PORTCULLIS_INIT (""),
    CREATOR_PORTCULLIS_SUCCESS (""),
    CREATOR_PORTCULLIS_STEP1 (""),
    CREATOR_PORTCULLIS_STEP2 (""),

    CREATOR_DRAWBRIDGE_STICKLORE (""),
    CREATOR_DRAWBRIDGE_INIT (""),
    CREATOR_DRAWBRIDGE_SUCCESS (""),
    CREATOR_DRAWBRIDGE_STEP1 (""),
    CREATOR_DRAWBRIDGE_STEP2 (""),
    CREATOR_DRAWBRIDGE_STEP3 (""),
    CREATOR_DRAWBRIDGE_STEP4 (""),

    CREATOR_ELEVATOR_STICKLORE (""),
    CREATOR_ELEVATOR_INIT (""),
    CREATOR_ELEVATOR_SUCCESS (""),
    CREATOR_ELEVATOR_STEP1 (""),
    CREATOR_ELEVATOR_STEP2 (""),

    CREATOR_SLIDINGDOOR_STICKLORE (""),
    CREATOR_SLIDINGDOOR_INIT (""),
    CREATOR_SLIDINGDOOR_SUCCESS (""),
    CREATOR_SLIDINGDOOR_STEP1 (""),
    CREATOR_SLIDINGDOOR_STEP2 (""),

    CREATOR_BIGDOOR_STICKLORE (""),
    CREATOR_BIGDOOR_INIT (""),
    CREATOR_BIGDOOR_SUCCESS (""),
    CREATOR_BIGDOOR_STEP1 (""),
    CREATOR_BIGDOOR_STEP2 (""),
    CREATOR_BIGDOOR_STEP3 (""),

    CREATOR_FLAG_STICKLORE (""),
    CREATOR_FLAG_INIT (""),
    CREATOR_FLAG_SUCCESS (""),
    CREATOR_FLAG_STEP1 (""),
    CREATOR_FLAG_STEP2 (""),
    CREATOR_FLAG_STEP3 (""),

    CREATOR_WINDMILL_STICKLORE (""),
    CREATOR_WINDMILL_INIT (""),
    CREATOR_WINDMILL_SUCCESS (""),
    CREATOR_WINDMILL_STEP1 (""),
    CREATOR_WINDMILL_STEP2 (""),
    CREATOR_WINDMILL_STEP3 (""),

    CREATOR_REVOLVINGDOOR_STICKLORE (""),
    CREATOR_REVOLVINGDOOR_INIT (""),
    CREATOR_REVOLVINGDOOR_SUCCESS (""),
    CREATOR_REVOLVINGDOOR_STEP1 (""),
    CREATOR_REVOLVINGDOOR_STEP2 (""),
    CREATOR_REVOLVINGDOOR_STEP3 (""),

    CREATOR_GARAGEDOOR_STICKLORE (""),
    CREATOR_GARAGEDOOR_INIT (""),
    CREATOR_GARAGEDOOR_SUCCESS (""),
    CREATOR_GARAGEDOOR_STEP1 (""),
    CREATOR_GARAGEDOOR_STEP2 (""),
    CREATOR_GARAGEDOOR_STEP3 (""),



    ;

    private final String[] variableNames;
    private final String key;

    /**
     * Constructs a message.
     * 
     * @param key           The name of the key.
     * @param variableNames The names of the variables in the value that can be
     *                      replaced.
     */
    Message(@NotNull final String key, final String... variableNames)
    {
        this.variableNames = variableNames;
        this.key = key;
    }

    /**
     * Gets the name of the variable at the given position for the given message.
     * 
     * @param msg The message for which to retrieve the variable name.
     * @param idx The index of the variable name.
     * @return The name of the variable at the given position of this message.
     */
    public static String getVariableName(Message msg, int idx)
    {
        return msg.variableNames[idx];
    }

    /**
     * Gets the names of the variables for the given message..
     *
     * @param msg The message for which to retrieve the variable names.
     * @return The names of the variables of this message.
     */
    public static String[] getVariableNames(Message msg)
    {
        return msg.variableNames;
    }

    /**
     * Gets the key of the message.
     * 
     * @param msg The message to retrieve the key for.
     * @return The key of the message.
     */
    public static String getKey(Message msg)
    {
        return msg.key;
    }

    /**
     * Gets the number of variables in this message that can be substituted.
     * 
     * @param msg The message to retrieve the variable count for.
     * @return The number of variables in this message that can be substituted.
     */
    public static int getVariableCount(Message msg)
    {
        return msg.variableNames.length;
    }
}
