package nl.pim16aap2.bigdoors.doors.bigdoor;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.tooluser.creator.CreatorTestsUtil;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreatorBigDoorTest extends CreatorTestsUtil
{
    @BeforeEach
    public void setup()
    {
        super.beforeEach();
    }

    @Test
    public void createBigDoor()
    {
        openDirection = RotateDirection.CLOCKWISE;
        String openDirectionName = "0";

        final @NonNull BigDoor actualDoor = new BigDoor(constructDoorData());
        final @NonNull CreatorBigDoor creator = new CreatorBigDoor(PLAYER);
        testCreation(creator, actualDoor,
                     doorName,
                     min.toLocation(world),
                     max.toLocation(world),
                     engine.toLocation(world),
                     powerblock.toLocation(world),
                     openDirectionName);
    }
}
