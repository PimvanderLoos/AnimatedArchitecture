package nl.pim16aap2.animatedarchitecture.spigot.v1_21.blockstate;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jeff_media.persistentdataserializer.PersistentDataSerializer;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.spigot.util.blockstate.BlockStateHandler;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of {@link BlockStateHandler} for {@link TileState}s for Spigot 1.21.
 */
@Flogger
@Singleton
final class BlockStateHandlerTileState_V1_21 extends BlockStateHandler<TileState>
{
    @Inject
    BlockStateHandlerTileState_V1_21()
    {
        super(TileState.class);
    }

    @Override
    protected void applyBlockState(TileState source, TileState target, Block block)
    {
        source.getPersistentDataContainer().copyTo(target.getPersistentDataContainer(), true);
        target.update(true, false);
    }

    @Override
    protected void appendSerializedData(Gson gson, TileState source, JsonObject jsonObject)
    {
        jsonObject.addProperty("persistentData", PersistentDataSerializer.toJson(source.getPersistentDataContainer()));
    }

    @Override
    protected void applySerializedBlockState(Gson gson, TileState target, JsonObject serializedBlockState)
    {
        final JsonObject persistentData = serializedBlockState.getAsJsonObject("persistentData");
        PersistentDataSerializer.fromJson(persistentData.getAsString(), target.getPersistentDataContainer());
        target.update(true, false);
    }
}
