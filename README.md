# Mech Arsenal

An addon prototype for Let it Walk! on NeoForge 1.21.1. GeckoLib is required for the articulated autocannon model.

All Mech Arsenal items are grouped in the dedicated **Mech Arsenal** creative tab.

## Autocannon

Craft the Mech Autocannon with iron, redstone, and a dispenser. Craft rounds from an iron nugget and gunpowder. Hold **R** while piloting to fire every installed autocannon, or hold the autocannon and **R** to fire the handheld weapon. Mounted turrets track the pilot's aim and each shot starts at that turret's muzzle. Every cannon consumes one round; walker magazines are used before the pilot's inventory. The server fires 48-block hitscan rounds and enforces a four-tick firing interval.

Craft an Autocannon Mount from an autocannon, iron, and redstone, then place it within 16 blocks of the walker's core in the walker structure. The detailed turret is two blocks tall with a 1.8-block barrel and tracks pilot aim continuously, including between shots. Craft an Armored Ammunition Magazine from iron, a chest, and redstone. It has nine slots and accepts both autocannon rounds and 120 mm shells; place it within 16 blocks of the core to feed matching ammunition to mounted weapons automatically. Turret positions and aim are transformed through Sable's walker pose, so multiple cannons fire from their own muzzles.

Craft 120 mm Heavy Shells from iron and gunpowder. The Stationary 120 mm Cannon places as a one-block-wide, three-block-long, two-block-high L-shaped assembly with a matching collision footprint. Its updated model uses an octagonal barrel profile, joined barrel segments, a detailed muzzle brake, and an armored breech. Install its anchor within 16 blocks of a walker's core; placement sets its fixed firing direction, and the pilot's fire key activates it alongside any rotating mounts. Placed outside a walker, right-click any part with an empty hand to fire. It consumes heavy shells from a nearby magazine, then from the pilot's inventory. Shells follow a ballistic arc, deal impact-area damage without destroying blocks, and produce custom muzzle and impact effects. The heavy report uses Create Big Cannons' cannon sound when available and a vanilla fallback otherwise.

While piloting, a tactical HUD (enabled by default; press **H** to toggle) displays installed autocannon and heavy-cannon counts, available ammunition, and a compact reticle. The custom HUD suppresses only the vanilla crosshair while active, leaving the vanilla status bars and chat visible. Heavy-cannon shots apply a brief camera kick for recoil.

## Development

Requires Java 21. Run `gradlew.bat build` on Windows. The provided Let it Walk! 0.1.4 jar dump is used as a compile-only API reference; the game runtime needs Let it Walk!, Create, Sable, and GeckoLib installed.
