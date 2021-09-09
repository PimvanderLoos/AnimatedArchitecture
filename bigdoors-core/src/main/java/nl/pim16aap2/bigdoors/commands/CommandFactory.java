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
    private final AddOwner.Factory addOwnerFactory;
    @Delegate
    private final Info.Factory infoFactory;
    @Delegate
    private final SetBlocksToMove.Factory setBlocksToMoveFactory;
    @Delegate
    private final Confirm.Factory confirmFactory;
    @Delegate
    private final InspectPowerBlock.Factory inspectPowerBlockFactory;
    @Delegate
    private final Restart.Factory restartFactory;
    @Delegate
    private final Version.Factory versionFactory;
    @Delegate
    private final Cancel.Factory cancelFactory;
    @Delegate
    private final Delete.Factory deleteFactory;
    @Delegate
    private final Specify.Factory specifyFactory;
    @Delegate
    private final Debug.Factory debugFactory;
    @Delegate
    private final Menu.Factory menuFactory;
    @Delegate
    private final RemoveOwner.Factory removeOwnerFactory;
    @Delegate
    private final SetAutoCloseTime.Factory setAutoCloseTimeFactory;
    @Delegate
    private final Toggle.Factory toggleFactory;
    @Delegate
    private final SetOpenDirection.Factory setOpenDirectionFactory;
    @Delegate
    private final StopDoors.Factory stopDoorsFactory;
    @Delegate
    private final Lock.Factory lockFactory;
    @Delegate
    private final SetName.Factory setNameFactory;
    @Delegate
    private final MovePowerBlock.Factory movePowerBlockFactory;
    @Delegate
    private final NewDoor.Factory newDoorFactory;
    @Delegate
    private final ListDoors.Factory listDoorsFactory;

    @Inject CommandFactory(AddOwner.Factory addOwnerFactory,
                           Info.Factory infoFactory,
                           SetBlocksToMove.Factory setBlocksToMoveFactory,
                           Confirm.Factory confirmFactory,
                           InspectPowerBlock.Factory inspectPowerBlockFactory,
                           Restart.Factory restartFactory,
                           Version.Factory versionFactory,
                           Cancel.Factory cancelFactory,
                           Delete.Factory deleteFactory,
                           Specify.Factory specifyFactory,
                           Debug.Factory debugFactory,
                           Menu.Factory menuFactory,
                           RemoveOwner.Factory removeOwnerFactory,
                           SetAutoCloseTime.Factory setAutoCloseTimeFactory,
                           Toggle.Factory toggleFactory,
                           SetOpenDirection.Factory setOpenDirectionFactory,
                           StopDoors.Factory stopDoorsFactory,
                           Lock.Factory lockFactory,
                           SetName.Factory setNameFactory,
                           MovePowerBlock.Factory movePowerBlockFactory,
                           NewDoor.Factory newDoorFactory,
                           ListDoors.Factory listDoorsFactory)
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
