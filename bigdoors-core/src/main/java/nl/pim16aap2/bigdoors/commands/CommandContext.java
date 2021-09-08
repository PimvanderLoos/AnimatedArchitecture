package nl.pim16aap2.bigdoors.commands;

import lombok.AccessLevel;
import lombok.Getter;
import nl.pim16aap2.bigdoors.api.DebugReporter;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DelayedCommandInputManager;
import nl.pim16aap2.bigdoors.managers.DoorSpecificationManager;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.moveblocks.DoorActivityManager;

import javax.inject.Inject;

@Getter(AccessLevel.PACKAGE)
public class CommandContext
{
    private final IPLogger logger;
    private final ILocalizer localizer;
    private final DatabaseManager databaseManager;
    private final ToolUserManager toolUserManager;
    private final DoorActivityManager doorActivityManager;
    private final DelayedCommandInputManager delayedCommandInputManager;
    private final DoorSpecificationManager doorSpecificationManager;
    private final IMessagingInterface messagingInterface;
    private final DebugReporter debugReporter;
    private final IBigDoorsPlatform platform;

    @Inject
    protected CommandContext(IPLogger logger, ILocalizer localizer,
                             DatabaseManager databaseManager, ToolUserManager toolUserManager,
                             DoorActivityManager doorActivityManager,
                             DelayedCommandInputManager delayedCommandInputManager,
                             DoorSpecificationManager doorSpecificationManager,
                             IMessagingInterface messagingInterface,
                             DebugReporter debugReporter,
                             IBigDoorsPlatform platform)
    {
        this.logger = logger;
        this.localizer = localizer;
        this.databaseManager = databaseManager;
        this.toolUserManager = toolUserManager;
        this.doorActivityManager = doorActivityManager;
        this.delayedCommandInputManager = delayedCommandInputManager;
        this.doorSpecificationManager = doorSpecificationManager;
        this.messagingInterface = messagingInterface;
        this.debugReporter = debugReporter;
        this.platform = platform;
    }
}
