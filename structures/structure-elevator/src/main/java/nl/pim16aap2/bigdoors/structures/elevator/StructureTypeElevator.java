package nl.pim16aap2.bigdoors.structures.elevator;

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

public final class StructureTypeElevator extends StructureType
{
    private static final int TYPE_VERSION = 1;

    private static final StructureTypeElevator INSTANCE = new StructureTypeElevator();

    private static final AudioSet AUDIO_SET = new AudioSet(
        new AudioDescription("bd.dragging2", 0.8f, 0.7f, 750),
        new AudioDescription("bd.thud", 0.2f, 0.15f, 250));

    private StructureTypeElevator()
    {
        super(Constants.PLUGIN_NAME, "Elevator", TYPE_VERSION,
              Arrays.asList(MovementDirection.UP, MovementDirection.DOWN), "structure.type.elevator");
    }

    /**
     * @return The instance of this type.
     */
    public static StructureTypeElevator get()
    {
        return INSTANCE;
    }

    @Override
    public Class<? extends AbstractStructure> getStructureClass()
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
