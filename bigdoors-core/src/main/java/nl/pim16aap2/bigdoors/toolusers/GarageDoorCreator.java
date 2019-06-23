package nl.pim16aap2.bigdoors.toolusers;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.MyBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Vector3D;

/**
 * This class represents players in the process of creating doors. Objects of
 * this class are instantiated when the createdoor command is used and they are
 * destroyed after The creation process has been completed successfully or the
 * timer ran out. In EventHandlers this class is used To check whether a user
 * that is left-clicking is a DoorCreator && tell this class a left-click
 * happened.
 **/
public class GarageDoorCreator extends BigDoorCreator
{
    protected String typeString;

    public GarageDoorCreator(BigDoors plugin, Player player, String name)
    {
        super(plugin, player, name, "GARAGEDOOR");
        type = DoorType.GARAGEDOOR;
    }

    @Override
    protected void setOpenDirection()
    {
        int xDepth = Math.abs(one.getBlockX() - two.getBlockX());
        int yDepth = Math.abs(one.getBlockY() - two.getBlockY());

        // If the height is 1 or more, it means the garage door is currently standing upright (closed).
        if (yDepth > 0)
        {
            if (xDepth > 0)
                openDir = RotateDirection.NORTH;
            else
                openDir = RotateDirection.EAST;
        }
        // This part isn't yet allowed by isPosTwoValid and updateEngineLoc, but it might be later on.
        else
        {
            int dX = engine.getBlockX() - one.getBlockX();
            int dZ = engine.getBlockZ() - one.getBlockZ();

            Vector3D dir = new Vector3D(dX < 0 ? -1 : dX > 0 ? 1 : 0, 0, dZ < 0 ? -1 : dZ > 0 ? 1 : 0);
            SpigotUtil.broadcastMessage("GarageDoorCreator: Found dir: " + dir.toString());

            MyBlockFace mbf = MyBlockFace.faceFromDir(new Vector3D(dX < 0 ? -1 : dX > 0 ? 1 : 0, 0, dZ < 0 ? -1 : dZ > 0 ? 1 : 0));
            openDir = RotateDirection.valueOf(mbf.toString());
        }
    }

    // TODO: When an "open" garage door (i.e. flat against the ceiling) is created,
    // Put the engine height at the selected block - length of not-selected axis
    // (if x was selected, use zLen), -2. Also, put the engine 1 further in the
    // Selected axis. I.e. if xSel == xMin, xEng = xMin - 1. If xSel == xMax,
    // xEng = xMax + 1.
    @Override
    protected void updateEngineLoc()
    {
        engine = one;
    }

    // TODO: Allow creation of "open" garagedoors (i.e. flat against the ceiling).
    @Override
    protected boolean isPosTwoValid(Location loc)
    {
        int xDepth = Math.abs(one.getBlockX() - loc.getBlockX());
        int yDepth = Math.abs(one.getBlockY() - loc.getBlockY());
        int zDepth = Math.abs(one.getBlockZ() - loc.getBlockZ());

        if (yDepth > 0)
            updateEngineLoc();

        // Check if it's only 1 deep in exactly 1 direction and at least 2 blocks high.
        return (xDepth == 0 ^ zDepth == 0) && yDepth > 0;
    }
}
