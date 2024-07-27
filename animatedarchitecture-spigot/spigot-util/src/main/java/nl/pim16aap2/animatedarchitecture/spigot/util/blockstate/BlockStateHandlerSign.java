package nl.pim16aap2.animatedarchitecture.spigot.util.blockstate;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;

import javax.inject.Singleton;
import java.util.List;

/**
 * The base handler for {@link Sign} block states.
 * <p>
 * This class is responsible for applying the block state of a source {@link Sign} to a target {@link Sign}.
 * <p>
 * This class is abstract because different versions of Minecraft have additional properties that need to be set.
 */
@Singleton
public abstract class BlockStateHandlerSign extends BlockStateHandler<Sign>
{
    private static final List<Side> SIDES = List.of(Side.values());

    protected BlockStateHandlerSign()
    {
        super(Sign.class);
    }

    /**
     * Applies the block state of the source block state to the target block state.
     * <p>
     * Note: This method does not update the block state of the target block state. This is left for subclasses to
     * implement.
     *
     * @param source
     *     The source block state to apply to the target block state.
     * @param target
     *     The target block state to apply the source block state to.
     * @param block
     */
    @Override
    protected void applyBlockState(Sign source, Sign target, Block block)
    {
        SIDES.forEach(side -> applyBlockState(source.getSide(side), target.getSide(side)));
    }

    /**
     * Applies the block state of the source block state to the target block state.
     *
     * @param source
     *     The source block state to apply to the target block state.
     * @param target
     *     The target block state to apply the source block state to.
     */
    private void applyBlockState(SignSide source, SignSide target)
    {
        final String[] lines = source.getLines();
        for (int i = 0; i < lines.length; i++)
            target.setLine(i, lines[i]);

        target.setGlowingText(source.isGlowingText());
    }

    /**
     * Serializes the {@link SignSide} to a {@link JsonObject}.
     *
     * @param gson
     *     The {@link Gson} instance to use for serialization.
     * @param source
     *     The {@link SignSide} to serialize.
     * @return The serialized {@link SignSide}.
     */
    protected JsonObject serializeSignSide(Gson gson, SignSide source)
    {
        final JsonObject sideJson = new JsonObject();

        sideJson.addProperty("lines", gson.toJson(source.getLines()));
        sideJson.addProperty("glowingText", source.isGlowingText());

        return sideJson;
    }

    /**
     * Serializes the {@link Sign} to a {@link JsonObject}.
     *
     * @param gson
     *     The {@link Gson} instance to use for serialization.
     * @param source
     *     The {@link Sign} to serialize.
     * @return The serialized {@link Sign}.
     */
    protected JsonObject serializeSign(Gson gson, Sign source)
    {
        final JsonObject signJson = new JsonObject();

        SIDES.forEach(side -> signJson.add(side.name(), serializeSignSide(gson, source.getSide(side))));

        return signJson;
    }

    @Override
    protected final void appendSerializedData(Gson gson, Sign source, JsonObject jsonObject)
    {
        jsonObject.add("sign", serializeSign(gson, source));
    }

    /**
     * Applies the serialized {@link SignSide} to the target {@link SignSide}.
     *
     * @param gson
     *     The {@link Gson} instance to use for deserialization.
     * @param target
     *     The target {@link Sign} to apply the serialized {@link Sign} to.
     * @param serializedSignSide
     *     The serialized {@link SignSide} to apply to the target {@link SignSide}.
     */
    protected void applySerializedSignSide(Gson gson, SignSide target, JsonObject serializedSignSide)
    {
        final String[] lines = gson.fromJson(serializedSignSide.get("lines"), String[].class);
        for (int i = 0; i < lines.length; i++)
            target.setLine(i, lines[i]);

        target.setGlowingText(serializedSignSide.get("glowingText").getAsBoolean());
    }

    /**
     * Applies the serialized {@link Sign} to the target {@link Sign}.
     * <p>
     * Subclasses can override this method to apply additional properties if necessary.
     *
     * @param gson
     *     The {@link Gson} instance to use for deserialization.
     * @param target
     *     The target {@link Sign} to apply the serialized {@link Sign} to.
     * @param serializedSign
     *     The serialized {@link Sign} to apply to the target {@link Sign}.
     */
    protected void applySerializedSign(Gson gson, Sign target, JsonObject serializedSign)
    {
        SIDES.forEach(side ->
            applySerializedSignSide(gson, target.getSide(side), serializedSign.getAsJsonObject(side.name())));
    }

    @Override
    protected final void applySerializedBlockState(Gson gson, Sign target, JsonObject serializedBlockState)
    {
        final JsonObject signJson = serializedBlockState.get("sign").getAsJsonObject();
        applySerializedSign(gson, target, signJson);
        target.update(true, false);
    }
}
