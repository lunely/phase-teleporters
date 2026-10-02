# Phase Teleporters

A lightweight Fabric technology mod for Minecraft focused on teleportation, machines, energy and convenient travel between dimensions.

Phase Teleporters is designed for technology-focused modpacks with many dimensions and hard-to-reach worlds, especially space-themed setups using mods such as Galacticraft.

The goal is simple: provide a compact technological progression around teleportation without requiring a huge technology mod just to get reliable interdimensional travel.

> **Note:** Phase Teleporters is not designed as a standalone mod. It is intended to be used alongside other technology and progression mods as part of a larger modpack.
**Recommended:** Play with JEI installed to easily view Phase Teleporters recipes.

## Features

### Local Teleports
Create teleport networks for fast travel within the same dimension.

- Public and private frequencies
- Custom frequency colors
- Multiple teleports can share the same frequency
- Configurable teleport sizes

### Interdimensional Teleports
Connect teleports between different dimensions.

- Separate frequency system from Local Teleports
- Works with vanilla and modded dimensions
- Public and private frequencies
- Custom teleport colors

### Localization

English (`en_us`) and Russian (`ru_ru`) are maintained during development. New items and UI text are translated into these two languages first. All other translations are updated before release.

The mod currently includes 38 locales: `en_us`, `ru_ru`, `de_de`, `fr_fr`, `es_es`, `pt_br`, `pl_pl`, `uk_ua`, `zh_cn`, `zh_tw`, `ja_jp`, `ko_kr`, `tr_tr`, `it_it`, `nl_nl`, `cs_cz`, `da_dk`, `sv_se`, `no_no`, `fi_fi`, `hu_hu`, `ro_ro`, `el_gr`, `he_il`, `ar_sa`, `id_id`, `vi_vn`, `th_th`, `bg_bg`, `hr_hr`, `sk_sk`, `sl_si`, `lt_lt`, `lv_lv`, `et_ee`, `ca_es`, `es_mx` and `pt_pt`.

### Energy

Phase Teleporters measures energy in joules: **J**, **kJ**, and **MJ**.

Machines and teleports use joules to operate.

The Solar Panel generates **250 J/t** during the day, or **110 J/t** in rain, with an unobstructed sky above it. It does not generate at night or in dimensions without a sky. Its buffer holds **5 kJ** and its total output is limited to **250 J/t** across all faces. Output, redstone control and security use the same settings as the other machines.

Optional **Team Reborn Energy API 4.1.x** support allows energy exchange with compatible mods, including Tech Reborn. Install the Energy API library alongside the mod to enable this integration.

The exchange rate is **1 API energy unit = 6 J**. Configured input/output sides and transfer limits apply to cross-mod transfers. Energy displays use joules; the mod also works without the library. Existing saved energy values are unchanged.

Quantum teleporters expose item input/output through the **Fabric Transfer API** included in Fabric API. Compatible pipes can insert into an item input to deliver directly to inventories beside remote outputs on the same frequency, or extract from an item output to pull from inventories beside remote inputs. Endpoints must be loaded and enabled. A transfer accepts only what its destination can receive; the teleporter holds no cargo, and configuration slots are never exposed to pipes. Compatibility with a particular pipe mod still depends on its support for Fabric item storage.

### Machines

Quantum teleports also route any **FluidVariant** (including modded fluids and their data components) through **Fabric Transfer API**. Configure **Fluid input / Fluid output** or **Any: input / Any: output** on endpoints sharing a frequency. Compatible pipes may push into inputs or pull from outputs; adjacent fluid stores can also transfer directly, up to one bucket per input face per tick. There is no internal fluid tank: unavailable, full, unloaded or incompatible destinations leave fluid at the source. Transfers use the caller's transaction, including rollback and simulation.

Ad Astra fluid pipes have optional direct compatibility: quantum teleports can access connected tanks through Desh/Ostrum fluid pipes and fluid pipe ducts even though those pipes expose no Fabric fluid storage. Pipe connections set with the wrench, tank sides, loaded chunks and pipe transfer rates are respected. At the source tank use Extract, at the receiving tank use Insert; Normal connections also allow transfer. At the teleport end, Insert feeds its input and Extract draws from its output. No Ad Astra dependency is required.

Phase Teleporters includes its own machines used for processing materials and progressing through teleportation technology.

### Anchor Upgrade

Teleports can use an **Upgrade: Anchor**.

When installed, it keeps the chunk containing the teleport controller loaded.

## Modpack Use

Phase Teleporters is intended to work well in modpacks with many dimensions.

It is especially useful for progression-heavy or space-themed packs where repeatedly travelling between distant worlds can become inconvenient.
