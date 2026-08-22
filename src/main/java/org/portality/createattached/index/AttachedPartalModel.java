package org.portality.createattached.index;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.box.PackageStyles;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import org.portality.createattached.Createattached;

public class AttachedPartalModel {
    public static final PartialModel
        MAGNET_BOOT = item("magnet_boot"),
        MAGNET_ARM = item("magnet_arm"),
        MAGNET_HELMET = item("magnet_helmet"),
        MAGNET_LEG = item("magnet_leg"),
        MAGNET_PLATE = item("magnet_plate"),

        ATTACHED = block("attached_block");
    ;

    private static PartialModel block(String path) {
        return PartialModel.of(Createattached.asResource("block/" + path));
    }

    private static PartialModel armor(String path){return PartialModel.of(Createattached.asResource("armor/" + path));}

    private static PartialModel item(String path) {
        return PartialModel.of(Createattached.asResource("item/" + path));
    }

    private static PartialModel entity(String path) {
        return PartialModel.of(Createattached.asResource("entity/" + path));
    }

    public static void register(){

    }
}
