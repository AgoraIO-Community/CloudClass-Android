package io.agora.agoraeducore.core.internal.log.service.bean.response;

public class LogParamsRes {
    public int vendor; //1:s3    2:oss
    public String bucketName;
    public String callbackHost; //callback host
    public String callbackUrl;
    public String callbackBody;
    public String callbackContentType;
    public String preSignedUrl;
    public String ossKey;
    public String accessKeyId;
    public String accessKeySecret;
    public String securityToken;
    public String ossEndpoint;
}
