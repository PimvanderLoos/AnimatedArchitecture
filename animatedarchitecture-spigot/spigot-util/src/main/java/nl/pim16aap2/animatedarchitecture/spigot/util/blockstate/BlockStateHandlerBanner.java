package nl.pim16aap2.animatedarchitecture.spigot.util.blockstate;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.bukkit.DyeColor;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.banner.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Handler for {@link Banner} block states.
 */
@Singleton
final class BlockStateHandlerBanner extends BlockStateHandler<Banner>
{
    private static final Type PATTERN_LIST_TYPE = new TypeToken<List<Pattern>>() {}.getType();

    @Inject
    BlockStateHandlerBanner()
    {
        super(Banner.class);
    }

    private void applyData(Banner target, DyeColor baseColor, List<Pattern> patterns)
    {
        target.setBaseColor(baseColor);
        target.setPatterns(patterns);
        target.update(true, false);
    }

    @Override
    protected void applyBlockState(Banner source, Banner target, Block block)
    {
        DyeColor x;
        applyData(target, source.getBaseColor(), source.getPatterns());
    }

    @Override
    protected void appendSerializedData(Gson gson, Banner source, JsonObject jsonObject)
    {
        final JsonObject bannerJson = new JsonObject();

        bannerJson.add("baseColor", gson.toJsonTree(source.getBaseColor()));
        bannerJson.add("patterns", gson.toJsonTree(source.getPatterns()));

        jsonObject.add("banner", bannerJson);
    }

    @Override
    protected void applySerializedBlockState(Gson gson, Banner target, JsonObject serializedBlockState)
    {
        final JsonObject bannerJson = serializedBlockState.get("banner").getAsJsonObject();

        final DyeColor dyeColor = gson.fromJson(bannerJson.get("baseColor"), DyeColor.class);
        final List<Pattern> patterns = gson.fromJson(bannerJson.get("patterns"), PATTERN_LIST_TYPE);

        applyData(target, dyeColor, patterns);
    }
}
