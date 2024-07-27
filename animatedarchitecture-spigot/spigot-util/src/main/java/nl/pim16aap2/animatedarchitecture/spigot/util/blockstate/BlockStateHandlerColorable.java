package nl.pim16aap2.animatedarchitecture.spigot.util.blockstate;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.DyeColor;
import org.bukkit.block.Block;
import org.bukkit.material.Colorable;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Handler for {@link Colorable} block states.
 */
@Singleton
final class BlockStateHandlerColorable extends BlockStateHandler<Colorable>
{
    @Inject
    BlockStateHandlerColorable()
    {
        super(Colorable.class);
    }

    @Override
    protected void applyBlockState(Colorable source, Colorable target, Block block)
    {
        target.setColor(source.getColor());
    }

    @Override
    protected void appendSerializedData(Gson gson, Colorable source, JsonObject jsonObject)
    {
        jsonObject.add("colorable", gson.toJsonTree(source.getColor()));
    }

    @Override
    protected void applySerializedBlockState(Gson gson, Colorable target, JsonObject serializedBlockState)
    {
        target.setColor(gson.fromJson(serializedBlockState.get("colorable"), DyeColor.class));
    }
}
