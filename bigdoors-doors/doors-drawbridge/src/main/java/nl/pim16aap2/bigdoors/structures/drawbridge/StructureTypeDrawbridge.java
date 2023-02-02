package nl.pim16aap2.bigdoors.structures.drawbridge;

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

public final class StructureTypeDrawbridge extends StructureType
{
    private static final int TYPE_VERSION = 1;

    private static final StructureTypeDrawbridge INSTANCE = new StructureTypeDrawbridge();

    private static final AudioSet AUDIO_SET = new AudioSet(
        new AudioDescription("bd.drawbridge-rattling", 0.8f, 0.7f, 750),
        new AudioDescription("bd.closing-vault-door", 0.2f, 0.15f, 750));

    private StructureTypeDrawbridge()
    {
        super(Constants.PLUGIN_NAME, "DrawBridge", TYPE_VERSION,
              Arrays.asList(MovementDirection.NORTH, MovementDirection.EAST,
                            MovementDirection.SOUTH, MovementDirection.WEST), "structure.type.drawbridge");
    }

    @Override
    public Creator getCreator(Creator.Context context, IPPlayer player, @Nullable String name)
    {
        return new CreatorDrawbridge(context, player, name);
    }

    /**
     * @return The instance of this type.
     */
    public static StructureTypeDrawbridge get()
    {
        return INSTANCE;
    }

    @Override
    public Class<? extends AbstractStructure> getStructureClass()
    {
        return Drawbridge.class;
    }

    @Override
    public AudioSet getAudioSet()
    {
        return AUDIO_SET;
    }
}