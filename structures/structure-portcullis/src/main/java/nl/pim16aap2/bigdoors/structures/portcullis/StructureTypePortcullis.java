package nl.pim16aap2.bigdoors.structures.portcullis;

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

public final class StructureTypePortcullis extends StructureType
{
    private static final int TYPE_VERSION = 1;

    private static final StructureTypePortcullis INSTANCE = new StructureTypePortcullis();

    private static final AudioSet AUDIO_SET = new AudioSet(
        new AudioDescription("bd.dragging2", 0.8f, 0.7f, 750),
        new AudioDescription("bd.thud", 0.2f, 0.15f, 250));

    private StructureTypePortcullis()
    {
        super(Constants.PLUGIN_NAME, "Portcullis", TYPE_VERSION,
              Arrays.asList(MovementDirection.UP, MovementDirection.DOWN), "structure.type.portcullis");
    }

    /**
     * @return The instance of this type.
     */
    public static StructureTypePortcullis get()
    {
        return INSTANCE;
    }

    @Override
    public Class<? extends AbstractStructure> getStructureClass()
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
