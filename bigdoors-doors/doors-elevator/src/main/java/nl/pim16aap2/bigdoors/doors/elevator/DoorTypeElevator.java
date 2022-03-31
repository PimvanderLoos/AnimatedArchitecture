package nl.pim16aap2.bigdoors.doors.elevator;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PSound;
import nl.pim16aap2.bigdoors.audio.AudioDescription;
import nl.pim16aap2.bigdoors.audio.AudioSet;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class DoorTypeElevator extends DoorType
{
    private static final int TYPE_VERSION = 1;

    private static final DoorTypeElevator INSTANCE = new DoorTypeElevator();

    private static final AudioSet AUDIO_SET = new AudioSet(
        new AudioDescription(PSound.DRAGGING, 0.8f, 0.7f, 1),
        new AudioDescription(PSound.THUD, 0.2f, 0.15f, 1));

    private DoorTypeElevator()
    {
        super(Constants.PLUGIN_NAME, "Elevator", TYPE_VERSION,
              Arrays.asList(RotateDirection.UP, RotateDirection.DOWN), "door.type.elevator");
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    public static DoorTypeElevator get()
    {
        return INSTANCE;
    }

    @Override
    public Class<? extends AbstractDoor> getDoorClass()
    {
        return Elevator.class;
    }

    @Override
    public Creator getCreator(Creator.Context context, IPPlayer player, @Nullable String name)
    {
        return new CreatorElevator(context, player, name);
    }

    @Override
    public AudioSet getAudioSet()
    {
        return AUDIO_SET;
    }
}
