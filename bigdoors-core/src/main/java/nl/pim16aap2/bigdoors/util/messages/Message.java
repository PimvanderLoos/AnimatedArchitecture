package nl.pim16aap2.bigdoors.util.messages;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a localizable message.
 *
 * @author Pim
 */
public enum Message implements MessageVariable
{
    EMPTY(),

    DOORTYPE_BIGDOOR(),
    DOORTYPE_DRAWBRIDGE(),
    DOORTYPE_PORTCULLIS(),
    DOORTYPE_ELEVATOR(),
    DOORTYPE_SLIDINGDOOR(),
    DOORTYPE_FLAG(),
    DOORTYPE_GARAGEDOOR(),
    DOORTYPE_WINDMILL(),
    DOORTYPE_REVOLVINGDOOR(),
    DOORTYPE_CLOCK(),

    ERROR_TOOMANYDOORSOWNED(DOORLIMIT),
    ERROR_INVALIDDOORNAME(INPUT),
    ERROR_INVALIDDOORID(INPUT),
    ERROR_DOORALREADYCLOSED(DOOR),
    ERROR_DOORALREADYOPEN(DOOR),
    ERROR_TOGGLEFAILURE(DOOR),
    ERROR_TOGGLECANCELLED(DOOR),
    ERROR_NOOPENDIRECTION(DOOR),
    ERROR_DOORISBUSY(DOOR),
    ERROR_DOORISLOCKED(DOOR),
    ERROR_DOORTOOBIG(DOOR),
    ERROR_NOTOGGLEPERMISSION(DOOR),
    ERROR_TOOMANYDOORSFOUND(DOOR),
    ERROR_DOORISOBSTRUCTED(DOOR),
    ERROR_NODOORSFOUND(),
    ERROR_PLAYERISBUSY(),
    ERROR_PLAYERISNOTBUSY(),
    ERROR_INVALIDINPUT(INPUT),
    ERROR_PLAYERNOTFOUND(INPUT),
    ERROR_GENERALERROR(),
    ERROR_NOPERMISSIONFORDOORTYPE(DOORTYPE),
    ERROR_DOORTYPEDISABLED(DOORTYPE),
    ERROR_NOPERMISSIONFORLOCATION(HOOKNAME),
    ERROR_NOPERMISSIONFORACTION(),
    ERROR_COMMAND_NOPERMISSION(),
    ERROR_COMMAND_INVALIDPERMISSIONVALUE(INPUT),
    ERROR_COMMAND_NOTAPLAYER(),
    ERROR_COMMAND_NOTFOUND(),
    ERROR_COMMAND_NOTHINGTOCONFIRM(),

    GUI_PAGE_DOORLIST(),
    GUI_PAGE_SUBMENU(),
    GUI_PAGE_CONFIRM(),
    GUI_PAGE_NEWDOORS(),
    GUI_PAGE_REMOVEOWNER(),

    GUI_SORTING_ALPHABETICAL(),
    GUI_SORTING_NUMERICAL(),
    GUI_SORTING_TYPICAL(),

    GUI_BUTTON_PREVIOUSPAGE(),
    GUI_BUTTON_NEXTPAGE(),
    GUI_BUTTON_SORT(),
    GUI_BUTTON_NEW(),
    GUI_BUTTON_LOCK(),
    GUI_BUTTON_UNLOCK(),
    GUI_BUTTON_TOGGLE(),
    GUI_BUTTON_INFO(),
    GUI_BUTTON_RELOCATEPB(),
    GUI_BUTTON_DOOR_DELETE(),
    GUI_BUTTON_DOOR_DELETE_CONFIRM(),
    GUI_BUTTON_DOOR_DELETE_CANCEL(),
    GUI_BUTTON_BLOCKSTOMOVE(),
    GUI_BUTTON_DIRECTION(),
    GUI_BUTTON_TIMER(),
    GUI_BUTTON_OWNER_ADD(),
    GUI_BUTTON_OWNER_DELETE(),

