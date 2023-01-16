package nl.pim16aap2.bigdoors.movable.portcullis;

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

public final class MovableTypePortcullis extends MovableType
{
    private static final int TYPE_VERSION = 1;

    private static final MovableTypePortcullis INSTANCE = new MovableTypePortcullis();

    private static final AudioSet AUDIO_SET = new AudioSet(
        new AudioDescription("bd.dragging2", 0.8f, 0.7f, 750),
        new AudioDescription("bd.thud", 0.2f, 0.15f, 250));

    private MovableTypePortcullis()
    {
        super(Constants.PLUGIN_NAME, "Portcullis", TYPE_VERSION,
              Arrays.asList(RotateDirection.UP, RotateDirection.DOWN), "door.type.portcullis");
    }

    /**
     * Obtains the instance of this type.
     *
     * @return The instance of this type.
     */
    public static MovableTypePortcullis get()
    {
        return INSTANCE;
    }

    @Override
    public Class<? extends AbstractMovable> getMovableClass()
    {
        return Portcullis.class;
    }

    @Override
    public Creator getCreator(Creator.Context context, IPPlayer player, @Nullable String name)
    {
        return new CreatorPortcullis(context, player, name);
    }

    @Override
    public AudioSet getAudioSet()
    {
        return AUDIO_SET;
    }
}
