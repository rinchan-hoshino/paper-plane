# Development

Use JDK 21 to launch Gradle. Profiles request the appropriate Java 17, 21 or 25 toolchain.

```sh
python3 scripts/build.py                         # all 17 published combinations
python3 scripts/build.py --minecraft 1.20.6     # both loaders for one Minecraft version
python3 scripts/build.py --loader fabric        # Fabric across every supported version
./gradlew build                                # default 1.21.1 profile
```

`matrix.json` is the supported-version list. Each profile keeps its loader/build API differences; `shared/java` contains version-independent gameplay and selection sessions. `shared/modern` contains the modern FTB teleport/payment service and request ledger. Minecraft accessors stay in each profile's small `PaperPlanePlatform`. The 1.19.2/1.20.1 profile retains its adapter for the older FTB API and string request IDs.

RinLib is a required dependency in every profile, pulled from Modrinth: the two oldest Minecraft targets use the RinLib 0.2.0 line, newer targets use 1.0.2 (`rinlib_version` in each profile's `gradle.properties`).

Do not replace real client/server verification with source-text checks or compilation alone. Exercise request acceptance, denial and consumption with `/tpa` disabled, and verify the packaged jars in an isolated game instance.
