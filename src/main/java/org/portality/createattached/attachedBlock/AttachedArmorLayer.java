package org.portality.createattached.attachedBlock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.mixin.accessor.EntityRenderDispatcherAccessor;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.portality.createattached.index.AttachedPartalModel;
import org.portality.createattached.magneticArmour.MagneticArmourItem;

public class AttachedArmorLayer <T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    public AttachedArmorLayer(RenderLayerParent<T, M> p_117346_) {
        super(p_117346_);
    }

    @Override
    public void render(PoseStack ms, MultiBufferSource buffer, int light, LivingEntity entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitc) {

        @Nullable AttachedItem chestplate = AttachedItem.getWornBy(entity, EquipmentSlot.CHEST);

        if (entity.getPose() == Pose.SLEEPING)
            return;

        M entityModel = getParentModel();
        if (!(entityModel instanceof HumanoidModel<?> model))
            return;

        BlockState renderedState = Blocks.ANDESITE.defaultBlockState();
        VertexConsumer vc = buffer.getBuffer(Sheets.cutoutBlockSheet());

        SuperByteBuffer chest = CachedBuffers.partial(AttachedPartalModel.ATTACHED, renderedState);

        if(chestplate != null){
            ms.pushPose();

            model.body.translateAndRotate(ms);

            ms.translate(-(8f) / 16f, 13F / 16f, 8/16f);
            ms.scale(1, -1, -1);

            chest.center()
                    .rotateYDegrees(180)
                    .rotateXDegrees(-90)
                    .uncenter();

            chest.disableDiffuse()
                    .light(light)
                    .renderInto(ms, vc);

            ms.popPose();
        }
    }

    public static void registerOnAll(EntityRenderDispatcher renderManager) {
        for (EntityRenderer<? extends Player> renderer : renderManager.getSkinMap().values())
            registerOn(renderer);
        for (EntityRenderer<?> renderer : ((EntityRenderDispatcherAccessor) renderManager).create$getRenderers().values())
            registerOn(renderer);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerOn(EntityRenderer<?> entityRenderer) {
        if (!(entityRenderer instanceof LivingEntityRenderer<?, ?> livingRenderer))
            return;
        if (!(livingRenderer.getModel() instanceof HumanoidModel))
            return;
        AttachedArmorLayer<?, ?> layer = new AttachedArmorLayer<>(livingRenderer);
        livingRenderer.addLayer((AttachedArmorLayer) layer);
    }
}