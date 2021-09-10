package nl.pim16aap2.bigdoors.doors;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.moveblocks.AutoCloseScheduler;
import nl.pim16aap2.bigdoors.moveblocks.DoorActivityManager;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import org.jetbrains.annotations.Nullable;

@Getter
public class DoorToggleRequest
{
    private final DoorRetriever.AbstractRetriever doorRetriever;
    private final DoorActionCause cause;
    private final IMessageable messageReceiver;
    private final @Nullable IPPlayer responsible;
    private final double time;
    private final boolean skipAnimation;
    private final DoorActionType doorActionType;

    private final IPLogger logger;
    private final ILocalizer localizer;
    private final DoorActivityManager doorActivityManager;
    private final AutoCloseScheduler autoCloseScheduler;

    @AssistedInject //
    DoorToggleRequest(@Assisted DoorRetriever.AbstractRetriever doorRetriever, @Assisted DoorActionCause cause,
                      @Assisted IMessageable messageReceiver, @Assisted @Nullable IPPlayer responsible,
                      @Assisted double time, @Assisted boolean skipAnimation, @Assisted DoorActionType doorActionType,
                      IPLogger logger, ILocalizer localizer, DoorActivityManager doorActivityManager,
                      AutoCloseScheduler autoCloseScheduler)
    {
        this.doorRetriever = doorRetriever;
        this.cause = cause;
        this.messageReceiver = messageReceiver;
        this.responsible = responsible;
        this.time = time;
        this.skipAnimation = skipAnimation;
        this.doorActionType = doorActionType;
        this.logger = logger;
        this.localizer = localizer;
        this.doorActivityManager = doorActivityManager;
        this.autoCloseScheduler = autoCloseScheduler;
    }

    @AssistedFactory
    interface Factory
    {
        DoorToggleRequest create(DoorRetriever.AbstractRetriever doorRetriever, DoorActionCause cause,
                                 IMessageable messageReceiver, @Nullable IPPlayer responsible, double time,
                                 boolean skipAnimation, DoorActionType doorActionType);
    }
}
