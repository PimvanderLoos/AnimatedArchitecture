package nl.pim16aap2.animatedarchitecture.spigot.util.blockstate;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import org.bukkit.Nameable;
import org.bukkit.block.Block;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Handler for {@link Nameable} block states.
 */
@Singleton
final class BlockStateHandlerNameable extends BlockStateHandler<Nameable>
{
    @Inject
    BlockStateHandlerNameable()
    {
        super(Nameable.class);
    }

    @Override
    protected void applyBlockState(Nameable source, Nameable target, Block block)
    {
        target.setCustomName(source.getCustomName());
    }

    @Override
    public void appendSerializedData(Gson gson, Nameable src, JsonObject jsonObject)
    {
        jsonObject.addProperty("nameable", src.getCustomName());
    }

    @Override
    protected void applySerializedBlockState(Gson gson, Nameable target, JsonObject serializedBlockState)
    {
        final String name = Util
            .requireNonNull(serializedBlockState.get("nameable"), "deserialized 'nameable'")
            .getAsString();
        target.setCustomName(name);
    }
}
