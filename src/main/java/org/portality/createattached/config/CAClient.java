package org.portality.createattached.config;

import net.createmod.catnip.config.ConfigBase;

@SuppressWarnings({"all"})
public class CAClient extends ConfigBase {
    public final ConfigGroup client = group(0, "client",
            Comments.client);

    public final ConfigBool interpolation = new ConfigBool("interpolation", true, Comments.client);

    @Override
    public String getName() {return "client";}

    private static class Comments {
        static String client =
                "Client-only settings - If you're looking for general settings, look inside your worlds serverconfig folder!";
        static String interpolation = "Client side interpolation for attached sublevels - turn off if you want to see actual attached sublevel position and rotation";

    }
}
