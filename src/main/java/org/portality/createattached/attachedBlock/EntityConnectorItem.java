package org.portality.createattached.attachedBlock;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.portality.createattached.index.AttachedIndex;
import org.portality.createattached.physics.AttachedConstraint;
import org.portality.createattached.physics.PlayerPhysicHandler;

import java.util.UUID;

public class EntityConnectorItem extends Item {
    public EntityConnectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if(level.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        ItemStack stack = context.getItemInHand();

        UUID uuid = stack.getOrDefault(AttachedIndex.ATTACHED_ENTITY, null);
        if(uuid == null) return InteractionResult.SUCCESS;

        if(be instanceof AttachedBE attachedBE){
            SubLevel subLevel = Sable.HELPER.getContaining(attachedBE);
            if(subLevel == null) return InteractionResult.SUCCESS;
            UUID subLevelId = subLevel.getUniqueId();

            attachedBE.setAttachedEntity(uuid);
            PlayerPhysicHandler.sublevelToEntity.put(subLevelId, uuid);
            attachedBE.followingPlayer = true;

            attachedBE.setConstraint(new AttachedConstraint(uuid, 1, null));
            attachedBE.sendData();

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }
}
