package nl.pim16aap2.bigdoors.movable.bigdoor;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.audio.AudioDescription;
import nl.pim16aap2.bigdoors.audio.AudioSet;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class MovableBigDoor extends MovableType
{
    private static final int TYPE_VERSION = 1;

    private static final MovableBigDoor INSTANCE = new MovableBigDoor();

    private static final AudioSet AUDIO_SET = new AudioSet(
        new AudioDescription("bd.dragging2", 0.8f, 0.7f, 750),
        new AudioDescription("bd.thud", 0.2f, 0.15f, 250));

    private MovableBigDoor()
    {
        super(Constants.PLUGIN_NAME, "BigDoor", TYPE_VERSION,
              Arrays.asList(MovementDirection.CLOCKWISE, MovementDirection.COUNTERCLOCKWISE), "movable.type.big_door");
    }

    /**
     * @return The instance of this type.
     */
    public static MovableBigDoor get()
    {
        return INSTANCE;
    }

    @Override
    public Class<? extends AbstractMovable> getMovableClass()
    {
        return BigDoor.class;
    }

    @Override
    public Creator getCreator(Creator.Context context, IPPlayer player, @Nullable String name)
    {
        return new CreatorBigDoor(context, player, name);
    }

    @Override
    public AudioSet getAudioSet()
    {
        return AUDIO_SET;
    }
}
