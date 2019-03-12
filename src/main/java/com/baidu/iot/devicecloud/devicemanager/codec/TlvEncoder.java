package com.baidu.iot.devicecloud.devicemanager.codec;

import com.baidu.iot.devicecloud.devicemanager.bean.TlvMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by Yao Gang (yaogang@baidu.com) on 2019/3/7.
 *
 * @author <a href="mailto:yaogang AT baidu DOT com">Yao Gang</a>
 */
@Slf4j
public class TlvEncoder extends MessageToByteEncoder<TlvMessage> {
    private final String name;
    private final ByteOrder byteOrder;

    public TlvEncoder(String name) {
        this(name, ByteOrder.LITTLE_ENDIAN);
    }

    public TlvEncoder(String name, ByteOrder byteOrder) {
        super();
        this.name = name;
        this.byteOrder = byteOrder;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, TlvMessage msg, ByteBuf out) throws Exception {
        log.debug("-----------------This is {}'s encoder, byte order is {}", name, byteOrder);
        int type = msg.getType();
        byte[] typeBytes;
        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            typeBytes = new byte[]{(byte)type, (byte)(type >> 8)};
        } else {
            typeBytes = new byte[]{(byte)(type >> 8), (byte)type};
        }
        int intLen = Math.toIntExact(msg.getLength());
        out.writeBytes(ByteBuffer.allocate(2).order(byteOrder).put(typeBytes).compact());
        out.writeBytes(ByteBuffer.allocate(4).order(byteOrder).putInt(intLen).compact());

        ObjectMapper om = new ObjectMapper();
        final ObjectWriter writer = om.writer();
        final byte[] bytes = writer.writeValueAsBytes(msg.getValue());
        out.writeBytes(ByteBuffer.allocate(intLen).order(byteOrder).put(bytes).compact());
    }
}
