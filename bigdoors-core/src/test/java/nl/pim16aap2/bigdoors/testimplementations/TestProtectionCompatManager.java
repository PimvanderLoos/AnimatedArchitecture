package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IProtectionCompatManager;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class TestProtectionCompatManager implements IProtectionCompatManager
{
    @Override
    @NotNull
    public Optional<String> canBreakBlock(final @NotNull IPPlayer player, final @NotNull IPLocationConst loc)
    {
        return Optional.empty();
    }

    @Override
    @NotNull
    public Optional<String> canBreakBlocksBetweenLocs(final @NotNull IPPlayer player,
                                                      final @NotNull Vector3DiConst pos1,
                                                      final @NotNull Vector3DiConst pos2,
                                                      final @NotNull IPWorld world)
    {
        return Optional.empty();
    }
}
