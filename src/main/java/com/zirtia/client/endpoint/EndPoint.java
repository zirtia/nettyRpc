package com.zirtia.client.endpoint;

import java.util.Objects;

public class EndPoint {
    private String ip;
    private int port;

    public EndPoint(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EndPoint)) return false;
        EndPoint endPoint = (EndPoint) o;
        return port == endPoint.port && Objects.equals(ip, endPoint.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

}
