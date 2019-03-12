package com.baidu.iot.devicecloud.devicemanager.server;

import com.baidu.iot.devicecloud.devicemanager.bean.TlvMessage;
import com.baidu.iot.devicecloud.devicemanager.codec.TlvDecoder;
import com.baidu.iot.devicecloud.devicemanager.codec.TlvEncoder;
import com.baidu.iot.devicecloud.devicemanager.constant.TlvConstant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.sun.org.apache.xpath.internal.operations.String;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Yao Gang (yaogang@baidu.com) on 2019/3/11.
 *
 * @author <a href="mailto:yaogang AT baidu DOT com">Yao Gang</a>
 */
@Slf4j
public class TcpRelayServer {
    private ConcurrentHashMap<String, String> deviceUUIDToRemoteAddr = new ConcurrentHashMap<>();

    private ChannelGroup relayChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

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
                                .addLast("relayHandler", new RelayClientHandler())

                                .addLast("tlvEncoder", new TlvEncoder("Relay server"));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        try {
            // TODO: the local address need to be configured
            channelFuture = b.bind(8009).sync();
            channelFuture.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("The relay server has bound to {}", 8009);
                } else {
                    log.error("The relay server has failed to bind 8009.");
                    future.cause().printStackTrace();
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Getting responsible for the relay client to DCS proxy.
     */
    private class RelayClientHandler extends SimpleChannelInboundHandler<TlvMessage> {
        private ChannelHandlerContext innerCtx;
        private ChannelFuture channelFuture;

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            log.info("{} has connect to the relay server.", ctx.channel().toString());
            // Initializing a new connection to DCS proxy for each client connected to the relay server
            Bootstrap dcsProxyClient = new Bootstrap();
            dcsProxyClient
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    // Inbounds start from below
                                    .addLast("tlvDecoder", new TlvDecoder())
                                    .addLast(new SimpleChannelInboundHandler<TlvMessage>() {
                                        @Override
                                        protected void channelRead0(ChannelHandlerContext innerCtx0, TlvMessage msg) throws Exception {
                                            log.info("Inner channel {} has read a message: {}", innerCtx0.channel().toString(), msg.toString());
                                            final int type = msg.getType();
                                            // do something with the received msg, like requesting tts

                                            log.debug("The relay server is responding message to {}", ctx.channel().toString());

                                            ChannelFuture channelFuture0 = ctx.channel().writeAndFlush(msg);

                                            channelFuture0.addListeners((ChannelFutureListener) future -> {
                                                if (!future.isSuccess()) {
                                                    log.error("The relay server responding message has failed: {}", future.cause());
                                                    future.cause().printStackTrace();
                                                } else if (type == TlvConstant.TYPE_DOWNSTREAM_FINISH) {
                                                    innerCtx0.close();
                                                    ctx.close();
                                                } else {
                                                    log.info("The relay server has responded successfully.");
                                                }
                                            }, ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                                        }

                                        @Override
                                        public void channelActive(ChannelHandlerContext innerCtx0) throws Exception {
                                            log.debug("Connected {} to DCS: {}", ctx.channel().toString(), innerCtx0.channel().toString());
                                            innerCtx = innerCtx0;
                                            super.channelActive(innerCtx0);
                                        }
                                    })
                                    // Inbounds stop at above

                                    // Outbounds stop at below
                                    .addLast("tlvEncoder", new TlvEncoder("Relay client"))
                                    // Outbounds start from above
                            ;

                        }
                    });
            // share the outer channel's eventLoop with the inner
            dcsProxyClient.group(ctx.channel().eventLoop());

            // TODO: the dcs proxy address need to be configured
            channelFuture = dcsProxyClient.connect(new InetSocketAddress("localhost", 8010));
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, TlvMessage msg) throws Exception {
            log.info("The relay server has read a message: {}", msg.toString());
            fixTlv(msg);
            if (channelFuture.isDone()) {
                // do something with received data

                if (innerCtx != null && innerCtx.channel().isActive()) {
                    ChannelFuture channelFuture = innerCtx.channel().writeAndFlush(msg);

                    channelFuture.addListener((ChannelFutureListener) future -> {
                        if (!future.isSuccess()) {
                            log.error("The relay server forwarding message has failed: {}", future.cause());
                            future.cause().printStackTrace();
                        }
                    });
                }
            }
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
