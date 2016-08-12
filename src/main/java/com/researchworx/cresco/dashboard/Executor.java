package com.researchworx.cresco.dashboard;

import com.researchworx.cresco.library.plugin.core.CExecutor;
import com.researchworx.cresco.library.utilities.CLogger;

public class Executor extends CExecutor {
    private final CLogger logger;

    Executor(Plugin plugin) {
        super(plugin);
        this.logger = new CLogger(this.plugin.getMsgOutQueue(), this.plugin.getRegion(), this.plugin.getAgent(), this.plugin.getPluginID(), CLogger.Level.Trace);
    }
}
