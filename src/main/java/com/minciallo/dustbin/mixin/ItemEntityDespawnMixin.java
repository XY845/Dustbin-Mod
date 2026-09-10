package com.minciallo.dustbin.mixin;

import com.minciallo.dustbin.storage.DustbinStorage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla 5-minute (6000 tick) despawn of dropped items:
 * - Items younger than the configured threshold keep living (vanilla despawn is cancelled).
 * - Once an item reaches the configured age it is moved into the global trash-bin
 *   storage; if the storage is full, the item falls through to the vanilla despawn.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityDespawnMixin {
	// Runs at the start of every item tick on the server: collect items that have
	// reached the configured age. This also supports thresholds below 6000 ticks.
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void mincialloDustbin$onTickHead(CallbackInfo ci) {
		ItemEntity itemEntity = (ItemEntity) (Object) this;
		if (itemEntity.level().isClientSide()) {
			return;
		}
		ServerLevel level = (ServerLevel) itemEntity.level();
		DustbinStorage storage = DustbinStorage.get(level);
		if (itemEntity.getAge() >= storage.getCollectionTicks()) {
			if (storage.tryCollect(itemEntity.getItem())) {
				itemEntity.discard();
				ci.cancel();
			}
			// Storage full -> fall through; the vanilla 6000-tick despawn below will handle it.
		}
	}

	// Targets the "age >= 6000 -> despawn" branch at the end of ItemEntity.tick()
	// (the second discard() call in the method) and cancels it while items are still
	// younger than the configured collection time, so they outlive the vanilla
	// 5-minute despawn. Client-side this also keeps the item rendered until the
	// server actually removes it.
	@Inject(method = "tick",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V",
					ordinal = 1,
					shift = At.Shift.BEFORE),
			cancellable = true,
			require = 1)
	private void mincialloDustbin$cancelVanillaDespawn(CallbackInfo ci) {
		ItemEntity itemEntity = (ItemEntity) (Object) this;
		if (itemEntity.level().isClientSide()) {
			ci.cancel();
			return;
		}
		DustbinStorage storage = DustbinStorage.get((ServerLevel) itemEntity.level());
		if (itemEntity.getAge() < storage.getCollectionTicks()) {
			ci.cancel();
		}
	}
}
