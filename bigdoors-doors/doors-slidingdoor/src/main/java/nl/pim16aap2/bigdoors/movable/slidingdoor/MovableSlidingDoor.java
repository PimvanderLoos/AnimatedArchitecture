package nl.pim16aap2.bigdoors.movable.slidingdoor;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.audio.AudioDescription;
import nl.pim16aap2.bigdoors.audio.AudioSet;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class MovableSlidingDoor extends MovableType
{
    private static final int TYPE_VERSION = 1;

    private static final MovableSlidingDoor INSTANCE = new MovableSlidingDoor();

    private static final AudioSet AUDIO_SET = new AudioSet(
        new AudioDescription("bd.dragging2", 0.8f, 0.7f, 750),
        new AudioDescription("bd.thud", 0.2f, 0.15f, 250));

    private MovableSlidingDoor()
    {
        super(Constants.PLUGIN_NAME, "SlidingDoor", TYPE_VERSION, Arrays
            .asList(RotateDirection.NORTH, RotateDirection.EAST,
                    RotateDirection.SOUTH, RotateDirection.WEST), "movable.type.sliding_door");
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    public static MovableSlidingDoor get()
    {
        return INSTANCE;
    }

    @Override
    public Class<? extends AbstractMovable> getMovableClass()
    {
        return SlidingDoor.class;
    }

    @Override
    public Creator getCreator(Creator.Context context, IPPlayer player, @Nullable String name)
    {
        return new CreatorSlidingDoor(context, player, name);
    }

    @Override
    public AudioSet getAudioSet()
    {
        return AUDIO_SET;
    }
}
