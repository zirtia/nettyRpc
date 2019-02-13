package com.zirtia.client;

import java.lang.reflect.Method;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.zirtia.protocol.StandardProtocol;
import com.zirtia.utils.IDGenerator;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import static com.zirtia.client.RPCClientOptions.*;


@SuppressWarnings("unchecked")
public class RPCProxy implements MethodInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(RPCProxy.class);

    private RPCClient rpcClient;

    public RPCProxy(RPCClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    public static <T> T getProxy(RPCClient rpcClient, Class clazz) {
        Enhancer en = new Enhancer();
        en.setSuperclass(clazz);
        en.setCallback(new RPCProxy(rpcClient));
        return (T) en.create();
    }

    public Object intercept(Object obj, Method method, Object[] args,MethodProxy proxy) throws Throwable {
        if (method.getParameterTypes().length < 1 || !MessageLite.class.isAssignableFrom(method.getParameterTypes()[0])) {
            return proxy.invokeSuper(obj, args);
        }

        Long callId = IDGenerator.instance().getId();
        StandardProtocol protocol = StandardProtocol.instance();
        RPCCallback callback;
        Class<?> responseClass;
        Object fullRequest;
        if (args.length > 1) {
            callback = (RPCCallback) args[1];
            Method syncMethod = method.getDeclaringClass().getMethod(method.getName(), method.getParameterTypes()[0]);
            responseClass = syncMethod.getReturnType();
            fullRequest = protocol.newRequest(callId, syncMethod, args[0], callback);
        } else {
            callback = null;
            responseClass = method.getReturnType();
            fullRequest = protocol.newRequest(callId, method, args[0], null);
        }

        int currentTryTimes = 0;
        Object response = null;
        while (currentTryTimes++ < maxTryTimes) {
            Future future = rpcClient.sendRequest(callId, fullRequest, responseClass, callback);
            if (future == null) {
                continue;
            }
            if (callback != null) {
                return future;
            } else {
                response = future.get(readTimeoutMillis, TimeUnit.MILLISECONDS);
                if (response != null) {
                    break;
                }
            }
        }
        return response;
    }

}
