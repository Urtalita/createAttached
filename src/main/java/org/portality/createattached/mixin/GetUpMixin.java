package org.portality.createattached.mixin;

import dev.ryanhcode.sable.mixinhelpers.CanFallAtleastHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CanFallAtleastHelper.class)
public class GetUpMixin {
    @Inject(
            method = "canFallAtleastWithSubLevels",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void createAttached$canFallLeastWithSubLevels(Level level, AABB aabb, CallbackInfoReturnable<Vector3d> cir) {
        cir.setReturnValue(null);
    }
}
