package nl.pim16aap2.animatedarchitecture.structures.garagedoor;

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

public final class StructureTypeGarageDoor extends StructureType
{
    private static final int TYPE_VERSION = 10;

    private static final StructureTypeGarageDoor INSTANCE = new StructureTypeGarageDoor();

    private static final AudioSet AUDIO_SET = new AudioSet(
        new AudioDescription("bd.drawbridge-rattling", 0.8f, 0.7f, 800),
        new AudioDescription("bd.thud", 0.2f, 0.15f, 250)
    );

    private StructureTypeGarageDoor()
    {
        super(
            NamespacedKey.of("GarageDoor"),
            TYPE_VERSION,
            List.of(
                MovementDirection.NORTH,
                MovementDirection.EAST,
                MovementDirection.SOUTH,
                MovementDirection.WEST),
            List.of(
                Property.ANIMATION_SPEED_MULTIPLIER,
                Property.OPEN_STATUS,
                Property.REDSTONE_MODE,
                Property.ROTATION_POINT
            ),
            "structure.type.garage_door"
        );
    }

    /**
     * Getter for the type for this structure.
     *
     * @return The instance of this type.
     */
    public static StructureTypeGarageDoor get()
    {
        return INSTANCE;
    }

    @Override
    public Class<? extends Structure> getStructureClass()
    {
        return GarageDoor.class;
    }

    @Override
    public Creator getCreator(ToolUser.Context context, IPlayer player, @Nullable String name)
    {
        return new CreatorGarageDoor(context, player, name);
    }

    @Override
    public AudioSet getAudioSet()
    {
        return AUDIO_SET;
    }
}
