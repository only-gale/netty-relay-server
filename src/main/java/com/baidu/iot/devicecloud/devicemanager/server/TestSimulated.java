package com.baidu.iot.devicecloud.devicemanager.server;

import com.baidu.iot.devicecloud.devicemanager.client.TlvOverTcpClient;

import java.net.InetSocketAddress;

import static com.baidu.iot.devicecloud.devicemanager.constant.TlvConstant.TYPE_UPSTREAM_INIT;
import static com.baidu.iot.devicecloud.devicemanager.util.TlvUtil.demo;

/**
 * Created by Yao Gang (yaogang@baidu.com) on 2019/3/11.
 *
 * @author <a href="mailto:yaogang AT baidu DOT com">Yao Gang</a>
 */
public class TestSimulated {
    public static void main(String[] args) {
        SimulatedDCSProxy dcsProxy = new SimulatedDCSProxy();
        TcpRelayServer relayServer = new TcpRelayServer();

        dcsProxy.start();
        relayServer.start();

        TlvOverTcpClient tcpClient = new TlvOverTcpClient(new InetSocketAddress("localhost", 8009));
        tcpClient.startClient();
        tcpClient.writeMessage(demo(TYPE_UPSTREAM_INIT, "{\"param\":\"{\\\"device-id\\\":\\\"3F1806118800C678\\\"}\",\"pid\":\"-1001\",\"clientip\":\"xxx.xxx.xxx.xxx\",\"sn\":\"91598059-4d06-447a-878f-551637bcaf89_7\"}"));
    }

}
