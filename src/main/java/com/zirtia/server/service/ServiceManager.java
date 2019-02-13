package com.zirtia.server.service;

import com.zirtia.protocol.RPCMeta;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ServiceManager {

	private static volatile ServiceManager instance;

    private Map<String, ServiceInfo> serviceMap;

    public static ServiceManager getInstance() {
        if (instance == null) {
            synchronized (ServiceManager.class) {
                if (instance == null) {
                    instance = new ServiceManager();
                }
            }
        }
        return instance;
    }

    public ServiceManager() {
        this.serviceMap = new HashMap<>();
    }

    public void registerService(Object service) {
        Class clazz = service.getClass().getInterfaces()[0];
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            ServiceInfo serviceInfo = new ServiceInfo();
            RPCMeta rpcMeta = method.getAnnotation(RPCMeta.class);
            serviceInfo.setServiceName(rpcMeta.serviceName());
            serviceInfo.setMethodName(rpcMeta.methodName());
            serviceInfo.setService(service);
            serviceInfo.setMethod(method);
            serviceInfo.setRequestClass(method.getParameterTypes()[0]);
            serviceInfo.setResponseClass(method.getReturnType());
            try {
                Method parseFromMethod = serviceInfo.getRequestClass().getMethod("parseFrom", byte[].class);
                serviceInfo.setParseFromForRequest(parseFromMethod);
            } catch (Exception ex) {
                throw new RuntimeException("getMethod failed, register failed");
            }
            serviceMap.put(serviceInfo.getServiceName() + "." + serviceInfo.getMethodName(), serviceInfo);
        }
    }

    public ServiceInfo getService(String serviceName, String methodName) {
        return serviceMap.get(serviceName + "." + methodName);
    }
}
