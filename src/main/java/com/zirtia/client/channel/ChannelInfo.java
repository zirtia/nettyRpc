package com.zirtia.client.channel;

import io.netty.channel.Channel;

public class ChannelInfo {
    private RPCChannelGroup channelGroup;
    private Channel channel;

    public ChannelInfo(RPCChannelGroup channelGroup, Channel channel) {
        this.channelGroup = channelGroup;
        this.channel = channel;
    }

    public RPCChannelGroup getChannelGroup() {
        return channelGroup;
    }

    public Channel getChannel() {
        return channel;
    }
}
