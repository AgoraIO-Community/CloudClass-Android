package io.agora.edu.common.impl;

import org.jetbrains.annotations.NotNull;

import io.agora.edu.common.api.Base;
import io.agora.edu.common.api.Report;

public class ReportImpl extends Base implements Report {

    protected ReportImpl(@NotNull String appId, @NotNull String roomUuid) {
        super(appId, roomUuid);
    }
}
