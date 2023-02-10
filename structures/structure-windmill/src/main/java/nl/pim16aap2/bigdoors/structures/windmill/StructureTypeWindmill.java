package nl.pim16aap2.bigdoors.structures.windmill;

import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.audio.AudioDescription;
import nl.pim16aap2.bigdoors.core.audio.AudioSet;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structuretypes.StructureType;
import nl.pim16aap2.bigdoors.core.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.core.util.Constants;
import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class StructureTypeWindmill extends StructureType
{
    private static final int TYPE_VERSION = 1;

    private static final StructureTypeWindmill INSTANCE = new StructureTypeWindmill();

    private static final AudioSet AUDIO_SET = new AudioSet(
        new AudioDescription("bd.drawbridge-rattling", 0.8f, 0.7f, 750),
        new AudioDescription("bd.thud", 0.2f, 0.15f, 250));

    private StructureTypeWindmill()
    {
        super(Constants.PLUGIN_NAME, "Windmill", TYPE_VERSION,
              Arrays.asList(MovementDirection.NORTH, MovementDirection.EAST,
                            MovementDirection.SOUTH, MovementDirection.WEST), "structure.type.windmill");
    }

    /**
     * @return The instance of this type.
     */
    public static StructureTypeWindmill get()
    {
        return INSTANCE;
    }

    @Override
    public Class<? extends AbstractStructure> getStructureClass()
    {
        return Windmill.class;
    }

    @Override
    public Creator getCreator(Creator.Context context, IPlayer player, @Nullable String name)
    {
        return new CreatorWindMill(context, player, name);
    }

    @Override
    public AudioSet getAudioSet()
    {
        return AUDIO_SET;
    }
}
