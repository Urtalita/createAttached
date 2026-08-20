package org.portality.createattached.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Entity.class, priority = 1600)
public class SecondEntityMixinSquared {
    @TargetHandler(
            mixin = "dev.ryanhcode.sable.mixin.entity.entities_in_blocks.EntityMixin",
            name = "checkInsideBlocks"
    )
    @Inject(
            method = "@MixinSquared:Handler",
            at = @At("HEAD"),
            cancellable = true
    )
    public void createAttached$checkInsideBlocks(CallbackInfo ci) {
        ci.cancel();
    }
}
