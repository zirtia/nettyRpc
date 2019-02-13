package com.zirtia.client;

public interface RPCCallback<T> {

    void success(T response);

    void fail(Throwable e);

}
