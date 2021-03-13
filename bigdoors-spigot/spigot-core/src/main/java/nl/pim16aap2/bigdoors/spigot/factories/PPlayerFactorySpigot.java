package nl.pim16aap2.bigdoors.spigot.factories;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PPlayerSpigot;

/**
 * Represents an implementation of {@link IPPlayerFactory} for the Spigot platform.
 *
 * @author Pim
 */
public class PPlayerFactorySpigot implements IPPlayerFactory
{
    @Override
    public @NonNull IPPlayer create(final @NonNull PPlayerData playerData)
    {
        return new PPlayerSpigot(playerData);
    }
}
