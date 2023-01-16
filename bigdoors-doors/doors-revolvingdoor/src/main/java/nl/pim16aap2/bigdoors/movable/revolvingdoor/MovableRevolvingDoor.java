package nl.pim16aap2.bigdoors.movable.revolvingdoor;

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

public final class MovableRevolvingDoor extends MovableType
{
    private static final int TYPE_VERSION = 1;

    private static final MovableRevolvingDoor INSTANCE = new MovableRevolvingDoor();

    private static final AudioSet AUDIO_SET = new AudioSet(
        new AudioDescription("bd.dragging2", 0.8f, 0.7f, 750),
        new AudioDescription("bd.thud", 0.2f, 0.15f, 250));

    private MovableRevolvingDoor()
    {
        super(Constants.PLUGIN_NAME, "RevolvingDoor", TYPE_VERSION,
              Arrays.asList(RotateDirection.CLOCKWISE, RotateDirection.COUNTERCLOCKWISE), "door.type.revolving_door");
    }

    /**
     * @return The instance of this type.
     */
    public static MovableRevolvingDoor get()
    {
        return INSTANCE;
    }

    @Override
    public Class<? extends AbstractMovable> getMovableClass()
    {
        return RevolvingDoor.class;
    }

    @Override
    public Creator getCreator(Creator.Context context, IPPlayer player, @Nullable String name)
    {
        return new CreatorRevolvingDoor(context, player, name);
    }

    @Override
    public AudioSet getAudioSet()
    {
        return AUDIO_SET;
    }
}
