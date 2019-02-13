package com.zirtia.protocol;

import java.lang.reflect.Method;

import com.google.protobuf.MessageLite;
import com.zirtia.client.RPCCallback;
import com.zirtia.client.RPCClient;
import com.zirtia.client.RPCFuture;
import com.zirtia.server.service.ServiceInfo;
import com.zirtia.server.service.ServiceManager;

public class StandardProtocol {

    private static StandardProtocol instance = new StandardProtocol();

    public static StandardProtocol instance() {
        return instance;
    }

    public Object newRequest(Long callId, Method method, Object request, RPCCallback callback) {
        RPCMessage<RPCHeader.RequestHeader> fullRequest = new RPCMessage<>();
        RPCHeader.RequestHeader.Builder headerBuilder = RPCHeader.RequestHeader.newBuilder();
        headerBuilder.setCallId(callId);
        RPCMeta rpcMeta = method.getAnnotation(RPCMeta.class);
        headerBuilder.setServiceName(rpcMeta.serviceName());
        headerBuilder.setMethodName(rpcMeta.methodName());
        fullRequest.setHeader(headerBuilder.build());
        try {
            fullRequest.setBody((byte[]) request.getClass().getMethod("toByteArray").invoke(request));
        } catch (Exception ex) {
            return null;
        }

        return fullRequest;
    }
    
    public Object processRequest(Object request) {
        RPCMessage<RPCHeader.RequestHeader> fullRequest = (RPCMessage<RPCHeader.RequestHeader>) request;
        RPCHeader.RequestHeader requestHeader = fullRequest.getHeader();

        RPCHeader.ResponseHeader.Builder responseHeader = RPCHeader.ResponseHeader.newBuilder()
                .setCallId(fullRequest.getHeader().getCallId())
                .setResCode(RPCHeader.ResCode.RES_FAIL);
        RPCMessage<RPCHeader.ResponseHeader> fullResponse = new RPCMessage<RPCHeader.ResponseHeader>();

        String serviceName = requestHeader.getServiceName();
        String methodName = requestHeader.getMethodName();
        ServiceManager serviceManager = ServiceManager.getInstance();
        ServiceInfo serviceInfo = serviceManager.getService(serviceName, methodName);
        if (serviceInfo == null) {
            responseHeader.setResCode(RPCHeader.ResCode.RES_FAIL);
            responseHeader.setResMsg("can not find service info");
            fullResponse.setHeader(responseHeader.build());
            return fullResponse;
        }
        try {
            Method parseMethod = serviceInfo.getParseFromForRequest();
            MessageLite requestMessage = (MessageLite) parseMethod.invoke(null, fullRequest.getBody());
            MessageLite responseMessage =(MessageLite) serviceInfo.getMethod().invoke(serviceInfo.getService(), requestMessage);
            byte[] responseBody = responseMessage.toByteArray();

            responseHeader.setResCode(RPCHeader.ResCode.RES_SUCCESS).setResMsg("");
            fullResponse.setHeader(responseHeader.build());
            fullResponse.setBody(responseBody);
            return fullResponse;
        } catch (Exception ex) {
            responseHeader.setResCode(RPCHeader.ResCode.RES_FAIL);
            responseHeader.setResMsg("invoke method failed");
            fullResponse.setHeader(responseHeader.build());
            return fullResponse;
        }
    }

    public void processResponse(RPCClient rpcClient, Object object) throws Exception {
        RPCMessage<RPCHeader.ResponseHeader> fullResponse = (RPCMessage<RPCHeader.ResponseHeader>) object;
        Long callId = fullResponse.getHeader().getCallId();
        RPCFuture future = rpcClient.getRPCFuture(callId);
        if (future == null) {
            return;
        }
        rpcClient.removeRPCFuture(callId);

        if (fullResponse.getHeader().getResCode() == RPCHeader.ResCode.RES_SUCCESS) {
            Method decodeMethod = future.getResponseClass().getMethod("parseFrom", byte[].class);
            MessageLite responseBody = (MessageLite) decodeMethod.invoke(null, fullResponse.getBody());
            future.success(responseBody);
        } else {
            future.fail(new RuntimeException(fullResponse.getHeader().getResMsg()));
        }
    }
}
