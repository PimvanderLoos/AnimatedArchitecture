# BigDoors v1
BigDoors is a plugin for the Minecraft server mod [Spigot](https://spigotmc.org). Its aim is to enhance your server with
animated blocks that can be used to create, amongst other things, big (animated) doors.

This is the repository for v1 of the BigDoors plugin for SpigotMC. A new v2 version is in development [here](https://github.com/PimvanderLoos/BigDoors).

If you have a problem with this plugin, please create an issue or join our [Discord](https://discord.gg/5ykb943).

This plugin is available for download on the [Spigot page](https://www.spigotmc.org/resources/58669/).


# Previews:
### Regular doors:  
![https://i.imgur.com/7zsErlZ.gif](https://i.imgur.com/7zsErlZ.gif)
### Drawbridges:
[YouTube](https://youtu.be/ApawfmWX7Nw)


# Get the latest dev-build:
Download the latest dev-build [here](https://pim16aap2.nl/BigDoors/). If you decide to use it, there are a few things you'll have to keep in mind:

* Dev builds will automatically update to the latest stable release as soon as it's available regardless of your settings.
* The dev-builds get updated rather frequently, so check back often (URL stays the same, though).
* These builds might not be tested as thoroughly as the release builds.
* Dev builds may not always be backward compatible with the latest release build. This can occur if the database schema is changed in a dev-build for example (then the old version can no longer read the data). So be sure to make backups!

An updated list of the changelog can be found [here](https://github.com/PimvanderLoos/BigDoors/blob/v1/changelog.txt).


# List of features:
* Big, **animated** doors of the following types:
  * Big Door
  * Drawbridge
  * Portcullis
  * Sliding Door
* GUI for door management.
* Redstone support.
* Door creator to guide you through the door creation process.
* Translation support.
* Active developer support.
* [Support for various protection plugins](https://github.com/PimvanderLoos/BigDoors/tree/v1/hooks) such as:
  * [GriefDefender](https://www.spigotmc.org/resources/68900/)
  * [GriefPrevention](https://www.spigotmc.org/resources/1884/)
  * [Konquest](https://www.spigotmc.org/resources/92220/)
  * [Lands](https://www.spigotmc.org/resources/53313/)
  * [Medieval Factions](https://www.spigotmc.org/resources/79941/)
  * [PlotSquared](https://www.spigotmc.org/resources/1177/)
  * [RedProtect](https://www.spigotmc.org/resources/15841/)
  * [Towny Advanced](https://www.spigotmc.org/resources/72694/)
  * [WorldGuard](https://dev.bukkit.org/projects/worldguard)
    * Any protection plugin that uses WorldGuard under the hood, such as [LandLord](https://www.spigotmc.org/resources/44398/)
  * More will be added when needed! Just create an issue (or PR!) or post a message on our Discord!


# Add-ons
* [Big Doors Opener](https://www.spigotmc.org/resources/80805/) (Created by Eldoria). It allows you to automatically
open doors based on conditions such as player proximity, player permissions, or time of day!
* [Big Doors Physics](https://modrinth.com/plugin/bigdoorsphysics) (Created by bonn2). It adds collisions to the animated blocks!


# FAQ:
* **Will you add support for version 1.8 of Minecraft?**  
  *- No. Update your server!*
* **How can I control doors from the console/command blocks?**  
  *- When issuing commands from the console/command blocks, you cannot use the door name. Instead, you'll have to use doorUIDs.*


# Translations:
* (0.1.8.35+) [German](https://minecraft.zockerstation.com/bigdoors/), provided by ZockerStation.
* (0.1.8.16) [German](https://pim16aap2.nl/BigDoors/Translations/de_DE.txt), provided by Lord_Junes.
* (0.1.8.15) [Spanish](https://pim16aap2.nl/BigDoors/Translations/es.txt), provided by Link_1213.
* (0.1.8.18) [Norwegian](https://pim16aap2.nl/BigDoors/Translations/no.txt), provided by Themarwik.
* (0.1.8.27) [Japanese](http://pim16aap2.nl/BigDoors/Translations/ja_JP.txt), provided by keiichi.
* (0.1.8.30) [Chinese](https://github.com/sheip9/bigdoors_zhcn_translaton), provided by sheip9.
* (0.1.8.32) [Italian](https://pim16aap2.nl/BigDoors/Translations/it_IT.txt), provided by MiniMoro.

Got your own translation? Let me know!
Note that translations created for older versions may still work. If there are any missing messages you'll be notified in the log.


# Unsupported Environments:
If the plugin detects that it is in an unsupported environment during startup, it will abort its initialization to prevent issues. You can force the plugin to enable anyway by enabling unsafe mode in the config, but doing so is not supported!!
The following servers are considered unsupported:
* CatServer
* Mohist
* Magma
* Glowstone
* Akarin

And using the following plugins together with BigDoors is also unsupported:
* GeyserMC
* ViaRewind


# Video Tutorials:
* [How to create big working animated doors in MineCraft - PirateCraft](https://youtu.be/2Q4mXiBLy4U)
* [How to Use Big Doors!](https://youtu.be/pN0fntO939c)
* [Big Doors Plugin Tutorial - Minecraft Moving Drawbridges, Portcullis, Sliding Doors](https://youtu.be/rhuTdlvV_3w)


Some more examples:
======
**![https://i.imgur.com/O6igfAZ.gif](https://i.imgur.com/O6igfAZ.gif)**
**![https://i.imgur.com/XVzfCRZ.gif](https://i.imgur.com/XVzfCRZ.gif)**
**![https://i.imgur.com/nSaFXLW.gif](https://i.imgur.com/nSaFXLW.gif)**
**![https://i.imgur.com/hWflKfN.gif](https://i.imgur.com/hWflKfN.gif)**
**![https://i.imgur.com/wXQGWWB.gif](https://i.imgur.com/wXQGWWB.gif)**


# Known Issues:
* Because every block in a door is transformed into an entity, larger doors can be taxing on the client.
* Due to their nature, anti-lag plugins tend to remove the entities used by this plugin. This will result in blocks temporarily disappearing and appearing at their opened position after a slight delay. To prevent this, add entities of type "FallingBlock" named "BigDoorsEntity" to their whitelist.


# Commands and Permissions:
*When opening doors from the console/command block, NAMES DO NOT WORK! 
You need to use DoorUIDs there. 
Why? Because names are not unique, while DoorUIDs are. 
This constraint means that your command blocks won't break when someone 
creates a new door with the same name as the one you used.*

The permissions are set up such that `bigdoors.user.*` will give a user access to all
actions that a user should generally be allowed to do. 
Any admin-specific access is locked behind `bigdoors.admin.*`. 
This should provide a good starting point with setting up the plugin.

In all the following commands, any parameter between angled brackets is required (i.e. `<RequiredArgument>`) and
parameters between square brackets are optional (i.e. `[OptionalParameter]`).

Whenever the instructions refer to a `DoorID`, 
this means that you can use either the door's name or its (numerical) UID.

## User Permissions:
<table>
  <tr>
    <th>Permission</th>
    <th>Command(s)</th>
    <th>Explanation</th>
  </tr>
  <tr>
    <th>bigdoors.own.&lt;numberOfDoors&gt;</th>
    <td>-</td>
    <td>Sets the maximum number of doors this group can own. Replace <code>&lt;numberOfDoors&gt;</code> 
        by the actual number of doors. e.g. <code>bigdoors.own.4</code>.</td>
  </tr>
  <tr>
    <th>bigdoors.user.gui</th>
    <td><code>/BigDoors menu</code>
            <br>
            <br>
        <code>/bdm</code></td>
    <td>Opens the BigDoors GUI.</td>
  </tr>
  <tr>
    <th>bigdoors.user.createdoor.&lt;type&gt;</th>
    <td><code>/NewDoor [-BD||-DB||-PC||-SD]</code></td>
    <td><b>Command usage:    </b>Creates a new door of a given type.
                                 Available flags:
                                 <ul>
                                     <li><code>-BD</code>: <u><b>B</b></u>ig<u><b>D</b></u>oor</li>
                                     <li><code>-DB</code>: <u><b>D</b></u>raw<u><b>B</b></u>ridge</li>
                                     <li><code>-PC</code>: <u><b>P</b></u>ort<u><b>c</b></u>ullis</li>
                                     <li><code>-SD</code>: <u><b>S</b></u>liding<u><b>D</b></u>oor</li>
                                 </ul>
                                 Example usage: <code>/newdoor -PC MyNewPortcullis</code>
                                    <br>
                                 When not otherwise specified, this defaults to a big door.
                                    <br>
                                 All these can also be created from the GUI.
                                    <br>
                                    <br>
                                    <br>
        <b>Permission Usage: </b>Replace <code>&lt;type&gt;</code> with one of the following types: 
                                 <ul>
                                     <li><code>door</code>
                                     <li><code>drawbridge</code>
                                     <li><code>slidingdoor</code>
                                     <li><code>portcullis</code>
                                 </ul>
                                 For example: <code>bigdoors.user.createdoor.bigdoor</code></td>
  </tr>
  <tr>
    <th>bigdoors.user.base</th>
    <td><code>/SetBlocksToMove &lt;doorID&gt; &lt;BlocksToMove&gt;</code></td>
    <td>Sets the number of blocks this door will try to move. 
        Only applies to doors such as portcullises and sliding doors.
            <br>
            <br>
        Example usage: <code>/setblockstomove MyDoor 12</code></td>
  </tr>
  <tr>
    <th>bigdoors.user.base</th>
    <td><code>/SetDoorRotation &lt;doorID&gt; &lt;direction&gt;</code></td>
    <td>Changes the direction a door will try to move. Valid directions:
        <ul>
            <li><code>north</code></li>
            <li><code>east</code></li>
            <li><code>south</code></li>
            <li><code>west</code></li>
            <li><code>clockwise</code> (or: <code>clock</code>)</li>
            <li><code>counterclockwise</code> (or: <code>counter</code>)</li>
        </ul>
        Example usage: <code>/setdoorrotation MyDoor north</code></td>
  </tr>
  <tr>
    <th>bigdoors.user.base</th>
    <td><code>/NameDoor &lt;DoorName&gt;</code></td>
    <td>Sets the name of the door when you're in a door creation process.
            <br>
            <br>
        Example usage: <code>/namedoor MyDoor</code></td>
  </tr>
  <tr>
    <th>bigdoors.user.base</th>
    <td><code>/BDCancel</code></td>
    <td>Cancel the current object creation process.</td>
  </tr>
  <tr>
    <th>bigdoors.user.deletedoor</th>
    <td><code>/DelDoor &lt;DoorID&gt;</code></td>
    <td>Allows you to delete a Big Door.</td>
  </tr>
  <tr>
    <th>bigdoors.user.toggledoor</th>
    <td><code>/OpenDoor &lt;DoorID&gt; [DoorID2] ... [DoorID..]</code>
            <br>
            <br>
        <code>/CloseDoor &lt;DoorID&gt; [DoorID2] ... [DoorID..]</code>
            <br>
            <br>
        <code>/ToggleDoor &lt;DoorID&gt; [DoorID2] ... [DoorID..]</code></td>
    <td>Opens, closes, or toggles a door (of any type). More than one door can be provided in a single command.
            <br>
        A door can be specified using either its name (only when executing the command as a player) 
        or its UID (both for players and the console/command blocks.)
            <br>
        To "toggle" a door here means that it will be closed if currently open or opened if currently closed.</td>
  </tr>
  <tr>
    <th>bigdoors.user.listdoors</th>
    <td><code>/ListDoors [DoorName]</code></td>
    <td>List all doors owned by you, with a specific name if provided.</td>
  </tr>
  <tr>
    <th>-</th>
    <td><code>/ListDoors &lt;DoorName || PlayerName || PlayerUUID&gt;</code></td>
    <td>List all doors with a given name (and who owns them) or all doors owned by a player 
        (PlayerName can only be used for online players!).
            <br>
        <b>Only works in the console!</b></td>
  </tr>
  <tr>
    <th>bigdoors.user.doorinfo</th>
    <td><code>/DoorInfo &lt;DoorID&gt;</code></td>
    <td>Allows you to get information about a given door.</td>
  </tr>
  <tr>
    <th>bigdoors.user.relocatepowerblock</th>
    <td><code>/ChangePowerBlockLoc &lt;DoorName&gt;</code></td>
    <td>Allows you to change the location of the powerblock of a given door.
            <br>
        <b>Only works for players!</b></td>
  </tr>
  <tr>
    <th>bigdoors.user.inspectpowerblock</th>
    <td><code>/InspectPowerBlockLoc</code></td>
    <td>Gives you a tool that gives you the door info of any powerblock you hit with it.
            <br>
        <b>Only works for players!</b></td>
  </tr>
  <tr>
    <th>bigdoors.user.addowner</th>
    <td><code>/BigDoors AddOwner &lt;DoorID&gt; &lt;PlayerName&gt;</code></td>
    <td>Adds another user as an owner of the given door.
            <br>
            <br>
        Example usage: <code>/bigdoors addowner MyDoor pim16aap2</code></td>
  </tr>
  <tr>
    <th>bigdoors.user.removeowner</th>
    <td><code>/BigDoors RemoveOwner &lt;DoorID&gt; &lt;PlayerName&gt;</code></td>
    <td>Removes another user as an owner of the given door.
            <br>
            <br>
        Example usage: <code>/bigdoors removeowner MyDoor pim16aap2</code></td>
  </tr>
  <tr>
    <th>bigdoors.user.setclosetime</th>
    <td><code>/SetAutoCloseTime &lt;DoorID&gt; &lt;AutoCloseTime&gt;</code></td>
    <td>Sets the amount of time after which a door will try to close itself after it was opened. 
        Negative values mean the door will not try to automatically close (you can still use redstone, of course).
            <br>
            <br>
        Example usage: <code>/setautoclosetime MyDoor 10</code>, 
        for it to automatically close 10 seconds after opening.</td>
  </tr>
</table>

---

## Admin Permissions:
<table>
  <tr>
    <th>bigdoors.admin.pausedoors</th>
    <td><code>/PauseDoors</code></td>
    <td>Allows an admin to pause all doors. Execute the command again to unpause the doors.</td>
  </tr>
  <tr>
    <th>bigdoors.admin.stopdoors</th>
    <td><code>/StopDoors</code></td>
    <td>Immediately ends all active animations. 
        Any animated blocks are placed in their final position, so no blocks are deleted.</td>
  </tr>
  <tr>
    <th>bigdoors.admin.killbigdoorsentities</th>
    <td><code>/KillBigDoorsEntities</code></td>
    <td>Kills all entities that may have been left behind by BigDoors for one reason or another.
        This should not be necessary, but useful just in case.</td>
  </tr>
  <tr>
    <th>bigdoors.admin.filldoor</th>
    <td><code>/FillDoor &lt;DoorID&gt;</code></td>
    <td>Fills in a door with stone blocks, regardless of whether the user who issues the command is allowed to 
        build there or not or if there are already blocks there.</td>
  </tr>
  <tr>
    <th>bigdoors.admin.version</th>
    <td><code>/BigDoors Version</code></td>
    <td>Gets the version of the plugin. If it's a dev-build, it'll also show you the build number.</td>
  </tr>
  <tr>
    <th>bigdoors.admin.restart</th>
    <td><code>/BigDoors Restart</code></td>
    <td>Restarts the plugin. Almost everything will be reinitialized (config, translation, etc.)</td>
  </tr>
  <tr>
    <th>bigdoors.admin.bypass.&lt;attribute&gt;</th>
    <td>-</td>
    <td>Gives the user access to change the attribute of a door you do not own.
            <br>
        Note that when accessing a door you're not an owner of, you'll have to use its UID!
            <br>
        These are the valid attributes:
            <ul>
                <li><code>addowner</code></li>
                <li><code>blockstomove</code></li>
                <li><code>changetime</code></li>
                <li><code>delete</code></li>
                <li><code>direction</code></li>
                <li><code>info</code></li>
                <li><code>lock</code></li>
                <li><code>relocatepowerblock</code></li>
                <li><code>removeowner</code></li>
                <li><code>toggle</code></li>
            </ul>
    </td>
  </tr>
</table>

---

## Using BigDoors in your project

This project is hosted on [EldoNexus](https://eldonexus.de/#browse/browse:maven-public:nl%2Fpim16aap2%2FBigDoors).

#### As a Maven dependency
```xml
<repositories>
  <repository>
    <id>eldonexus</id>
    <url>https://eldonexus.de/repository/maven-public/</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>nl.pim16aap2</groupId>
    <artifactId>BigDoors</artifactId>
    <version>0.1.8.43</version>
    <scope>provided</scope>
  </dependency>
</dependencies>
```

#### As a Gradle dependency
```
maven {
  name = 'eldonexus'
  url = 'https://eldonexus.de/repository/maven-public/'
}

dependencies {
  compileOnly 'nl.pim16aap2:BigDoors:0.1.8.43'
}
```



## Thanks

As a final note, I'd like to thank [Captain_Chaos](https://dev.bukkit.org/members/captain_chaos) for making his [PorteCoulissante](https://dev.bukkit.org/projects/portecoulissante) plugin. I've used it a lot and it inspired me to make this plugin!
