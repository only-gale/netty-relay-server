package com.baidu.iot.devicecloud.devicemanager.bean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import lombok.Data;

import java.io.IOException;

/**
 * TLV: Type(2 bytes) + Length(4 bytes) + Value(Length bytes)
 *
 * Created by Yao Gang (yaogang@baidu.com) on 2019/3/4.
 *
 * @author <a href="mailto:yaogang AT baidu DOT com">Yao Gang</a>
 */
@Data
public class TlvMessage {
    // 16-bit unsigned short
    private int type;

    // 32-bit unsigned int
    private long length;

    private JsonNode value;

    public TlvMessage(int type, long length) {
        this(type, length, NullNode.getInstance());
    }

    public TlvMessage(int type, byte[] content) {
        this(type, content.length, content);
    }
    public TlvMessage(int type, long length, byte[] content) {
        this(type, length, fromBytes(length, content));
    }

    public TlvMessage(int type, long length, JsonNode value) {
        this.type = type;
        this.length = length;
        this.value = value;
    }

    static JsonNode fromBytes(long length, byte[] content) {
        if (null == content) {
            throw new NullPointerException("No content.");
        }

        if (content.length != length) {
            throw new IllegalArgumentException("Content's length illegal.");
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readTree(content);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return NullNode.getInstance();
    }

    @Override
    public String toString() {
        return String.format("\ntype: %d\nlength: %d\nvalue: %s", type, length, String.valueOf(value));
    }
}
