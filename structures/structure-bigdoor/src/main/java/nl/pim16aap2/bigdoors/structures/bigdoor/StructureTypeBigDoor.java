package nl.pim16aap2.bigdoors.structures.bigdoor;

import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.audio.AudioDescription;
import nl.pim16aap2.bigdoors.core.audio.AudioSet;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structures.StructureType;
import nl.pim16aap2.bigdoors.core.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.core.util.Constants;
import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class StructureTypeBigDoor extends StructureType
{
    private static final int TYPE_VERSION = 1;

    private static final StructureTypeBigDoor INSTANCE = new StructureTypeBigDoor();

    private static final AudioSet AUDIO_SET = new AudioSet(
        new AudioDescription("bd.dragging2", 0.8f, 0.7f, 750),
        new AudioDescription("bd.thud", 0.2f, 0.15f, 250));

    private StructureTypeBigDoor()
    {
        super(Constants.PLUGIN_NAME, "BigDoor", TYPE_VERSION,
              Arrays.asList(MovementDirection.CLOCKWISE, MovementDirection.COUNTERCLOCKWISE),
              "structure.type.big_door");
    }

    /**
     * @return The instance of this type.
     */
    public static StructureTypeBigDoor get()
    {
        return INSTANCE;
    }

    @Override
    public Class<? extends AbstractStructure> getStructureClass()
    {
        return BigDoor.class;
    }

    @Override
    public Creator getCreator(Creator.Context context, IPlayer player, @Nullable String name)
    {
        return new CreatorBigDoor(context, player, name);
    }

    @Override
    public AudioSet getAudioSet()
    {
        return AUDIO_SET;
    }
}
