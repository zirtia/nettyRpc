package com.zirtia.server;

public class RPCServerOptions {

    // The keep alive
    public static boolean keepAlive;

    public static boolean tcpNoDelay = true;

    // so linger
    public static int soLinger = 5;

    // backlog
    public static int backlog = 1024;

    // receive buffer size
    public static int receiveBufferSize = 1024 * 64;

    // send buffer size
    public static int sendBufferSize = 1024 * 64;

    public static int readerIdleTime = 60;

    public static int writerIdleTime = 60;

    // keepAlive时间（second）
    public static int keepAliveTime;

    // acceptor threads, default use Netty default value
    public static int acceptorThreadNum = 0;

    // io threads, default use Netty default value
    public static int ioThreadNum = 0;

    // real work threads
    public static int workThreadNum = Runtime.getRuntime().availableProcessors() * 2;

}
