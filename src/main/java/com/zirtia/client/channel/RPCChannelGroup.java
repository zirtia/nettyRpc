package com.zirtia.client.channel;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;


public class RPCChannelGroup {

    private static final Logger LOG = LoggerFactory.getLogger(RPCChannelGroup.class);

    private Bootstrap bootstrap;
    private String ip;
    private int port;
    private int connectionNum;
    private ChannelFuture[] channelFutures;
    private ReentrantLock[] locks;
    private AtomicInteger failedNum = new AtomicInteger();

    public RPCChannelGroup(String ip, int port, int connectionNum, Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
        this.ip = ip;
        this.port = port;
        this.connectionNum = connectionNum;
        this.channelFutures = new ChannelFuture[connectionNum];
        this.locks = IntStream.range(0,connectionNum).boxed()
                .map(x -> new ReentrantLock()).toArray(ReentrantLock[]::new);
        this.channelFutures = IntStream.range(0,connectionNum).boxed()
                .map(x -> connect(ip, port)).toArray(ChannelFuture[]::new);
    }

    public Channel getChannel(int index) {
        Validate.isTrue(index >=0 && index < connectionNum);
        if (isChannelValid(channelFutures[index])) {
            return channelFutures[index].channel();
        }

        ReentrantLock lock = locks[index];
        lock.lock();
        try {
            if (isChannelValid(channelFutures[index])) {
                return channelFutures[index].channel();
            }
            channelFutures[index] = connect(ip, port);
            if (channelFutures[index] == null) {
                return null;
            } else {
                channelFutures[index].sync();
                if (channelFutures[index].isSuccess()) {
                    return channelFutures[index].channel();
                } else {
                    return null;
                }
            }
        } catch (Exception ex) {
            LOG.warn("connect to {}:{} failed, msg={}", ip, port, ex.getMessage());
            return null;
        } finally {
            lock.unlock();
        }
    }

    public void close() {
        IntStream.range(0,connectionNum).boxed()
                .filter(i -> channelFutures[i] != null)
                .forEach(i -> channelFutures[i].channel().close());
    }

    private ChannelFuture connect(final String ip, final int port) {
        ChannelFuture future = bootstrap.connect(new InetSocketAddress(ip, port));
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    LOG.info("connect to {}:{} success, channel={}",
                            ip, port, channelFuture.channel());
                } else {
                    LOG.warn("future callback, connect to {}:{} failed due to {}",
                            ip, port, channelFuture.cause().getMessage());
                }
            }
        });
        return future;
    }

    private boolean isChannelValid(ChannelFuture channelFuture) {
        if (channelFuture != null && channelFuture.isSuccess()) {
            Channel channel = channelFuture.channel();
            if (channel != null && channel.isOpen() && channel.isActive()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RPCChannelGroup)) return false;
        RPCChannelGroup that = (RPCChannelGroup) o;
        return port == that.port &&
                connectionNum == that.connectionNum &&
                Objects.equals(bootstrap, that.bootstrap) &&
                Objects.equals(ip, that.ip) &&
                Arrays.equals(channelFutures, that.channelFutures) &&
                Arrays.equals(locks, that.locks) &&
                Objects.equals(failedNum, that.failedNum);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(bootstrap, ip, port, connectionNum, failedNum);
        result = 31 * result + Arrays.hashCode(channelFutures);
        result = 31 * result + Arrays.hashCode(locks);
        return result;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public int getConnectionNum() {
        return connectionNum;
    }

    public void incFailedNum() {
        this.failedNum.incrementAndGet();
    }
}
