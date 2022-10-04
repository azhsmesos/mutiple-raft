package com.github.raft.transport.netty.protocol.codec;

import com.github.raft.transport.netty.protocol.message.Message;
import com.github.raft.transport.netty.protocol.serialize.SerializerEnum;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 消息数据转字节数组编码器
 *
 * @author wujiuye
 */
public class MessageEncoder extends MessageToByteEncoder<Message> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Message message, ByteBuf out) {
        MessageCodecManager.encode(out, message, SerializerEnum.JSON);
    }

}
