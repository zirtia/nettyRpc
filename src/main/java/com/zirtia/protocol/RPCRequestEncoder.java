package com.zirtia.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class RPCRequestEncoder extends MessageToMessageEncoder<Object> {
	
    @Override
    protected void encode(ChannelHandlerContext ctx, Object object, List<Object> out) throws Exception {
	 RPCMessage<RPCHeader.RequestHeader> request = (RPCMessage<RPCHeader.RequestHeader>) object;
	 byte[] headerBytes = request.getHeader().toByteArray();
	
	 // length buffer
	 ByteBuf lengthBuf = Unpooled.buffer(8);
	 lengthBuf.writeInt(headerBytes.length);
	 lengthBuf.writeInt(request.getBody().length);
	
	 ByteBuf outBuf = Unpooled.wrappedBuffer(lengthBuf.array(), headerBytes, request.getBody());
	 out.add(outBuf);
    }
}
