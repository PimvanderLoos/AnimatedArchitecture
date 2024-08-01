package nl.pim16aap2.animatedarchitecture.structures.bigdoor;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.NamespacedKey;
import nl.pim16aap2.animatedarchitecture.core.audio.AudioDescription;
import nl.pim16aap2.animatedarchitecture.core.audio.AudioSet;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The type for the BigDoor structure.
 */
public final class StructureTypeBigDoor extends StructureType
{
    private static final int TYPE_VERSION = 1;

    private static final StructureTypeBigDoor INSTANCE = new StructureTypeBigDoor();

    private static final AudioSet AUDIO_SET = new AudioSet(
        new AudioDescription("bd.dragging2", 0.8f, 0.7f, 750),
        new AudioDescription("bd.thud", 0.2f, 0.15f, 250)
    );

    private StructureTypeBigDoor()
    {
        super(
            NamespacedKey.of("BigDoor"),
            TYPE_VERSION,
            List.of(
                MovementDirection.CLOCKWISE,
                MovementDirection.COUNTERCLOCKWISE),
            List.of(
                Property.ANIMATION_SPEED_MULTIPLIER,
                Property.OPEN_STATUS,
                Property.REDSTONE_MODE,
                Property.ROTATION_POINT
            ),
            "structure.type.big_door"
        );
    }

    /**
     * Getter for the type for this structure.
     *
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
    public Creator getCreator(ToolUser.Context context, IPlayer player, @Nullable String name)
    {
        return new CreatorBigDoor(context, player, name);
    }

    @Override
    public AudioSet getAudioSet()
    {
        return AUDIO_SET;
    }
}
