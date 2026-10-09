# Bond rules

For Minecraft 1.20.1, Fabric, Bond of the Beast 1.1.0-beta_24, and Shape Shifter Curse 1.9.2. Stage numbers below are SSC indices.

## Owners and the Necklace of Clarity

An owner must be free of another owner and have a non-feral form below stage 2. Allay forms retain their existing exception. At stage 2, an unowned, non-feral player may act as an owner while wearing the Necklace of Clarity in the necklace slot. Stage 3 and owned players gain no owner rights from it.

The same permissions apply to signing contracts, opening the grimoire, sending command packets, forced collaring, leads, and armor management. Removing the necklace at stage 2 disables owner controls; re-equipping it restores access. Reaching the final feral stage releases registered pets as before.

Craft the necklace from two gold ingots, two amethyst shards, and one Moondust Crystal Shard:

```text
 G
ACA
 G
```

`G`: gold ingot; `A`: amethyst shard; `C`: Moondust Crystal Shard. This item does not prevent Lunar Oblivion Dust or forced collar replacement.

## Voluntary contracts

Voluntary taming requires the pet's terminal SSC form and both signatures. Subsequent ability unlocks require no pet confirmation. Time accumulates while the pet is alive, online, in survival, and wearing a collar; the owner may be offline.

| Active bond time | Available controls |
| --- | --- |
| Initial contract | Sit, block/interaction/attack restrictions, armor, leads and beds |
| 30 minutes | Recall and protection |
| 2 hours total | Aura and vampirism |
| 6 hours total | Absorption |

The old numeric XP fields remain for saved data compatibility; they do not unlock abilities.

## Forced collaring

Dust stuns for 2.5 seconds, permitting small sidesteps. The owner must hold right click on a nearby, stunned player for 2 seconds while maintaining aim. Releasing, looking away, or losing range cancels without consuming the collar. An occupied necklace slot is replaced, returning its item to the victim's inventory or dropping it if full.

An ordinary collar adds no passive catalyst. An infused collar supplies catalyst slowly and gives a 10-second warning after receiving an SSC curse. Sleeping on a pet bed adds 0.1 instinct per second at stages 0–2; passive growth pauses during resistance.

Forced control uses restrictions, leads, beds, and armor. Sit and block/interaction/attack restrictions unlock at stage 2; armor controls unlock at stage 3. Protection, aura, vampirism, recall, and absorption require a voluntary bond.

Before stage 3, resistance grants a 2-minute freedom window and has a 5-minute cooldown from activation, saved across reconnects. A golden apple releases stages below 2; an enchanted golden apple releases stage 2. At stage 3, destroy the matching signed contract to end the forced bond.

## Attribution

The Necklace of Clarity, recipe, and sprite originate from [dark-neon1101's PR #14](https://github.com/Cleaninn/BondOfTheBeast/pull/14). The merge preserves the original commits and adapts their permission checks to the current bond system.
