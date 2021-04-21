package top.dteam.dfx.plugin;

import ro.fortsoft.pf4j.ExtensionPoint;

import java.util.Map;

public interface Accessible extends ExtensionPoint {

    Map invoke(Map parameters);

}
