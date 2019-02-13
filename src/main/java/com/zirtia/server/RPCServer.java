package com.zirtia.server;

import com.zirtia.protocol.RPCReponseEncoder;
import com.zirtia.protocol.RPCRequestDecoder;
import com.zirtia.server.handler.RPCServerChannelIdleHandler;
import com.zirtia.server.handler.RPCServerHandler;
import com.zirtia.server.service.ServiceManager;
import com.zirtia.utils.CustomThreadFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.zirtia.server.RPCServerOptions.*;

public class RPCServer {

    // 端口
    private int port;

    // netty bootstrap
    private ServerBootstrap bootstrap;

    // netty acceptor thread pool
    private EventLoopGroup bossGroup;

    // netty io thread pool
    private EventLoopGroup workerGroup;

    // business handler thread pool
    private ThreadPoolExecutor workThreadPool;

    public RPCServer(int port) {
        this.port = port;

        this.workThreadPool = new ThreadPoolExecutor(workThreadNum, workThreadNum,
                60L, TimeUnit.SECONDS,  new LinkedBlockingQueue<>(),
                new CustomThreadFactory("worker-thread"));
        this.workThreadPool.prestartAllCoreThreads();

        bootstrap = new ServerBootstrap();
        if (Epoll.isAvailable()) {
            bossGroup = new EpollEventLoopGroup(acceptorThreadNum, new CustomThreadFactory("server-acceptor-thread"));
            workerGroup = new EpollEventLoopGroup(ioThreadNum, new CustomThreadFactory("server-io-thread"));
            ((EpollEventLoopGroup) bossGroup).setIoRatio(100);
            ((EpollEventLoopGroup) workerGroup).setIoRatio(100);
            bootstrap.channel(EpollServerSocketChannel.class);
            bootstrap.option(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED);
            bootstrap.childOption(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED);
        } else {
            bossGroup = new NioEventLoopGroup(acceptorThreadNum,new CustomThreadFactory("server-acceptor-thread"));
            workerGroup = new NioEventLoopGroup(ioThreadNum, new CustomThreadFactory("server-io-thread"));
            ((NioEventLoopGroup) bossGroup).setIoRatio(100);
            ((NioEventLoopGroup) workerGroup).setIoRatio(100);
            bootstrap.channel(NioServerSocketChannel.class);
        }

        bootstrap.option(ChannelOption.SO_BACKLOG, backlog);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, keepAlive);
        bootstrap.childOption(ChannelOption.TCP_NODELAY, tcpNoDelay);
        bootstrap.childOption(ChannelOption.SO_REUSEADDR, true);
        bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.childOption(ChannelOption.SO_LINGER, soLinger);
        bootstrap.childOption(ChannelOption.SO_SNDBUF, sendBufferSize);
        bootstrap.childOption(ChannelOption.SO_RCVBUF, receiveBufferSize);
        bootstrap.group(bossGroup, workerGroup).childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast("idleStateAwareHandler", new IdleStateHandler(readerIdleTime, writerIdleTime, keepAliveTime));
                ch.pipeline().addLast("idle", new RPCServerChannelIdleHandler());
                ch.pipeline().addLast("decoder", new RPCRequestDecoder());
                ch.pipeline().addLast("handler", new RPCServerHandler(RPCServer.this));
                ch.pipeline().addLast("encoder", new RPCReponseEncoder());
            }
        });
    }

    public void start() throws InterruptedException {
            bootstrap.bind(port).sync();
    }

    public void shutdown() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        workThreadPool.shutdown();
    }

    public ThreadPoolExecutor getWorkThreadPool() {
        return workThreadPool;
    }

    public void registerService(Object service) {
        ServiceManager.getInstance().registerService(service);
    }
}
