package org.portality.createattached.mixin;

import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.portality.createattached.physics.PlayerPhysicHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision.class)
public class SubLevelEntityCollision {

    @ModifyVariable(
            method = "collide",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/objects/ObjectSet;iterator()Lit/unimi/dsi/fastutil/objects/ObjectIterator;",
                    remap = false
            ),
            remap = false
    )
    private static ObjectSet<SubLevel> modifyIntersecting(ObjectSet<SubLevel> original) {
        original.removeIf(PlayerPhysicHandler::isSublevelAttached);
        return original;
    }
}
