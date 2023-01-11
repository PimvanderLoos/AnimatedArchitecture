# BigDoors v2

BigDoors is a plugin for the Minecraft server mod [Spigot](https://spigotmc.org). Its aim is to enhance your server with
animated blocks that can be used to create, amongst other things, big (animated) doors.

BigDoors v2 is the new upcoming release of BigDoors. It is currently still very much a work in progress without a
planned release date.<br>
If you are looking for the current release of BigDoors, please check out
[v1](https://github.com/PimvanderLoos/BigDoors/tree/v1) instead.

## Using v2

### Warnings

Before you try to use v2, please be aware that this project is still very much in development, so:

* There are many unfinished features.
* You will encounter many bugs when using it. I am aware of many of them, so you don't need to report every minor issue
  you encounter.
* Many of the current systems are just placeholders that will be replaced when I get around to them.
* There is currently no upgrade path from v1; this will be added (much) later on in the development process.
* <ins><b>Any doors you create now may be reset at any time</ins></b>.

### Requirements:

* Java 19
* A Spigot server (or a fork) for Minecraft 1.19.3.

### Installation:

* Grab the latest release from the [actions page](https://github.com/PimvanderLoos/BigDoors/actions). I would recommend
  picking a build from the master branch. You will need both `BigDoors-Spigot.zip` and `DoorTypes.zip` files.
* Extract both zip files.
* Place the `BigDoors-Spigot.jar` in the plugins directory of your server.
* Create the following folder (or start the server to generate it automatically): `plugins/BigDoors2/extensions`.
* Move all the jars from the `DoorTypes.zip` file to the new `extensions` directory.
* (Re)start your server.

## Translations

<a href="https://hosted.weblate.org/engage/bigdoors/">
<img src="https://hosted.weblate.org/widgets/bigdoors/-/multi-auto.svg" alt="Translation status" />
</a>

## Compiling BigDoors v2

Compilation requirements:

* Java 19+
* Maven
* Have NMS for 1.19.3 in your local repository. You can get this
  by [running BuildTools](https://www.spigotmc.org/wiki/buildtools/#running-buildtools) for 1.19.3.

You can then compile the project by running the following command in this directory:

```mvn package```

To also run all the tests, static analysis tools etc., you can run the following command:

```mvn -P=errorprone test package checkstyle:checkstyle pmd:check```

The `BigDoors-Spigot.jar` file can then be found in `bigdoors-spigot/spigot-core/target/BigDoors-Spigot.jar`.
The jars for each door type can be found in `bigdoors-doors/DoorTypes/<DoorType>.jar`.
