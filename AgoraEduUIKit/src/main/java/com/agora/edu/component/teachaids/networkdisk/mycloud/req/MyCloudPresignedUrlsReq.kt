package com.agora.edu.component.teachaids.networkdisk.mycloud.req


class MyCloudPresignedUrlsReq(
    val resourceName: String,
    val contentType: String,
    val ext:String?,
    val size:Long,
    val conversion: Conversion?
)