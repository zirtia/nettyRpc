package com.zirtia.api;

import com.zirtia.client.RPCCallback;

import java.util.concurrent.Future;

public interface SampleServiceAsync extends SampleService {
    Future<Sample.SampleResponse> sampleRPC(Sample.SampleRequest request, RPCCallback<Sample.SampleResponse> callback);
}
