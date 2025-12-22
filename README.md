# <img src="src/main/resources/assets/amd-patch-for-voxy/icon.png" width="64" valign="middle" alt="Logo"> AMD Patch for Voxy (Unofficial)

**An open-source, standalone patcher to fix rendering issues with the Voxy mod on AMD GPUs.**

![License](https://img.shields.io/badge/License-GPLv3-blue.svg) ![Status](https://img.shields.io/badge/Status-Unofficial-red)

---

## Important Disclaimer

**This is an UNOFFICIAL patch.**

* This project is **NOT** associated with, endorsed by, supported by, or affiliated with MCRcortex (the creator of Voxy) in any way.
* **Do NOT report bugs** encountered while using this patch to the official Voxy issue trackers or Discord.
* This software is provided "as-is" to enable interoperability for AMD users who cannot currently use the mod.

---

## What is this?

Voxy is an incredible LOD mod, but it currently suffers from critical compatibility issues with AMD Radeon drivers, often resulting in invisible terrain, rendering glitches, or crashes.

This is a **standalone patcher** that uses Mixins to modify rendering behavior at runtime. It aims to make the mod playable on AMD hardware by addressing specific occlusion and rendering conflicts.

## How it Works (Technical & Legal Note)

This `.jar` file contains **NO original code, binaries, or assets** from the Voxy mod.

It is strictly a patcher that utilizes the Fabric/Forge Mixin system to inject necessary fixes into the game's memory as it launches. Because of this, **you must have the original Voxy mod installed** for this patch to work. It does not function on its own.

### Performance Trade-off

To fix the rendering corruption on AMD cards, this patch may need to alter or disable specific optimization techniques used by Voxy (such as occlusion culling) in certain scenarios.

**What this means for you:** While the game will render correctly, you may experience lower FPS compared to Nvidia users in visually complex scenes. This is a known and necessary trade-off to get the mod working on AMD architecture.

## Tested Hardware

The community has reported successful tests on the following GPU architectures:

* **RX 500 Series:** (e.g., RX 550, RX 580) - *Confirmed Working*

> *Have you tested this on RX 6000, 7000, or older cards? Please open an Issue to let us know so we can update this list!*

## Installation

1.  Ensure you have installed the appropriate mod loader (Fabric/Forge) for your Minecraft version.
2.  Download and install the **official Voxy mod** from its official source.
3.  Download the latest release of `AMD-Patch-for-Voxy.jar` from the [Releases page](../../releases) of this repository.
4.  Place **both** the official Voxy jar AND this patch jar into your `.minecraft/mods` folder.
5.  Launch the game.

## Contributing

This project is **Open Source**. Unlike the original mod, we believe in community collaboration.
If you know how to improve performance or fix other bugs, **Pull Requests are welcome!**

## License

This project is licensed under the **GNU General Public License v3.0 (GPLv3)**.

**What this means:**
* You are free to use, copy, modify, and distribute this software.
* **If you modify and distribute this patch, you MUST keep it Open Source** under the same license.
* You must preserve the author's copyright and credits.

> **License Note:**
> The GPLv3 license applies **ONLY** to the source code within this repository (the Mixins/Patch). It does **NOT** extend to the original Voxy mod, which remains the proprietary intellectual property of MCRcortex and is subject to its own "All Rights Reserved" license.

## Credits

* **Patch Author:** Marquinhos0550
* **Voxy Original Creator:** MCRcortex (All rights to the original Voxy mod belong to him).

---
*Run with freedom.*
