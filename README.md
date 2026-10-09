# Bond of the Beast

Test build 1.1.0-beta_24 for Minecraft 1.20.1, Fabric, and Shape Shifter Curse 1.9.2. Java sources target Java 17; run Gradle with JDK 21.

Voluntary bond abilities unlock while a pet wears its collar: after 30 minutes, 2 hours, and 6 hours. The grimoire shows pet cards with a 3D preview and a button to open controls.

The Necklace of Clarity lets an unowned, non-feral player at SSC stage 2 act as an owner. Wear it in the necklace slot to sign contracts and use owner controls. It grants no owner rights at stage 3 or while bound to another player, and does not prevent forced collaring. Recipe: two gold ingots, two amethyst shards, and one Moondust Crystal Shard. The item and assets originate from [dark-neon1101's PR #14](https://github.com/Cleaninn/BondOfTheBeast/pull/14); its changes are integrated with the shared bond permission checks.

Build with `./gradlew build` (Windows: `gradlew.bat build`).
