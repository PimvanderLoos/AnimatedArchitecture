# LightKeeper end-to-end tests

From the repository root, with Java 25+ (the current AnimatedArchitecture build requirement):

```sh
mvn -pl :animatedarchitecture-lightkeeper-e2e -am verify
```

Select a test with `"-Dit.test=AnimatedArchitecturePluginIT#plugin_shouldEnableWithVaultFixture"`.
After the initial build, use an ordinary native JUnit class/method run in the IDE. LightKeeper automatically
packages current saved production sources and upstream modules, including Shade and Assembly. No runtime
manifest VM option, IDE plugin or separate preparation command is required.

The starter is a test dependency supplying the framework and coherent JUnit engine/API. The Maven plugin's
inherited `lock-build` goal protects producer outputs before compilation; this module alone binds `prepare-server`.
The test-scoped producer dependency establishes reactor ordering. Its selector uses Assembly's actual main
artifact, `AnimatedArchitecture-Spigot.jar`, rather than guessing a path or requiring a prior `install`.

Vault is an explicit, checksum-pinned fixture for this normal enabled-state assertion. LightKeeper does not infer
or install descriptor dependencies, and it does not require the target plugin to load or enable. A future intentional
missing-Vault scenario should use a distinct preparation variant without that fixture and assert its expected failure.
Methods share class-local world state; separate classes start clean. `@FreshServer` restarts the process while retaining
that class's data. Ordinary unit tests without the extension do not start Minecraft.

Requires LightKeeper `1.2.0-SNAPSHOT` with automatic Maven preparation (LightKeeper PR #159 or newer).
See https://lightkeeper.pim16aap2.nl/ide-testing.html for supported Maven context and runner limitations.
