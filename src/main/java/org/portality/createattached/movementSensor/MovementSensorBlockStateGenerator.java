package org.portality.createattached.movementSensor;

import com.Portality.createsprings.CreateSprings;
import com.simibubi.create.content.redstone.diodes.AbstractDiodeBlock;
import com.simibubi.create.content.redstone.diodes.ToggleLatchBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.BlockModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import org.portality.createattached.Createattached;

import java.util.ArrayList;
import java.util.List;

public class MovementSensorBlockStateGenerator extends SpecialBlockStateGen {
    private List<ModelFile> models;

    @Override
    protected int getXRotation(BlockState state) {
        Direction facing = state.getValue(RedstoneLinkBlock.FACING);
        return facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 270;
    }

    @Override
    protected int getYRotation(BlockState state) {
        Direction facing = state.getValue(RedstoneLinkBlock.FACING);
        return facing.getAxis()
                .isVertical() ? 180 : horizontalAngle(facing);
    }

    protected <T extends Block> List<ModelFile> createModels(DataGenContext<Block, T> ctx,
                                                             BlockModelProvider prov) {
        List<ModelFile> models = new ArrayList<>(2);
        String name = ctx.getName();
        ResourceLocation off = existing("movement_sensor");
        ResourceLocation on = existing("movement_sensor");

        models.add(prov.withExistingParent(name, on));
        models.add(prov.withExistingParent(name + "_deactivated", off));

        return models;
    }

    protected ResourceLocation existing(String name) {
        return Createattached.asResource("block/" + name);
    }

    protected int getModelIndex(BlockState state) {
        boolean powered = state.getValue(ToggleLatchBlock.POWERED);

        if(powered) return 0;
        return 1;
    }

    @Override
    public final <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov,
                                                      BlockState state) {
        if (models == null)
            models = createModels(ctx, prov.models());
        return models.get(getModelIndex(state));
    }
    }