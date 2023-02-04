package nl.pim16aap2.bigdoors.structures.revolvingdoor;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.audio.AudioDescription;
import nl.pim16aap2.bigdoors.audio.AudioSet;
import nl.pim16aap2.bigdoors.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.structuretypes.StructureType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class StructureTypeRevolvingDoor extends StructureType
{
    private static final int TYPE_VERSION = 1;

    private static final StructureTypeRevolvingDoor INSTANCE = new StructureTypeRevolvingDoor();

    private static final AudioSet AUDIO_SET = new AudioSet(
        new AudioDescription("bd.dragging2", 0.8f, 0.7f, 750),
        new AudioDescription("bd.thud", 0.2f, 0.15f, 250));

    private StructureTypeRevolvingDoor()
    {
        super(Constants.PLUGIN_NAME, "RevolvingDoor", TYPE_VERSION,
              Arrays.asList(MovementDirection.CLOCKWISE, MovementDirection.COUNTERCLOCKWISE),
              "structure.type.revolving_door");
    }

    /**
     * @return The instance of this type.
     */
    public static StructureTypeRevolvingDoor get()
    {
        return INSTANCE;
    }

    @Override
    public Class<? extends AbstractStructure> getStructureClass()
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
