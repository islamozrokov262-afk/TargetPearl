package com.islamozroko.pearlmirror.mixin;

import com.islamozroko.pearlmirror.PearlMirrorMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownEnderPearl; // С большой P
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class EnderPearlSpawnMixin {
    @Inject(method = "addEntity", at = @At("TAIL"))
    private void pearlMirror$onEntityAdded(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof ThrownEnderPearl pearl) { // С большой P
            PearlMirrorMod.onPearlSpawned(pearl);
        }
    }
}
