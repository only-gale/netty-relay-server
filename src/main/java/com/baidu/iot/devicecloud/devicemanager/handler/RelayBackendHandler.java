package com.baidu.iot.devicecloud.devicemanager.handler;

import com.baidu.iot.devicecloud.devicemanager.bean.TlvMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Yao Gang (yaogang@baidu.com) on 2019/3/12.
 *
 * @author <a href="mailto:yaogang AT baidu DOT com">Yao Gang</a>
 */
@Slf4j
public class RelayBackendHandler extends SimpleChannelInboundHandler<TlvMessage> {
    private final Channel inboundChannel;

    public RelayBackendHandler(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TlvMessage msg) throws Exception {
        log.info("Inner channel {} has read a message: {}", ctx.channel().toString(), msg.toString());
        // do something with the received msg, like requesting tts

        log.debug("The relay server is responding message to {}", inboundChannel.toString());

        inboundChannel
                .writeAndFlush(msg)
                .addListeners((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        log.error("The relay server responding message has failed: {}", future.cause());
                        future.cause().printStackTrace();
                    } else {
                        log.info("The relay server has responded successfully.");
                    }
                }, ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Connected {} to DCS: {}", inboundChannel.toString(), ctx.channel().toString());
        super.channelActive(ctx);
    }
}
