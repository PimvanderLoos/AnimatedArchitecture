package nl.pim16aap2.bigdoors.commands;

import lombok.experimental.Delegate;

import javax.inject.Inject;

/**
 * Represents a factory for commands.
 *
 * @author Pim
 */
public class CommandFactory
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

    @Inject //
    CommandFactory(AddOwner.IFactory addOwnerFactory, Info.IFactory infoFactory,
                   SetBlocksToMove.IFactory setBlocksToMoveFactory, Confirm.IFactory confirmFactory,
                   InspectPowerBlock.IFactory inspectPowerBlockFactory, Restart.IFactory restartFactory,
                   Version.IFactory versionFactory, Cancel.IFactory cancelFactory, Delete.IFactory deleteFactory,
                   Specify.IFactory specifyFactory, Debug.IFactory debugFactory, Menu.IFactory menuFactory,
                   RemoveOwner.IFactory removeOwnerFactory, SetAutoCloseTime.IFactory setAutoCloseTimeFactory,
                   Toggle.IFactory toggleFactory, SetOpenDirection.IFactory setOpenDirectionFactory,
                   StopDoors.IFactory stopDoorsFactory, Lock.IFactory lockFactory, SetName.IFactory setNameFactory,
                   MovePowerBlock.IFactory movePowerBlockFactory, NewDoor.IFactory newDoorFactory,
                   ListDoors.IFactory listDoorsFactory)
    {
        this.addOwnerFactory = addOwnerFactory;
        this.infoFactory = infoFactory;
        this.setBlocksToMoveFactory = setBlocksToMoveFactory;
        this.confirmFactory = confirmFactory;
        this.inspectPowerBlockFactory = inspectPowerBlockFactory;
        this.restartFactory = restartFactory;
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
    }
}