    GUI_DESCRIPTION_NEXTPAGE(NEXTPAGE, PREVIOUSPAGE, PAGECOUNT),
    GUI_DESCRIPTION_PREVIOUSPAGE(NEXTPAGE, PREVIOUSPAGE, PAGECOUNT),
    GUI_DESCRIPTION_INITIATION(DOORTYPE),
    GUI_DESCRIPTION_BLOCKSTOMOVE(BLOCKSTOMOVE),
    GUI_DESCRIPTION_DOORID(DOORID),
    GUI_DESCRIPTION_TIMER_SET(AUTOCLOSE),
    GUI_DESCRIPTION_OPENDIRECTION(OPENDIRECTION),
    GUI_DESCRIPTION_OPENDIRECTION_RELATIVE(OPENDIRECTION, RELATIVEDIRECTION),
    GUI_DESCRIPTION_TIMER_NOTSET(),
    GUI_DESCRIPTION_DOOR_DELETE(),
    GUI_DESCRIPTION_DOOR_DELETE_CONFIRM(),
    GUI_DESCRIPTION_DOOR_DELETE_CANCEL(),
    GUI_DESCRIPTION_INFO(DOORNAME),

    GENERAL_DIRECTION_CLOCKWISE(),
    GENERAL_DIRECTION_COUNTERCLOCKWISE(),
    GENERAL_DIRECTION_NORTH(),
    GENERAL_DIRECTION_EAST(),
    GENERAL_DIRECTION_SOUTH(),
    GENERAL_DIRECTION_WEST(),
    GENERAL_DIRECTION_UP(),
    GENERAL_DIRECTION_DOWN(),
    GENERAL_DIRECTION_NONE(),

    COMMAND_TIMEOUTORFAIL(),
    COMMAND_ADDOWNER_INIT(),
    COMMAND_ADDOWNER_SUCCESS(),
    COMMAND_ADDOWNER_FAIL(INPUT),
    COMMAND_REMOVEOWNER_INIT(),
    COMMAND_REMOVEOWNER_SUCCESS(),
    COMMAND_REMOVEOWNER_FAIL(INPUT),
    COMMAND_REMOVEOWNER_LIST(),
    COMMAND_SETTIME_INIT(),
    COMMAND_SETTIME_SUCCESS(AUTOCLOSE),
    COMMAND_SETTIME_DISABLED(),
    COMMAND_BLOCKSTOMOVE_INIT(),
    COMMAND_BLOCKSTOMOVE_SUCCESS(BLOCKSTOMOVE),
    COMMAND_BLOCKSTOMOVE_DISABLED(),
    COMMAND_SETROTATION_SUCCESS(OPENDIRECTION),
    COMMAND_DOOR_DELETE_SUCCESS(DOORNAME, DOORID),

    CREATOR_GENERAL_GIVENAME(),
    CREATOR_GENERAL_INVALIDPOINT(),
    CREATOR_GENERAL_INVALIDROTATIONPOINT(),
    CREATOR_GENERAL_POWERBLOCKINSIDEDOOR(),
    CREATOR_GENERAL_INVALIDROTATIONDIRECTION(),
    CREATOR_GENERAL_CANCELLED(),
    CREATOR_GENERAL_TIMEOUT(),
    CREATOR_GENERAL_STICKNAME(),
    CREATOR_GENERAL_INIT(),
    CREATOR_GENERAL_AREATOOBIG(BLOCKCOUNT, BLOCKLIMIT),
    CREATOR_GENERAL_2NDPOSNOT2D(),
    CREATOR_GENERAL_POINTNOTACORNER(),
    CREATOR_GENERAL_CONFIRMPRICE(DOORPRICE),
    CREATOR_GENERAL_INSUFFICIENTFUNDS(DOORPRICE),
    CREATOR_GENERAL_MONEYWITHDRAWN(DOORPRICE),
    CREATOR_GENERAL_SETPOWERBLOCK(),
    CREATOR_GENERAL_SETOPENDIR(OPENDIRECTIONLIST),
    CREATOR_GENERAL_POWERBLOCKTOOFAR(DISTANCE, DISTANCELIMIT),
    CREATOR_GENERAL_BLOCKSTOMOVETOOFAR(DISTANCE, DISTANCELIMIT),


    CREATOR_PBRELOCATOR_STICKLORE(),
    CREATOR_PBRELOCATOR_INIT(),
    CREATOR_PBRELOCATOR_SUCCESS(),
    CREATOR_PBRELOCATOR_LOCATIONINUSE(),
    CREATOR_PBRELOCATOR_LOCATIONNOTINSAMEWORLD(),

    CREATOR_PBINSPECTOR_STICKLORE(),
    CREATOR_PBINSPECTOR_INIT(),

    CREATOR_PORTCULLIS_STICKLORE(),
    CREATOR_PORTCULLIS_INIT(),
    CREATOR_PORTCULLIS_SUCCESS(),
    CREATOR_PORTCULLIS_STEP1(),
    CREATOR_PORTCULLIS_STEP2(),
    CREATOR_PORTCULLIS_BLOCKSTOMOVE(),

