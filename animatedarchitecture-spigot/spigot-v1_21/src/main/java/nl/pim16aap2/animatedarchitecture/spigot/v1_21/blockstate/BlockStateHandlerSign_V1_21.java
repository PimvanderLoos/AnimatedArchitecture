package nl.pim16aap2.animatedarchitecture.spigot.v1_21.blockstate;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import nl.pim16aap2.animatedarchitecture.spigot.util.blockstate.BlockStateHandler;
import nl.pim16aap2.animatedarchitecture.spigot.util.blockstate.BlockStateHandlerSign;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

import javax.inject.Inject;

/**
 * Implementation of {@link BlockStateHandler} for {@link Sign}s.
 */
public class BlockStateHandlerSign_V1_21 extends BlockStateHandlerSign
{
    @Inject
    BlockStateHandlerSign_V1_21()
    {
        super();
    }

    @Override
    protected void applyBlockState(Sign source, Sign target, Block block)
    {
        super.applyBlockState(source, target, block);
        target.setWaxed(source.isWaxed());
        target.update(true, false);
    }

    @Override
    protected JsonObject serializeSign(Gson gson, Sign source)
    {
        final JsonObject jsonObject = super.serializeSign(gson, source);
        jsonObject.addProperty("waxed", source.isWaxed());
        return jsonObject;
    }

    @Override
    protected void applySerializedSign(Gson gson, Sign target, JsonObject serializedSign)
    {
        super.applySerializedSign(gson, target, serializedSign);
        target.setWaxed(serializedSign.get("waxed").getAsBoolean());
        target.update(true, false);
    }
}
