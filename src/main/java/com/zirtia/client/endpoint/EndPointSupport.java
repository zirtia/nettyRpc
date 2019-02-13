package com.zirtia.client.endpoint;

import com.zirtia.client.channel.RPCChannelGroup;
import io.netty.bootstrap.Bootstrap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.collections4.CollectionUtils;

import static com.zirtia.client.RPCClientOptions.maxConnectionNumPerHost;

public class EndPointSupport {

	private CopyOnWriteArrayList<RPCChannelGroup> allConnections;
    private Bootstrap bootstrap;
    private CopyOnWriteArrayList<EndPoint> endPoints;
    
    public EndPointSupport(CopyOnWriteArrayList<RPCChannelGroup> allConnections,
                           Bootstrap bootstrap,
                           CopyOnWriteArrayList<EndPoint> endPoints) {
		super();
		this.allConnections = allConnections;
		this.bootstrap = bootstrap;
		this.endPoints = endPoints;
	}

	public void updateEndPoints(List<EndPoint> newEndPoints) {
        CollectionUtils.subtract(newEndPoints, endPoints).forEach(x -> addEndPoint(x));
        CollectionUtils.subtract(endPoints, newEndPoints).forEach(x -> deleteEndPoint(x));
    }
	
	public void updateEndPoints(String ipPorts) {
		List<EndPoint> endPoints = parseEndPoints(ipPorts);
		updateEndPoints(endPoints);
    }
	
	public void updateEndPoints(EndPoint newEndPoint) {
		List<EndPoint> newEndPoints = new ArrayList<EndPoint>(1);
		newEndPoints.add(newEndPoint);
		updateEndPoints(newEndPoints);
    }

    private void addEndPoint(EndPoint endPoint) {
        for (RPCChannelGroup channelGroup : allConnections) {
            if (channelGroup.getIp().equals(endPoint.getIp()) && channelGroup.getPort() == endPoint.getPort()) {
                return;
            }
        }
        allConnections.add(new RPCChannelGroup(endPoint.getIp(),endPoint.getPort(), maxConnectionNumPerHost, bootstrap));
        endPoints.add(endPoint);
    }

    private void deleteEndPoint(EndPoint endPoint) {
        RPCChannelGroup channelGroup = null;
        for (RPCChannelGroup item : allConnections) {
            if (item.getIp().equals(endPoint.getIp()) && item.getPort() == endPoint.getPort()) {
                channelGroup = item;
                break;
            }
        }
        if (channelGroup != null) {
            channelGroup.close();
            allConnections.remove(channelGroup);
        }
        endPoints.remove(endPoint);
    }

    private List<EndPoint> parseEndPoints(String serviceList) {
        String[] ipPortSplits = serviceList.split(",");
        List<EndPoint> endPoints = new ArrayList<EndPoint>(ipPortSplits.length);
        for (String ipPort : ipPortSplits) {
            String[] ipPortSplit = ipPort.split(":");
            String ip = ipPortSplit[0];
            int port = Integer.valueOf(ipPortSplit[1]);
            EndPoint endPoint = new EndPoint(ip, port);
            endPoints.add(endPoint);
        }
        return endPoints;
    }
}
