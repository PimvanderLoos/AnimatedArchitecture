# BigDoors Protection Hooks

BigDoors uses protection hooks to verify players are allowed to create and operate doors in specific areas.

When toggling a door, for example, the hooks are used to make sure that the player responsible for the toggle is allowed 
to break blocks both between the current min/max coordinates of the door, and the potential future min/max coordinates.

---

## Creating a new hook

Pull requests for new hooks are always welcome!

Follow these steps to create a new hook:
1) Create a new Maven submodule in this module. 
2) Add your new module as submodule to the ["hooks" module](https://github.com/PimvanderLoos/BigDoors/blob/v1/hooks/pom.xml).
3) Add your new module as dependency in the ["hooks-bundle" module](https://github.com/PimvanderLoos/BigDoors/blob/v1/hooks/hooks-bundle/pom.xml).
4) Write an implementation of [IProtectionCompat](https://github.com/PimvanderLoos/BigDoors/blob/v1/hooks/hooks-api/src/main/java/nl/pim16aap2/bigDoors/compatibility/IProtectionCompat.java).</br>
   Take a look at the [hook for the Lands plugin](https://github.com/PimvanderLoos/BigDoors/blob/v1/hooks/hook-lands/src/main/java/nl/pim16aap2/bigDoors/compatibility/LandsProtectionCompat.java).</br>
   The implementation should check if a player is allowed to break all blocks in region defined by two locations. 
5) (Optional) Add your plugin to the list of [supported protection plugins](https://github.com/PimvanderLoos/BigDoors/tree/v1#list-of-features).
