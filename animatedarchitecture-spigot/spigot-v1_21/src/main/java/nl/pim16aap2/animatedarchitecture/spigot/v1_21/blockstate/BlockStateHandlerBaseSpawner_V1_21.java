package nl.pim16aap2.animatedarchitecture.spigot.v1_21.blockstate;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import nl.pim16aap2.animatedarchitecture.spigot.util.blockstate.BlockStateHandler;
import org.bukkit.block.Block;
import org.bukkit.spawner.BaseSpawner;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of {@link BlockStateHandler} for {@link BaseSpawner}s for Spigot 1.21.
 */
@Singleton
final class BlockStateHandlerBaseSpawner_V1_21 extends BlockStateHandler<BaseSpawner>
{
    @Inject
    BlockStateHandlerBaseSpawner_V1_21()
    {
        super(BaseSpawner.class);
    }

    @Override
    protected void applyBlockState(BaseSpawner source, BaseSpawner target, Block block)
    {
        target.setSpawnRange(source.getSpawnRange());
        target.setDelay(source.getDelay());
        target.setPotentialSpawns(source.getPotentialSpawns());
        target.setSpawnedEntity(source.getSpawnedEntity());
        target.setSpawnRange(source.getSpawnRange());
    }

    @Override
    protected void appendSerializedData(Gson gson, BaseSpawner source, JsonObject jsonObject)
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    protected void applySerializedBlockState(Gson gson, BaseSpawner target, JsonObject serializedBlockState)
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
