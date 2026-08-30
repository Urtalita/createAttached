package org.portality.createattached.config;

import com.Portality.createsprings.config.CSStress;
import net.createmod.catnip.config.ConfigBase;

public class CAKinetics extends ConfigBase {

    public final com.Portality.createsprings.config.CSStress stressValues = nested(1, CSStress::new, Comments.stress);

    @Override
    public String getName() {
        return "kinetics";
    }

    private static class Comments {
        static String stress = "Fine tune the kinetic stats of individual components";
    }
}
