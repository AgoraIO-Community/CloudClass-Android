package io.agora.report.v2

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ReportServiceV2 {
    @POST("v2/report")
    fun report(@Body body: ReportRequest): Call<ReportResponse>
}