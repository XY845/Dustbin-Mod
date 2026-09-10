# Dustbin 1.0.0

**Minecraft 26.2 ・ Fabric ・ Java 25 ・ CC BY-NC-SA 4.0**

> 掉落物不再无声消失 —— 它们会滑进一个共享的垃圾桶。
> Dropped items no longer vanish silently — they slide into a shared trash bin.

首个正式版本，功能集已稳定。 / First stable release; the feature set is frozen.

![垃圾桶的关闭与打开状态 / Closed and open states](https://raw.githubusercontent.com/MinCiallo/Dustbin-Mod/Fabric/docs/preview.png)

*左：关盖 · 右：开盖（95°）。由方块模型离线渲染。*
*Left: closed · Right: open at 95°. Rendered offline from the block model.*

---

## 概述 / Overview

原版中掉落物会在 5 分钟后直接消失，不留痕迹。本模组改写了这条逻辑：物品存活到设定阈值后**被收入垃圾桶**，你随时可以打开界面把东西取回来。

In vanilla, dropped items simply despawn after 5 minutes. This mod rewrites that path: once an item reaches the configured age it is **collected into a bin**, and you can open the GUI to take it back at any time.

垃圾桶是**兜底机制，不是无限仓库** —— 满了以后，物品回落到原版行为正常消失。

The bin is a **safety net, not unlimited storage** — once full, items fall back to vanilla despawn.

---

## 本版亮点 / What's in 1.0.0

- **取消原版 5 分钟消失**：收集阈值默认 **1 分钟**，可调范围 **1 ~ 1440 分钟**。
  **Vanilla despawn is cancelled**: the collection threshold defaults to **1 minute**, adjustable from **1 to 1440 minutes**.
- **可朝向的桶身**：放置时正面朝向你，盖子朝远离你的一侧翻起 —— 从哪边放，就从哪边打开。
  **Directional bin**: the front faces you when placed and the lid tips away from you — whichever side you place it from is the side it opens towards.
- **石镐及以上可挖**：木镐挖了不掉落；硬度与箱子同档，用镐片刻即掉。
  **Stone pickaxe or better**: a wooden pickaxe yields nothing; hardness matches a chest's, so a pickaxe takes it down in moments.
- **54 格共享存储**：同一维度内所有垃圾桶共用一份存储，不是每个方块各存一份。
  **54 slots, shared per dimension**: every bin in a dimension reads the same storage, not one store per block.
- **一种物品只占一格**：相同 id + 组件视为同种，绝不跨格堆放；每格上限等于该物品自身的堆叠上限（鸡蛋 16、石头 64、工具 1），超出部分直接丢弃。
  **One item type per slot**: matching id + components count as one type and never span slots; the per-slot cap equals the item's own max stack size (eggs 16, stone 64, tools 1), and any overflow is discarded.
- **只取不放的界面**：6×9 标准箱子布局，但所有格子都禁止放入（拖入、Shift 点击均无效）。想往桶里塞东西只有一条路 —— 丢在地上等它自己进去。
  **Take-only GUI**: a standard 6×9 chest layout with placement blocked in every slot (drag and shift-click alike). The only way in is to drop the item on the ground.
- **开盖动画**：打开界面时桶盖掀起，关闭时合上；由 BlockEntityRenderer 逐帧插值，不是瞬间切换。ESC、走远、死亡、切换维度都会触发合盖。
  **Animated lid**: the lid lifts on GUI open and folds back on close, interpolated frame-by-frame by a BlockEntityRenderer — not an instant state swap. ESC, walking away, dying, or changing dimension all close it.
- **管理指令**：`/dustbin clear` 清空并反馈清掉的物品组数；`/dustbin settime <minutes>` 设置收集阈值。
  **Admin commands**: `/dustbin clear` empties the bin and reports how many stacks were cleared; `/dustbin settime <minutes>` sets the collection threshold.

---

## 合成 / Crafting

8 个铁锭围 1 个箱子 / 8 iron ingots around 1 chest:

```
I I I
I C I
I I I
```

`I` = 铁锭 / Iron Ingot ・ `C` = 箱子 / Chest

---

## 指令 / Commands

| 指令 Command | 作用 Description |
| --- | --- |
| `/dustbin clear` | 清空垃圾桶，反馈清掉的物品组数。Empty the bin; reports how many stacks were cleared. |
| `/dustbin settime <minutes>` | 设置收集阈值，范围 1 ~ 1440 分钟。Set the collection threshold (1–1440 minutes). |

单人世界的房主可直接使用；多人服务器需要管理员权限。
Singleplayer hosts can use them directly; multiplayer servers require admin permission.

---

## 安装 / Installation

1. 安装 **Fabric Loader ≥ 0.19.3** 与 **Fabric API**。
   Install **Fabric Loader ≥ 0.19.3** and **Fabric API**.
2. 把 `dustbin-fabric-1.0.0.jar` 放进 `mods/` 目录。
   Put `dustbin-fabric-1.0.0.jar` into your `mods/` folder.
3. 需要 **Java 25** 运行时。
   Requires a **Java 25** runtime.

模组身份标识（mod id）为 `dustbin`，**不随模组端变化**；只有产物文件名带模组端后缀。这样将来从 Fabric 构建切换到其他模组端时，存档里的垃圾桶与桶内物品可以原样保留。

The mod id is `dustbin` and **stays the same across loaders**; only the artifact filename carries a loader suffix. That way, moving a world to another loader keeps every trash bin and its contents intact.

---

## 兼容性 / Compatibility

| 项目 Component | 要求 Requirement |
| --- | --- |
| Minecraft | 26.2 |
| Fabric Loader | ≥ 0.19.3 |
| Fabric API | 必需 / required |
| Java | ≥ 25 |

Fabric 构建通过 Mixin 注入 `ItemEntity#tick`。与其他同样改写掉落物消失逻辑的模组同时使用时**可能冲突**，建议实测后再大规模投入。

The Fabric build mixes into `ItemEntity#tick`. It **may conflict** with other mods that rewrite item despawn behaviour — test before shipping it in a pack.

---

## 已知限制 / Known limitations

- **物品年龄只在所在区块被加载时增长。** 长期未加载的区块里的掉落物不会"到点进桶"，直到有人靠近。
  **Item age only advances while the chunk is loaded.** Items in unloaded chunks are not collected until a player comes back.
- **开盖角度 95°，垂直占用超出方块本身。** 按模型几何实测：盖顶伸到方块顶面**上方约 0.37 格**，因此正上方紧贴方块时必然穿模；盖沿向后探出约 **0.035 格**，把角度压到 85° 以内即可消除。但即便压到 60°，盖顶仍有约 0.33 格高 —— 开盖的垂直占用无法靠调角度规避，只能靠留空解决。
  **The lid opens to 95°**, so it occupies space above the block. Measured from the geometry: the lid top reaches about **0.37 blocks above** the block's top face, so a block directly overhead is always clipped; the tab juts roughly **0.035 blocks** rearward, which disappears below 85°. Even at 60° the lid still stands about 0.33 blocks tall — the vertical footprint of an open lid cannot be designed away by lowering the angle, only accommodated by leaving headroom.
- **存储按维度保存在世界数据里。** 移除本模组后，数据文件仍留在存档中但不再被读取。
  **Storage is saved per dimension** in the world data. If you remove the mod, the file stays in the save but is never read again.

---

## 产物校验 / Artifact verification

建议下载后比对校验值，确认文件完整。

Please verify the checksum after downloading.

```
文件 / File : dustbin-fabric-1.0.0.jar
大小 / Size : 153,680 bytes
MD5         : 35b352e4caa3cc4ba93057f6ef892879
SHA-256     : ac46b4ba2c87c1112e3caca13c5a35c091fe22c16a7e7258cd65e845995b646d
```

---

## 许可证 / License

[CC BY-NC-SA 4.0](https://github.com/MinCiallo/Dustbin-Mod/blob/Fabric/LICENSE) —— 允许使用、修改与再分发，但**必须署名**、**不得用于商业用途**，且衍生作品需以相同协议共享。

[CC BY-NC-SA 4.0](https://github.com/MinCiallo/Dustbin-Mod/blob/Fabric/LICENSE) — use, adaptation and redistribution are permitted, provided you **give attribution**, **do not use it commercially**, and **share derivatives under the same license**.

欢迎提交 Issue 与 Pull Request。 / Issues and pull requests are welcome.
