package com.baidu.iot.devicecloud.devicemanager.codec;

import com.baidu.iot.devicecloud.devicemanager.bean.TlvMessage;
import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.List;

import static com.baidu.iot.devicecloud.devicemanager.util.TlvUtil.isLegalLength;
import static com.baidu.iot.devicecloud.devicemanager.util.TlvUtil.isLegalType;

/**
 * Created by Yao Gang (yaogang@baidu.com) on 2019/3/6.
 *
 * @author <a href="mailto:yaogang AT baidu DOT com">Yao Gang</a>
 */
@Slf4j
public class TlvDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while (true) {
            if (notEnoughData(in)) {
                return;
            }

            in.markReaderIndex();

            int type = in.readUnsignedShortLE();

            if (!isLegalType(type)) {
                closeConnection(ctx);
                return;
            }
            log.debug("TlvDecoder read type: {}", type);

            long len = in.readUnsignedIntLE();

            if (!isLegalLength(len)) {
                closeConnection(ctx);
                return;
            }
            log.debug("TlvDecoder read length: {}", len);

            if (in.readableBytes() < len) {
                in.resetReaderIndex();
                return;
            } else {
                byte[] data;
                try {
                    data = new byte[Math.toIntExact(len)];
                } catch (ArithmeticException e) {
                    log.error("There is too much data to read. {}", len);
                    return;
                }
                in.readBytes(data);
                log.debug("TlvDecoder read data success: {}", new String(data, Charsets.UTF_8));

                out.add(new TlvMessage(type, len, data));
//                InetSocketAddress addr = (InetSocketAddress) ctx.channel().remoteAddress();
//                RawData d = new RawData(data, addr.getAddress(), addr.getPort());
//                out.add(d);
            }
        }
    }

    private boolean notEnoughData(ByteBuf in) {
        return in.readableBytes() < 6;
    }

    private void closeConnection(ChannelHandlerContext ctx) {
        ctx.pipeline().remove(this);
        ctx.channel().close();
    }
}
