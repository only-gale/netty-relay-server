package com.baidu.iot.devicecloud.devicemanager.server;

import com.baidu.iot.devicecloud.devicemanager.bean.TlvMessage;
import com.baidu.iot.devicecloud.devicemanager.codec.TlvDecoder;
import com.baidu.iot.devicecloud.devicemanager.codec.TlvEncoder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Yao Gang (yaogang@baidu.com) on 2019/3/11.
 *
 * @author <a href="mailto:yaogang AT baidu DOT com">Yao Gang</a>
 */
@Slf4j
public class SimulatedDCSProxy {
    private ChannelFuture channelFuture;

    private EventLoopGroup bossGroup = null;
    private EventLoopGroup workerGroup = null;

    public void start() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        // Init the relay server
        ServerBootstrap b = new ServerBootstrap();

        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast("tlvDecoder", new TlvDecoder())
                                .addLast("echo", new SimpleChannelInboundHandler<TlvMessage>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, TlvMessage msg) throws Exception {
                                        log.info("The simulated DCS proxy has read a message: {}", msg.toString());
                                        fixTlv(msg);
                                        ChannelFuture channelFuture = ctx.channel().write(msg);
                                        ctx.flush();
                                        if (!channelFuture.isSuccess()) {
                                            log.error("The dcs proxy responding message has failed: {}", channelFuture.cause());
                                            channelFuture.cause().printStackTrace();
                                        }
                                    }

                                    @Override
                                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                        log.info("The simulated DCS proxy has accepted a connection: {}", ctx.channel().toString());
                                    }
                                })

                                .addLast("tlvEncoder", new TlvEncoder("DCS proxy"));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        try {
            channelFuture = b.bind(8010).sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (channelFuture != null) {
            try {
                channelFuture.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
    }

    private void fixTlv(TlvMessage tlv) {
        if (tlv != null && !tlv.getValue().isNull()) {
            ObjectNode objectNode = (ObjectNode) tlv.getValue();
            objectNode.set("fixed1", new TextNode("by dcs proxy"));
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
