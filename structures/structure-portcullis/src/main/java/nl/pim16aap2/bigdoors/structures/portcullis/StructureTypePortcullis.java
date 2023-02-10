package nl.pim16aap2.bigdoors.structures.portcullis;

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
    public Creator getCreator(Creator.Context context, IPlayer player, @Nullable String name)
    {
        return new CreatorPortcullis(context, player, name);
    }

    @Override
    public AudioSet getAudioSet()
    {
        return AUDIO_SET;
    }
}
