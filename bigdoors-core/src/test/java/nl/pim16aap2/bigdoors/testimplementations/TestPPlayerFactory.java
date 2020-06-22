package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TestPPlayerFactory implements IPPlayerFactory
{
    /** {@inheritDoc} */
    @Override
    public @NotNull IPPlayer create(@NotNull UUID playerUUID, @NotNull String playerName)
    {
        return new TestPPlayer(playerUUID, playerName);
    }
}
