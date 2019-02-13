package com.zirtia.client.handler;

import com.zirtia.client.RPCClient;
import com.zirtia.protocol.StandardProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;


public class RPCClientHandler extends SimpleChannelInboundHandler<Object> {

    private RPCClient rpcClient;

    public RPCClientHandler(RPCClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object fullResponse) throws Exception {
    	StandardProtocol.instance().processResponse(rpcClient, fullResponse);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

}
