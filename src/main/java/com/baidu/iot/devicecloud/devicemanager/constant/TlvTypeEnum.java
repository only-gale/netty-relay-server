package com.baidu.iot.devicecloud.devicemanager.constant;

/**
 * Created by Yao Gang (yaogang@baidu.com) on 2019/3/6.
 *
 * @author <a href="mailto:yaogang AT baidu DOT com">Yao Gang</a>
 */
public enum TlvTypeEnum {
    TYPE_UPSTREAM_INIT(0X0001),
    TYPE_UPSTREAM_ASR(0X0002),
    TYPE_UPSTREAM_DUMI(0X0003),
    TYPE_UPSTREAM_FINISH(0X0004),

    TYPE_DOWNSTREAM_INIT(0XF001),
    TYPE_DOWNSTREAM_ASR(0XF002),
    TYPE_DOWNSTREAM_DUMI(0XF003),
    TYPE_DOWNSTREAM_TTS(0XF004),
    TYPE_DOWNSTREAM_FINISH(0XF005),
    TYPE_DOWNSTREAM_PRE_TTS(0XF006);

    private int type;

    TlvTypeEnum(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
