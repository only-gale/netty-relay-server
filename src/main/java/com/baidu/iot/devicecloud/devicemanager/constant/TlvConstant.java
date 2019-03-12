package com.baidu.iot.devicecloud.devicemanager.constant;

/**
 * Created by Yao Gang (yaogang@baidu.com) on 2019/3/4.
 *
 * @author <a href="mailto:yaogang AT baidu DOT com">Yao Gang</a>
 */
public class TlvConstant {
    public static final int TYPE_UPSTREAM_INIT = 0X0001;
    public static final int TYPE_UPSTREAM_ASR = 0X0002;
    public static final int TYPE_UPSTREAM_DUMI = 0X0003;
    public static final int TYPE_UPSTREAM_FINISH = 0X0004;

    public static final int TYPE_DOWNSTREAM_INIT = 0XF001;
    public static final int TYPE_DOWNSTREAM_ASR = 0XF002;
    public static final int TYPE_DOWNSTREAM_DUMI = 0XF003;
    public static final int TYPE_DOWNSTREAM_TTS = 0XF004;
    public static final int TYPE_DOWNSTREAM_FINISH = 0XF005;
    public static final int TYPE_DOWNSTREAM_PRE_TTS = 0XF006;
}
