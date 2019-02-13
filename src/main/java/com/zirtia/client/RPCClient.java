package com.zirtia.client;

import com.zirtia.client.channel.ChannelInfo;
import com.zirtia.client.channel.RPCChannelGroup;
import com.zirtia.client.endpoint.EndPoint;
import com.zirtia.client.endpoint.EndPointSupport;
import com.zirtia.client.handler.RPCClientHandler;
import com.zirtia.protocol.RPCRequestEncoder;
import com.zirtia.protocol.RPCResponseDecoder;
import com.zirtia.utils.CustomThreadFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.*;
import java.util.concurrent.*;

import static com.zirtia.client.RPCClientOptions.*;

public class RPCClient {

    private Bootstrap bootstrap;
    private ConcurrentMap<Long, RPCFuture> pendingRPC;
    private ScheduledExecutorService timeoutTimer;
    private CopyOnWriteArrayList<RPCChannelGroup> allConnections;
    private EndPointSupport endPointSupport;
    private Random random = new Random(System.currentTimeMillis());
    
    public RPCClient(EndPoint endPoint) {
        this(endPoint, new RPCClientOptions());
    }

    public RPCClient(EndPoint endPoint, RPCClientOptions options) {
        this.init();
        endPointSupport.updateEndPoints(endPoint);
    }
    public RPCClient(List<EndPoint> endPoints) {
        this(endPoints, new RPCClientOptions());
    }

    public RPCClient(List<EndPoint> endPoints, RPCClientOptions options) {
        this.init();
        endPointSupport.updateEndPoints(endPoints);
    }

    // the right ipPorts format is 10.1.1.1:8888,10.2.2.2:9999
    public RPCClient(String ipPorts) {
        this(ipPorts, new RPCClientOptions());
    }

    public RPCClient(String ipPorts, RPCClientOptions options) {
        this.init();
        endPointSupport.updateEndPoints(ipPorts);
    }

    private void init() {
        pendingRPC = new ConcurrentHashMap<Long, RPCFuture>();
        timeoutTimer = Executors.newScheduledThreadPool(1,new CustomThreadFactory("timeout-timer-thread"));
        this.allConnections = new CopyOnWriteArrayList<RPCChannelGroup>();


        bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, keepAlive);
        bootstrap.option(ChannelOption.SO_REUSEADDR, reuseAddr);
        bootstrap.option(ChannelOption.TCP_NODELAY, tcpNoDelay);
        bootstrap.option(ChannelOption.SO_RCVBUF, receiveBufferSize);
        bootstrap.option(ChannelOption.SO_SNDBUF, sendBufferSize);
        ChannelInitializer<SocketChannel> initializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new RPCRequestEncoder());
                ch.pipeline().addLast(new RPCResponseDecoder());
                ch.pipeline().addLast(new RPCClientHandler(RPCClient.this));
            }
        };
        bootstrap.group(new NioEventLoopGroup(ioThreadNum,new CustomThreadFactory("client-io-thread")))
        	.handler(initializer);
        this.endPointSupport = new EndPointSupport(allConnections,bootstrap, new CopyOnWriteArrayList<EndPoint>());
    }
    
    public void stop() {
        if (bootstrap.config().group() != null) {
            bootstrap.config().group().shutdownGracefully();
        }
        for (RPCChannelGroup connectionsPerHost : allConnections) {
            connectionsPerHost.close();
        }
        if (timeoutTimer != null) {
            timeoutTimer.shutdown();
        }
    }

    public <T> Future<T> sendRequest(
            final Long callId, Object fullRequest,
            Class<T> responseClass, RPCCallback<T> callback) {
    	
        ChannelInfo channelInfo = selectChannel(allConnections);
        if (channelInfo == null || channelInfo.getChannel() == null || !channelInfo.getChannel().isActive()) {
            return null;
        }
        
        ScheduledFuture scheduledFuture = timeoutTimer.schedule(() -> {
            RPCFuture rpcFuture = removeRPCFuture(callId);
            if (rpcFuture != null) {
                rpcFuture.timeout();
            }
        }, readTimeoutMillis, TimeUnit.MILLISECONDS);
        
        RPCFuture future = new RPCFuture(scheduledFuture, callId, responseClass, callback, channelInfo.getChannelGroup());
        addRPCFuture(callId, future);
        channelInfo.getChannel().writeAndFlush(fullRequest);
        return future;
    }

    public ChannelInfo selectChannel(CopyOnWriteArrayList<RPCChannelGroup> allConnections) {
        int totalHostCount = allConnections.size();
        if (totalHostCount == 0) {
            return null;
        }
        int randomIndex = Math.abs(random.nextInt());
        RPCChannelGroup channelGroup = allConnections.get(randomIndex % totalHostCount);
        Channel channel = channelGroup.getChannel(randomIndex % channelGroup.getConnectionNum());
        return new ChannelInfo(channelGroup, channel);
    }

    public void addRPCFuture(Long callId, RPCFuture future) {
        pendingRPC.put(callId, future);
    }

    public RPCFuture getRPCFuture(Long callId) {
        return pendingRPC.get(callId);
    }

    public RPCFuture removeRPCFuture(long logId) {
        return pendingRPC.remove(logId);
    }

}
