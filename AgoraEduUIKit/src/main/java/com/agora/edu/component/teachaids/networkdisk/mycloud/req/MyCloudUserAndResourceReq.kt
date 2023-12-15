package com.agora.edu.component.teachaids.networkdisk.mycloud.req

class MyCloudUserAndResourceReq(
    val resourceName:String,
    val size:Long,
    val ext:String,
    val url:String,
    val conversion:Conversion? = null,
    val parentResourceUuid: String ="root",
    val type:String = "1"
)

class Conversion{
    var type:String?=null
    var preview:Boolean = true
    var scale:Float = 1.2f
    var outputFormat:String = "png"
}