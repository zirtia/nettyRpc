package com.zirtia.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class RPCReponseEncoder extends MessageToMessageEncoder<Object> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Object object, List<Object> out) throws Exception {
	  RPCMessage<RPCHeader.ResponseHeader> response = (RPCMessage<RPCHeader.ResponseHeader>) object;
      byte[] headerBytes = response.getHeader().toByteArray();

      // length buffer
      ByteBuf lengthBuf = Unpooled.buffer(8);
      lengthBuf.writeInt(headerBytes.length);
      lengthBuf.writeInt(response.getBody().length);

      ByteBuf outBuf = Unpooled.wrappedBuffer(lengthBuf.array(), headerBytes, response.getBody());
      out.add(outBuf);
    }
}
