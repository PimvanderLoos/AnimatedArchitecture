package nl.pim16aap2.animatedarchitecture.spigot.core.managers;

import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.api.IEconomyManager;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;

import java.util.OptionalDouble;

/**
 * Provides the canonical disabled economy behavior when Vault economy integration is unavailable.
 */
@Singleton
final class DisabledEconomyManager implements IEconomyManager, IDebuggable
{
    /**
     * Creates a disabled economy manager.
     */
    DisabledEconomyManager()
    {
    }

    @Override
    public boolean buyStructure(IPlayer player, IWorld world, StructureType type, int blockCount)
    {
        return true;
    }

    @Override
    public OptionalDouble getPrice(StructureType type, int blockCount)
    {
        return OptionalDouble.empty();
    }

    @Override
    public boolean isEconomyEnabled()
    {
        return false;
    }

    @Override
    public String getDebugInformation()
    {
        return "Economy backend: Disabled";
    }
}
