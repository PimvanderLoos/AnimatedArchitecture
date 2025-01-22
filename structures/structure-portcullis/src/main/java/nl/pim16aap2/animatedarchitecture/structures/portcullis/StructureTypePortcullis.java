package nl.pim16aap2.animatedarchitecture.structures.portcullis;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.NamespacedKey;
import nl.pim16aap2.animatedarchitecture.core.audio.AudioDescription;
import nl.pim16aap2.animatedarchitecture.core.audio.AudioSet;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class StructureTypePortcullis extends StructureType
{
    private static final int TYPE_VERSION = 10;

    private static final StructureTypePortcullis INSTANCE = new StructureTypePortcullis();

    private static final AudioSet AUDIO_SET = new AudioSet(
        new AudioDescription("bd.dragging2", 0.8f, 0.7f, 750),
        new AudioDescription("bd.thud", 0.2f, 0.15f, 250)
    );

    private StructureTypePortcullis()
    {
        super(
            NamespacedKey.of("Portcullis"),
            TYPE_VERSION,
            List.of(
                MovementDirection.UP,
                MovementDirection.DOWN),
            List.of(
                Property.ANIMATION_SPEED_MULTIPLIER,
                Property.BLOCKS_TO_MOVE,
                Property.OPEN_STATUS,
                Property.REDSTONE_MODE
            ),
            "structure.type.portcullis"
        );
    }

    /**
     * Getter for the type for this structure.
     *
     * @return The instance of this type.
     */
    public static StructureTypePortcullis get()
    {
        return INSTANCE;
    }

    @Override
    public Class<? extends Structure> getStructureClass()
    {
        return Portcullis.class;
    }

    @Override
    public Creator getCreator(ToolUser.Context context, IPlayer player, @Nullable String name)
    {
        return new CreatorPortcullis(context, player, name);
    }

    @Override
    public AudioSet getAudioSet()
    {
        return AUDIO_SET;
    }
}
