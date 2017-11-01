package top.dteam.dfx.plugin.implment;

import ro.fortsoft.pf4j.Extension;
import ro.fortsoft.pf4j.Plugin;
import ro.fortsoft.pf4j.PluginWrapper;
import top.dteam.dfx.plugin.Accessible;

import java.util.Map;

public class Plugin2 extends Plugin {

    public Plugin2(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Extension
    public static class Accessible1 implements Accessible {

        @Override
        public Map invoke(Map parameters) {

            System.out.println("Plugin2 is triggered");

            return parameters;
        }

    }
}

