# Hide-Units

![icon](icon.png)

### Your base is beautiful. You just can't see it.

Fifty flares parked on your core. A wall of mono spam over the drill grid you're trying to rebuild. That one poly that has decided your cursor is its home. You know the block is *right there* — you just can't click it.

**Hide-Units makes them get out of the way.**

Units near you fade out or vanish completely, so the factory underneath them comes back into view. Move away, they come back. That's it. No more zooming in, no more guessing, no more misclicking a unit instead of the conveyor you meant.

---

## What it does

**🎯 Hides only what's in your way.** Units are hidden inside a radius around an anchor point — not everywhere. The rest of the map stays exactly as it should be, so you keep your situational awareness while the clutter right in front of you disappears.

**📍 Pick your anchor.** Four modes:
- **Player unit** — clears the crowd that follows you around
- **Mouse cursor** — a bubble of clean vision that follows your crosshair
- **Camera center** — cleans up whatever you're looking at
- **All of the above** — for when you want all three at once

**🔧 Set your range.** 1 to 100 tiles. A tight 5-tile cursor bubble for surgical building, or a wide 60-tile dome to blank out a whole battlefront.

**👻 Ghosts, not just invisibility.** Slide the opacity from 0% (units simply aren't there) up to a translucent ghost that lets you see *through* your army instead of losing it. Bodies, shields, engines and trails all fade together — no ugly leftovers floating around.

**🦾 Choose your victims.** A full unit-type picker with every unit in the game as a clickable icon. Hide the flying spam but keep the enemy ground push visible. Hide everything except crawlers. Whatever you want — select all, clear all, or hand-pick. Units from other mods show up in the list too.

**🛡️ Your own unit is never hidden.** You'll always know where you are.

---

## Play anywhere with it

Hide-Units is **purely visual and 100% client-side**. Units still exist, still shoot, still block, still get shot. Nothing about the game changes — only what your eyes have to deal with.

That means the mod is **not required on servers**. Install it, join any server you like, and nobody else even knows it's there. No "mod mismatch", no kicked-on-join, no asking the host to install anything.

Inspired by the hide-units feature from foo's client — except you don't have to replace your whole game with a custom client to get it. It's just a mod.

---

## Using it

Everything is in **Settings → Hide-Units**:

1. Flip on **Hide units near anchor**.
2. Choose your **Anchor mode**, **Hide range** and **Hidden unit opacity**.
3. Open **Unit types...** and pick which units get hidden.

Change any of it mid-game — it takes effect the moment you close the menu.

---

## Small print

Requires Mindustry **build 158 or newer**.

A few honest quirks with the ghost (non-zero opacity) mode:
- Faded units all draw at one layer, so a ghosted ground unit may appear on top of effects that would normally cover it.
- Quasar forcefields keep their normal shield-layer rendering and fade independently with the unit.
- Ordinary hit shields on faded units render as plain shapes.
- Player name tags stay visible even when that player's unit is faded — the tag belongs to the player, not the unit.

Set opacity to 0% and none of that applies: the units are just gone.

---

*Made by Vini230909. Free, open source, and it will never ask you for anything.*
