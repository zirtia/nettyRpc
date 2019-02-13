package com.zirtia.server.handler;

import com.zirtia.protocol.StandardProtocol;
import com.zirtia.server.RPCServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;


public class RPCServerHandler extends SimpleChannelInboundHandler<Object> {

    private RPCServer rpcServer;

    public RPCServerHandler(RPCServer rpcServer) {
        this.rpcServer = rpcServer;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx,Object request) throws Exception {
        rpcServer.getWorkThreadPool().submit(() ->
            ctx.channel().writeAndFlush(StandardProtocol.instance().processRequest(request))
        );
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
