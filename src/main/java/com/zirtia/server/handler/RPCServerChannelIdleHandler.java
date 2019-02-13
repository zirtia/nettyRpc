package com.zirtia.server.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * 关闭空闲连接
 */
public class RPCServerChannelIdleHandler extends ChannelDuplexHandler {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        if (event instanceof IdleStateEvent) {
            if (((IdleStateEvent) event).state() == IdleState.ALL_IDLE) {
                ctx.close();
            }
        }
    }

}
