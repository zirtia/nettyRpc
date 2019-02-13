package com.zirtia.client;

import com.zirtia.client.channel.RPCChannelGroup;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
public class RPCFuture<T> implements Future<Object> {

    private CountDownLatch latch;
    private ScheduledFuture scheduledFuture;
    private Class<T> responseClass;
    private RPCCallback<T> callback;
    private RPCChannelGroup channelGroup;

    private Object response;
    private Throwable error;
    private boolean isDone;

    public RPCFuture(ScheduledFuture scheduledFuture,
                     Long callId,
                     Class<T> responseClass,
                     RPCCallback<T> callback,
                     RPCChannelGroup channelGroup) {
        this.responseClass = responseClass;
        this.scheduledFuture = scheduledFuture;
        this.callback = callback;
        this.channelGroup = channelGroup;
        this.latch = new CountDownLatch(1);
    }

    public void success(Object response) {
        this.response = response;
        scheduledFuture.cancel(true);
        latch.countDown();
        if (callback != null) {
            callback.success((T) response);
        }
        isDone = true;
    }

    public void fail(Throwable error) {
        this.error = error;
        channelGroup.incFailedNum();
        scheduledFuture.cancel(true);
        latch.countDown();
        if (callback != null) {
            callback.fail(error);
        }
        isDone = true;
    }

    public void timeout() {
        this.response = null;
        channelGroup.incFailedNum();
        latch.countDown();
        if (callback != null) {
            callback.fail(new RuntimeException("timeout"));
        }
        isDone = true;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return isDone;
    }

    @Override
    public T get() throws InterruptedException {
        latch.await();
        if (error != null) {
            return null;
        }
        return (T) response;
    }

    @Override
    public T get(long timeout, TimeUnit unit) {
        try {
            if (latch.await(timeout, unit)) {
                if (error != null) {
                    return null;
                }
            } else {
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        return (T) response;
    }

    public Class getResponseClass() {
        return responseClass;
    }
}