package nl.pim16aap2.animatedarchitecture.spigot.util.blockstate;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of {@link BlockStateHandler} for {@link Container}s.
 */
@Singleton
final class BlockStateHandlerContainer extends BlockStateHandler<Container>
{
    @Inject
    BlockStateHandlerContainer()
    {
        super(Container.class);
    }

    @Override
    protected void applyBlockState(Container source, Container target, Block block)
    {
        target.getInventory().setContents(source.getInventory().getContents());
    }

    @Override
    protected void appendSerializedData(Gson gson, Container source, JsonObject jsonObject)
    {
        final JsonObject inventoryJson = new JsonObject();

        inventoryJson.add("contents", gson.toJsonTree(source.getSnapshotInventory().getContents()));

        jsonObject.add("Container", inventoryJson);
    }

    @Override
    protected void applySerializedBlockState(Gson gson, Container target, JsonObject serializedBlockState)
    {
        final JsonObject inventory = serializedBlockState.getAsJsonObject("Container");

        final Inventory targetInventory = target.getInventory();
        targetInventory.setContents(gson.fromJson(inventory.get("contents"), ItemStack[].class));
    }
}
