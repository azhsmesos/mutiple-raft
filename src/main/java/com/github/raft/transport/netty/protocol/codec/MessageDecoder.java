package com.github.raft.transport.netty.protocol.codec;

import com.github.raft.transport.netty.protocol.message.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;

/**
 * 字节转消息数据解码器
 *
 * @author wujiuye
 */
public class MessageDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List out) {
        Message message = MessageCodecManager.decode(in);
        if (message != null) {
            out.add(message);
        }
    }

}