    CREATOR_DRAWBRIDGE_STICKLORE(),
    CREATOR_DRAWBRIDGE_INIT(),
    CREATOR_DRAWBRIDGE_SUCCESS(),
    CREATOR_DRAWBRIDGE_STEP1(),
    CREATOR_DRAWBRIDGE_STEP2(),
    CREATOR_DRAWBRIDGE_STEP3(),
    CREATOR_DRAWBRIDGE_STEP4(),

    CREATOR_ELEVATOR_STICKLORE(),
    CREATOR_ELEVATOR_INIT(),
    CREATOR_ELEVATOR_SUCCESS(),
    CREATOR_ELEVATOR_STEP1(),
    CREATOR_ELEVATOR_STEP2(),
    CREATOR_ELEVATOR_BLOCKSTOMOVE(),

    CREATOR_SLIDINGDOOR_STICKLORE(),
    CREATOR_SLIDINGDOOR_INIT(),
    CREATOR_SLIDINGDOOR_SUCCESS(),
    CREATOR_SLIDINGDOOR_STEP1(),
    CREATOR_SLIDINGDOOR_STEP2(),

    CREATOR_BIGDOOR_STICKLORE(),
    CREATOR_BIGDOOR_INIT(),
    CREATOR_BIGDOOR_SUCCESS(),
    CREATOR_BIGDOOR_STEP1(),
    CREATOR_BIGDOOR_STEP2(),
    CREATOR_BIGDOOR_STEP3(),

    CREATOR_FLAG_STICKLORE(),
    CREATOR_FLAG_INIT(),
    CREATOR_FLAG_SUCCESS(),
    CREATOR_FLAG_STEP1(),
    CREATOR_FLAG_STEP2(),
    CREATOR_FLAG_STEP3(),

    CREATOR_WINDMILL_STICKLORE(),
    CREATOR_WINDMILL_INIT(),
    CREATOR_WINDMILL_SUCCESS(),
    CREATOR_WINDMILL_STEP1(),
    CREATOR_WINDMILL_STEP2(),
    CREATOR_WINDMILL_STEP3(),

    CREATOR_REVOLVINGDOOR_STICKLORE(),
    CREATOR_REVOLVINGDOOR_INIT(),
    CREATOR_REVOLVINGDOOR_SUCCESS(),
    CREATOR_REVOLVINGDOOR_STEP1(),
    CREATOR_REVOLVINGDOOR_STEP2(),
    CREATOR_REVOLVINGDOOR_STEP3(),

    CREATOR_GARAGEDOOR_STICKLORE(),
    CREATOR_GARAGEDOOR_INIT(),
    CREATOR_GARAGEDOOR_SUCCESS(),
    CREATOR_GARAGEDOOR_STEP1(),
    CREATOR_GARAGEDOOR_STEP2(),
    CREATOR_GARAGEDOOR_STEP3(),

    CREATOR_CLOCK_STICKLORE(),
    CREATOR_CLOCK_INIT(),
    CREATOR_CLOCK_SUCCESS(),
    CREATOR_CLOCK_STEP1(),
    CREATOR_CLOCK_STEP2(),
    CREATOR_CLOCK_STEP3(),

    ;

    /**
     * The list of names that can be used as variables in this message.
     * <p>
     * For example: "This door will move %BLOCKSTOMOVE% blocks." Would contain at least "%BLOCKSTOMOVE%".
     */
    private final String[] variableNames;

    /**
     * Constructs a message.
     *
     * @param variableNames The names of the variables in the value that can be replaced.
     */
    Message(final @NotNull String... variableNames)
    {
        this.variableNames = variableNames;
    }

    /**
     * Gets the name of the variable at the given position for the given message.
     *
     * @param msg The message for which to retrieve the variable name.
     * @param idx The index of the variable name.
     * @return The name of the variable at the given position of this message.
     */
    @NotNull
    public static String getVariableName(final @NotNull Message msg, final int idx)
    {
        return msg.variableNames[idx];
    }

    /**
     * Gets the names of the variables for the given message..
     *
     * @param msg The message for which to retrieve the variable names.
     * @return The names of the variables of this message.
     */
    @NotNull
    public static String[] getVariableNames(final @NotNull Message msg)
    {
        return msg.variableNames;
    }

    /**
     * Gets the number of variables in this message that can be substituted.
     *
     * @param msg The message to retrieve the variable count for.
     * @return The number of variables in this message that can be substituted.
     */
    public static int getVariableCount(final @NotNull Message msg)
    {
        return msg.variableNames.length;
    }
}
