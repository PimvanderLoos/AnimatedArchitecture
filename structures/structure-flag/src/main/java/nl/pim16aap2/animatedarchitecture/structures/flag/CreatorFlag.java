package nl.pim16aap2.animatedarchitecture.structures.flag;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.Step;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@ToString(callSuper = true)
public class CreatorFlag extends Creator
{
    private static final StructureType STRUCTURE_TYPE = StructureTypeFlag.get();

    @GuardedBy("this")
    private boolean northSouthAnimated;

    protected CreatorFlag(
        ToolUser.Context context,
        StructureType structureType,
        IPlayer player,
        @Nullable String name)
    {
        super(context, structureType, player, name);
        init();
    }

    public CreatorFlag(ToolUser.Context context, IPlayer player, @Nullable String name)
    {
        this(context, STRUCTURE_TYPE, player, name);
    }

    @Override
    protected synchronized List<Step> generateSteps()
        throws InstantiationException
    {
        return Arrays.asList(
            factoryProvideName.construct(),
            factoryProvideFirstPos
                .textSupplier(text -> text.append(
                    localizer.getMessage("creator.flag.step_1"),
                    TextType.INFO,
                    getStructureArg()))
                .construct(),
            factoryProvideSecondPos
                .textSupplier(text -> text.append(
                    localizer.getMessage("creator.flag.step_2"),
                    TextType.INFO,
                    getStructureArg()))
                .construct(),
            factoryProvideRotationPointPos
                .textSupplier(text -> text.append(
                    localizer.getMessage("creator.flag.step_3"),
                    TextType.INFO,
                    getStructureArg()))
                .construct(),
            factoryProvidePowerBlockPos.construct(),
            factoryReviewResult.construct(),
            factoryConfirmPrice.construct(),
            factoryCompleteProcess.construct());
    }

    @Override
    protected synchronized void giveTool()
    {
        giveTool("tool_user.base.stick_name", "creator.flag.stick_lore");
    }

    @Override
    protected synchronized CompletableFuture<Boolean> provideSecondPos(ILocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return CompletableFuture.completedFuture(false);

        final Vector3Di firstPos = Util.requireNonNull(getFirstPos(), "firstPos");
        final Vector3Di cuboidDims = new Cuboid(firstPos, loc.getPosition()).getDimensions();
        // Flags must have a dimension of 1 along either the x or z axis, as it's a `2d` shape.
        if ((cuboidDims.x() == 1) ^ (cuboidDims.z() == 1))
        {
            northSouthAnimated = cuboidDims.x() == 1;
            return super.provideSecondPos(loc);
        }

        getPlayer().sendError(textFactory, localizer.getMessage("creator.base.second_pos_not_2d"));
        return CompletableFuture.completedFuture(false);
    }

    @Override
    protected synchronized boolean completeSetRotationPointStep(ILocation loc)
    {
        final Cuboid cuboid = Util.requireNonNull(getCuboid(), "cuboid");
        // For flags, the rotation point has to be a corner of the total area.
        // It doesn't make sense to have it in the middle or something; that's now how flags work.
        if ((loc.getBlockX() == cuboid.getMin().x() || loc.getBlockX() == cuboid.getMax().x()) &&
            (loc.getBlockZ() == cuboid.getMin().z() || loc.getBlockZ() == cuboid.getMax().z()))
            return super.completeSetRotationPointStep(loc);

        getPlayer().sendError(textFactory, localizer.getMessage("creator.base.position_not_in_corner"));
        return false;
    }

    @Override
    protected synchronized Structure constructStructure()
    {
        final Cuboid cuboid = Util.requireNonNull(getCuboid(), "cuboid");
        final Vector3Di rotationPoint = getRequiredProperty(Property.ROTATION_POINT);

        if (northSouthAnimated)
            setMovementDirection(
                rotationPoint.z() == cuboid.getMin().z() ? MovementDirection.SOUTH : MovementDirection.NORTH);
        else
            setMovementDirection(
                rotationPoint.x() == cuboid.getMin().x() ? MovementDirection.EAST : MovementDirection.WEST);

        return super.constructStructure();
    }

    /**
     * Gets whether the flag is animated along the north-south axis.
     *
     * @return True if the flag is animated along the north-south axis, false otherwise.
     */
    @SuppressWarnings("unused") // It is used by the generated toString method.
    protected final synchronized boolean isNorthSouthAnimated()
    {
        return northSouthAnimated;
    }
}
