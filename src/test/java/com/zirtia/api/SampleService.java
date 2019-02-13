package com.zirtia.api;


import com.zirtia.protocol.RPCMeta;

public interface SampleService {
    /**
     * 当需要定制serviceName和methodName时，用RPCMeta注解。
     * RPCMeta可选，默认是通过反射获取。
     */
    @RPCMeta(serviceName = "SampleService", methodName = "sampleRPC")
    Sample.SampleResponse sampleRPC(Sample.SampleRequest request);
}
