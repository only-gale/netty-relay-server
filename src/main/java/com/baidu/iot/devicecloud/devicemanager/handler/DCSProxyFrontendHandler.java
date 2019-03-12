package com.baidu.iot.devicecloud.devicemanager.handler;

import com.baidu.iot.devicecloud.devicemanager.bean.TlvMessage;
import com.baidu.iot.devicecloud.devicemanager.codec.TlvDecoder;
import com.baidu.iot.devicecloud.devicemanager.codec.TlvEncoder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

import static com.baidu.iot.devicecloud.devicemanager.util.NettyUtil.closeOnFlush;

/**
 * Getting responsible for the relay client to DCS proxy.
 *
 * Created by Yao Gang (yaogang@baidu.com) on 2019/3/12.
 *
 * @author <a href="mailto:yaogang AT baidu DOT com">Yao Gang</a>
 */
@Slf4j
public class DCSProxyFrontendHandler extends SimpleChannelInboundHandler<TlvMessage> {
    private final String dcsProxyHost;
    private final int dcsProxyPort;

    private ChannelFuture channelFuture;
    private Channel outboundChannel;

    public DCSProxyFrontendHandler(String remoteHost, int remotePort) {
        this.dcsProxyHost = remoteHost;
        this.dcsProxyPort = remotePort;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("{} has connect to the relay server.", ctx.channel().toString());
        final Channel inboundChannel = ctx.channel();
        // Initializing a new connection to DCS proxy for each client connected to the relay server
        Bootstrap dcsProxyClient = new Bootstrap();
        dcsProxyClient
                .group(inboundChannel.eventLoop())      // share the outer channel's eventLoop with the inner
                .channel(ctx.channel().getClass())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                // Inbounds start from below
                                .addLast("tlvDecoder", new TlvDecoder())
                                .addLast(new DCSProxyBackendHandler(inboundChannel))
                                // Inbounds stop at above

                                // Outbounds stop at below
                                .addLast("tlvEncoder", new TlvEncoder("dcsProxyClient"))
                                // Outbounds start from above
                        ;
                    }
                });

        // TODO: the dcs proxy address need to be configured
        channelFuture = dcsProxyClient.connect(dcsProxyHost, dcsProxyPort);
        outboundChannel = channelFuture.channel();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlvMessage msg) throws Exception {
        log.info("The relay server has read a message: {}", msg.toString());
        fixTlv(msg);
        if (channelFuture.isDone()) {
            // do something with received data

            if (outboundChannel != null && outboundChannel.isActive()) {
                outboundChannel
                        .writeAndFlush(msg)
                        .addListener((ChannelFutureListener) future -> {
                            if (!future.isSuccess()) {
                                log.error("The relay server forwarding message has failed: {}", future.cause());
                                future.cause().printStackTrace();
                            }
                        });
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        closeOnFlush(ctx.channel());
    }

    private void fixTlv(TlvMessage tlv) {
        if (tlv != null && !tlv.getValue().isNull()) {
            ObjectNode objectNode = (ObjectNode) tlv.getValue();
            objectNode.set("fixed", new TextNode("by relay server"));
            tlv.setValue(objectNode);

            ObjectMapper om = new ObjectMapper();
            final ObjectWriter writer = om.writer();
            try {
                final byte[] bytes = writer.writeValueAsBytes(objectNode);
                tlv.setLength(bytes.length);
                assert(bytes.length == objectNode.toString().getBytes().length);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }
}
