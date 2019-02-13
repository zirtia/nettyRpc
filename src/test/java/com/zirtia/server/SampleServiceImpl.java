package com.zirtia.server;

import com.zirtia.api.Sample;
import com.zirtia.api.SampleService;

public class SampleServiceImpl implements SampleService {

    @Override
    public Sample.SampleResponse sampleRPC(Sample.SampleRequest request) {
        String c = request.getB() + request.getA();
        Sample.SampleResponse response = Sample.SampleResponse.newBuilder()
                .setC(c).build();
        if (response != null) {
            System.out.println(response.getC());
        }
        return response;
    }
}
