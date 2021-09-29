package io.agora.agoraeducore.core.internal.report.reporters;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ReportService {
    @POST("v1.0/projects/{appId}/app-dev-report/v1/report")
    Call<ReportResp> report(
            @Path("appId") String appId,
            @Body Object body);
}