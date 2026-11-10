package com.islamozroko.pearlmirror.mixin;

import com.islamozroko.pearlmirror.PearlMirrorMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity; // ИСПРАВЛЕНО ПОД 1.21.11
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class EnderPearlSpawnMixin {
    @Inject(method = "addEntity", at = @At("TAIL"))
    private void pearlMirror$onEntityAdded(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof EnderPearlEntity pearl) { // ИСПРАВЛЕНО
            PearlMirrorMod.onPearlSpawned(pearl);
        }
    }
}
