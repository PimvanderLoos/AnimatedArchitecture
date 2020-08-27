package nl.pim16aap2.bigdoors.testimplementations;

import nl.pim16aap2.bigdoors.api.IBigDoorsToolUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import org.jetbrains.annotations.NotNull;

public class TestBigDoorsToolUtil implements IBigDoorsToolUtil
{
    @Override
    public void giveToPlayer(final @NotNull IPPlayer player, final @NotNull String name, final @NotNull String lore)
    {

    }

    @Override
    public void removeTool(final @NotNull IPPlayer player)
    {

    }

    @Override
    public boolean isPlayerHoldingTool(@NotNull IPPlayer player)
    {
        return true;
    }
}
