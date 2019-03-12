package com.baidu.iot.devicecloud.devicemanager.util;

import com.baidu.iot.devicecloud.devicemanager.bean.TlvMessage;
import com.baidu.iot.devicecloud.devicemanager.constant.TlvConstant;
import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Yao Gang (yaogang@baidu.com) on 2019/3/6.
 *
 * @author <a href="mailto:yaogang AT baidu DOT com">Yao Gang</a>
 */
@Slf4j
public class TlvUtil {
    private static List<Integer> legalTypes;

    static {
        legalTypes = Arrays.asList(
                TlvConstant.TYPE_UPSTREAM_INIT,
                TlvConstant.TYPE_UPSTREAM_ASR,
                TlvConstant.TYPE_UPSTREAM_DUMI,
                TlvConstant.TYPE_UPSTREAM_FINISH,
                TlvConstant.TYPE_DOWNSTREAM_INIT,
                TlvConstant.TYPE_DOWNSTREAM_ASR,
                TlvConstant.TYPE_DOWNSTREAM_DUMI,
                TlvConstant.TYPE_DOWNSTREAM_TTS,
                TlvConstant.TYPE_DOWNSTREAM_FINISH,
                TlvConstant.TYPE_DOWNSTREAM_PRE_TTS
        );
    }

    public static boolean isLegalType(int type) {
        return legalTypes.contains(type);
    }

    public static boolean isLegalLength(long length) {
        return length > 0;
    }

    public static List<TlvMessage> extractMessagesFrom(ByteBuf in) {
        List<TlvMessage> results = new LinkedList<>();
        while (true) {
            if (notEnoughData(in)) {
                break;
            }

            in.markReaderIndex();

            int type = in.readUnsignedShortLE();

            if (!isLegalType(type)) {
                in.resetReaderIndex();
                break;
            }

            long len = in.readUnsignedIntLE();

            if (!isLegalLength(len)) {
                in.resetReaderIndex();
                break;
            }

            if (in.readableBytes() < len) {
                in.resetReaderIndex();
                break;
            } else {
                byte[] data;
                try {
                    data = new byte[Math.toIntExact(len)];
                } catch (ArithmeticException e) {
                    break;
                }
                in.readBytes(data);

                results.add(new TlvMessage(type, len, data));
            }
        }
        return results;
    }

    private static boolean notEnoughData(ByteBuf in) {
        return in.readableBytes() < 6;
    }

    public static TlvMessage demo(int type, String s) {
        if (StringUtils.isEmpty(s)) {
            s = "";
        }
        return new TlvMessage(type, s.length(), s.getBytes());
    }
}
