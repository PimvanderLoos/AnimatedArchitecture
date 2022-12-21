package nl.pim16aap2.bigdoors.commands;

import lombok.Getter;
import lombok.experimental.Delegate;

import javax.inject.Inject;

/**
 * Represents a factory for commands.
 *
 * @author Pim
 */
public final class CommandFactory
{
    @Delegate
    private final AddOwner.IFactory addOwnerFactory;
    @Delegate
    private final Info.IFactory infoFactory;
    @Delegate
    private final SetBlocksToMove.IFactory setBlocksToMoveFactory;
    @Delegate
    private final Confirm.IFactory confirmFactory;
    @Delegate
    private final InspectPowerBlock.IFactory inspectPowerBlockFactory;
    @Delegate
    private final Restart.IFactory restartFactory;
    @Delegate
    private final Version.IFactory versionFactory;
    @Delegate
    private final Cancel.IFactory cancelFactory;
    @Delegate
    private final Delete.IFactory deleteFactory;
    @Delegate
    private final Specify.IFactory specifyFactory;
    @Delegate
    private final Debug.IFactory debugFactory;
    @Delegate
    private final Menu.IFactory menuFactory;
    @Delegate
    private final RemoveOwner.IFactory removeOwnerFactory;
    @Delegate
    private final SetAutoCloseTime.IFactory setAutoCloseTimeFactory;
    @Delegate
    private final Toggle.IFactory toggleFactory;
    @Delegate
    private final SetOpenDirection.IFactory setOpenDirectionFactory;
    @Delegate
    private final StopDoors.IFactory stopDoorsFactory;
    @Delegate
    private final Lock.IFactory lockFactory;
    @Delegate
    private final SetName.IFactory setNameFactory;
    @Delegate
    private final MovePowerBlock.IFactory movePowerBlockFactory;
    @Delegate
    private final NewDoor.IFactory newDoorFactory;
    @Delegate
    private final ListDoors.IFactory listDoorsFactory;

    @Getter
    private final AddOwnerDelayed addOwnerDelayed;
    @Getter
    private final RemoveOwnerDelayed removeOwnerDelayed;
    @Getter
    private final SetAutoCloseTimeDelayed setAutoCloseTimeDelayed;
    @Getter
    private final SetOpenDirectionDelayed setOpenDirectionDelayed;
    @Getter
    private final SetBlocksToMoveDelayed setBlocksToMoveDelayed;

    @Inject //
    CommandFactory(
        AddOwner.IFactory addOwnerFactory, AddOwnerDelayed addOwnerDelayed,
        Cancel.IFactory cancelFactory,
        Confirm.IFactory confirmFactory,
        Debug.IFactory debugFactory, Menu.IFactory menuFactory,
        Delete.IFactory deleteFactory,
        Info.IFactory infoFactory,
        InspectPowerBlock.IFactory inspectPowerBlockFactory,
        ListDoors.IFactory listDoorsFactory,
        Lock.IFactory lockFactory,
        MovePowerBlock.IFactory movePowerBlockFactory,
        NewDoor.IFactory newDoorFactory,
        RemoveOwner.IFactory removeOwnerFactory, RemoveOwnerDelayed removeOwnerDelayed,
        Restart.IFactory restartFactory,
        SetAutoCloseTime.IFactory setAutoCloseTimeFactory, SetAutoCloseTimeDelayed setAutoCloseTimeDelayed,
        SetBlocksToMove.IFactory setBlocksToMoveFactory, SetBlocksToMoveDelayed setBlocksToMoveDelayed,
        SetName.IFactory setNameFactory,
        SetOpenDirection.IFactory setOpenDirectionFactory, SetOpenDirectionDelayed setOpenDirectionDelayed,
        Specify.IFactory specifyFactory,
        StopDoors.IFactory stopDoorsFactory,
        Toggle.IFactory toggleFactory,
        Version.IFactory versionFactory)
    {
        this.addOwnerFactory = addOwnerFactory;
        this.infoFactory = infoFactory;
        this.setBlocksToMoveFactory = setBlocksToMoveFactory;
        this.confirmFactory = confirmFactory;
        this.inspectPowerBlockFactory = inspectPowerBlockFactory;
        this.restartFactory = restartFactory;
        this.setOpenDirectionDelayed = setOpenDirectionDelayed;
        this.versionFactory = versionFactory;
        this.cancelFactory = cancelFactory;
        this.deleteFactory = deleteFactory;
        this.specifyFactory = specifyFactory;
        this.debugFactory = debugFactory;
        this.menuFactory = menuFactory;
        this.removeOwnerFactory = removeOwnerFactory;
        this.setAutoCloseTimeFactory = setAutoCloseTimeFactory;
        this.toggleFactory = toggleFactory;
        this.setOpenDirectionFactory = setOpenDirectionFactory;
        this.stopDoorsFactory = stopDoorsFactory;
        this.lockFactory = lockFactory;
        this.setNameFactory = setNameFactory;
        this.movePowerBlockFactory = movePowerBlockFactory;
        this.newDoorFactory = newDoorFactory;
        this.listDoorsFactory = listDoorsFactory;
        this.addOwnerDelayed = addOwnerDelayed;
        this.removeOwnerDelayed = removeOwnerDelayed;
        this.setAutoCloseTimeDelayed = setAutoCloseTimeDelayed;
        this.setBlocksToMoveDelayed = setBlocksToMoveDelayed;
    }
}
