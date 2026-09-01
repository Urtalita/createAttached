package org.portality.createattached.config;

import net.createmod.catnip.config.ConfigBase;

@SuppressWarnings({"unused"})
public class CAServer extends ConfigBase {

    public final ConfigInt player_max_kpg = new ConfigInt("player_max_kpg", 30, 1, Integer.MAX_VALUE, Comments.player_max_kpg);
    public final ConfigInt player_weight = new ConfigInt("player_weight", 15, 1, Integer.MAX_VALUE, Comments.player_weight);

    public final ConfigInt entity_max_kpg = new ConfigInt("entity_max_kpg", 30, 1, Integer.MAX_VALUE, Comments.entity_max_kpg);
    public final ConfigInt entity_weight = new ConfigInt("entity_weight", 10, 1, Integer.MAX_VALUE, Comments.entity_weight);

    @Override
    public String getName() {
        return "server";
    }

    private static class Comments {
        static String player_max_kpg = "how much kpg player can lift while moving";
        static String player_weight = "how much player weights";

        static String entity_max_kpg = "max kpg per block of hitbox volume";
        static String entity_weight = "weight of entity per block of hitbox volume";
    }
}
