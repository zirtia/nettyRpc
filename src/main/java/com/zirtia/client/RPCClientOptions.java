package com.zirtia.client;

public class RPCClientOptions {

    public static int connectTimeoutMillis = 1000;

    public static int readTimeoutMillis = 1000;

    public static int maxConnectionNumPerHost = 8;

    public static int maxTryTimes = 3;

    // The keep alive
    public static boolean keepAlive = true;

    public static boolean reuseAddr = true;

    public static boolean tcpNoDelay = true;

    // receive buffer size
    public static int receiveBufferSize = 1024 * 64;

    // send buffer size
    public static int sendBufferSize = 1024 * 64;

    // io threads, default use Netty default value
    public static int ioThreadNum = 0;

}
