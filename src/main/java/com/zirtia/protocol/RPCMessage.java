package com.zirtia.protocol;

import com.google.protobuf.MessageLite;

public class RPCMessage<T extends MessageLite> {

    private T header;
    private byte[] body;

    public RPCMessage<T> copyFrom(RPCMessage<T> rhs) {
        this.header = rhs.getHeader();
        this.body = rhs.getBody();
        return this;
    }

    public T getHeader() {
        return header;
    }

    public void setHeader(T header) {
        this.header = header;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

}
