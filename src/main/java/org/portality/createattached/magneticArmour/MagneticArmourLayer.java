package org.portality.createattached.magneticArmour;

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
import org.portality.createattached.index.AttachedPartalModel;

public class MagneticArmourLayer <T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    public MagneticArmourLayer(RenderLayerParent<T, M> p_117346_) {
        super(p_117346_);
    }

    @Override
    public void render(PoseStack ms, MultiBufferSource buffer, int light, LivingEntity entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitc) {

        MagneticArmourItem boots = MagneticArmourItem.getWornBy(entity, EquipmentSlot.FEET);
        MagneticArmourItem leggings = MagneticArmourItem.getWornBy(entity, EquipmentSlot.LEGS);
        MagneticArmourItem chestplate = MagneticArmourItem.getWornBy(entity, EquipmentSlot.CHEST);
        MagneticArmourItem head = MagneticArmourItem.getWornBy(entity, EquipmentSlot.HEAD);

        if (entity.getPose() == Pose.SLEEPING)
            return;

        M entityModel = getParentModel();
        if (!(entityModel instanceof HumanoidModel<?> model))
            return;

        BlockState renderedState = Blocks.ANDESITE.defaultBlockState();
        VertexConsumer vc = buffer.getBuffer(Sheets.cutoutBlockSheet());

        SuperByteBuffer boot = CachedBuffers.partial(AttachedPartalModel.MAGNET_BOOT, renderedState);
        SuperByteBuffer leg = CachedBuffers.partial(AttachedPartalModel.MAGNET_LEG, renderedState);
        SuperByteBuffer arm = CachedBuffers.partial(AttachedPartalModel.MAGNET_ARM, renderedState);
        SuperByteBuffer chest = CachedBuffers.partial(AttachedPartalModel.MAGNET_PLATE, renderedState);
        SuperByteBuffer helmet = CachedBuffers.partial(AttachedPartalModel.MAGNET_HELMET, renderedState);

        if(boots != null){
            ms.pushPose();

            model.rightLeg.translateAndRotate(ms);

            ms.translate(-5.5f / 16f, 12 / 16f, 8/16f);
            ms.scale(1, -1, -1);

            boot.center()
                    .rotateYDegrees(180)
                    .uncenter();

            boot.disableDiffuse()
                    .light(light)
                    .renderInto(ms, vc);

            ms.popPose();

            ms.pushPose();

            model.leftLeg.translateAndRotate(ms);

            ms.translate(-5.5f / 16f, 12 / 16f, 8/16f);
            ms.scale(1, -1, -1);

            boot.center()
                    .rotateYDegrees(180)
                    .uncenter();

            boot.disableDiffuse()
                    .light(light)
                    .renderInto(ms, vc);

            ms.popPose();
        }


        if(leggings != null){
            ms.pushPose();

            model.leftLeg.translateAndRotate(ms);

            ms.translate(-9.5f / 16f, 7.5F / 16f, 8/16f);
            ms.scale(1, -1, -1);

            leg.center()
                    .rotateYDegrees(180)
                    .uncenter();

            leg.disableDiffuse()
                    .light(light)
                    .renderInto(ms, vc);

            ms.popPose();

            ms.pushPose();

            model.rightLeg.translateAndRotate(ms);

            ms.translate(-10.5f / 16f, 7.5F / 16f, 8/16f);
            ms.scale(1, -1, -1);

            leg.center()
                    .rotateYDegrees(180)
                    .uncenter();

            leg.disableDiffuse()
                    .light(light)
                    .renderInto(ms, vc);

            ms.popPose();
        }

        if(chestplate != null){
            ms.pushPose();

            model.body.translateAndRotate(ms);

            ms.translate(-(8f) / 16f, 13F / 16f, 8/16f);
            ms.scale(1, -1, -1);

            chest.center()
                    .rotateYDegrees(180)
                    .uncenter();

            chest.disableDiffuse()
                    .light(light)
                    .renderInto(ms, vc);

            ms.popPose();


            ms.pushPose();

            model.rightArm.translateAndRotate(ms);

            ms.translate(-15 / 16f, 10F / 16f, 8/16f);
            ms.scale(1, -1, -1);

            arm.center()
                    .rotateYDegrees(180)
                    .uncenter();

            arm.disableDiffuse()
                    .light(light)
                    .renderInto(ms, vc);

            ms.popPose();

            ms.pushPose();

            model.leftArm.translateAndRotate(ms);

            ms.translate(-13 / 16f, 10F / 16f, 8/16f);
            ms.scale(1, -1, -1);

            arm.center()
                    .rotateYDegrees(180)
                    .uncenter();

            arm.disableDiffuse()
                    .light(light)
                    .renderInto(ms, vc);

            ms.popPose();
        }

        if(head != null){
            ms.pushPose();

            model.head.translateAndRotate(ms);

            ms.translate(-(8f) / 16f, -5F / 16f, 8/16f);
            ms.scale(1, -1, -1);

            helmet.center()
                    .rotateYDegrees(180)
                    .uncenter();

            helmet.disableDiffuse()
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
        MagneticArmourLayer<?, ?> layer = new MagneticArmourLayer<>(livingRenderer);
        livingRenderer.addLayer((MagneticArmourLayer) layer);
    }
}
