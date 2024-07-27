package nl.pim16aap2.animatedarchitecture.spigot.util.blockstate;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.profile.PlayerProfile;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Handler for {@link Skull} block states.
 */
@Singleton
final class BlockStateHandlerSkull extends BlockStateHandler<Skull>
{
    @Inject
    BlockStateHandlerSkull()
    {
        super(Skull.class);
    }

    private void applySkullData(
        Skull target,
        @Nullable NamespacedKey noteBlockSound,
        @Nullable PlayerProfile ownerProfile)
    {
        target.setNoteBlockSound(noteBlockSound);
        target.setOwnerProfile(ownerProfile);
        target.update(true, false);
    }

    @Override
    protected void applyBlockState(Skull source, Skull target, Block block)
    {
        applySkullData(
            target,
            source.getNoteBlockSound(),
            source.getOwnerProfile()
        );
    }

    @Override
    public void appendSerializedData(Gson gson, Skull src, JsonObject jsonObject)
    {
        final JsonObject jsonSkull = new JsonObject();
        jsonSkull.add("noteBlockSound", gson.toJsonTree(src.getNoteBlockSound()));
        jsonSkull.add("ownerProfile", gson.toJsonTree(src.getOwnerProfile()));
        jsonObject.add("skull", jsonSkull);
    }

    @Override
    protected void applySerializedBlockState(Gson gson, Skull target, JsonObject serializedBlockState)
    {
        final JsonObject jsonSkull = serializedBlockState.getAsJsonObject("skull");

        applySkullData(
            target,
            gson.fromJson(jsonSkull.get("noteBlockSound"), NamespacedKey.class),
            gson.fromJson(jsonSkull.get("ownerProfile"), PlayerProfile.class)
        );
    }
}
