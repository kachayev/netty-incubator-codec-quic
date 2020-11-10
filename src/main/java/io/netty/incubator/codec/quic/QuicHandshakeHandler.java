/*
 * Copyright 2020 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.incubator.codec.quic;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;

public class QuicHandshakeHandler extends ChannelInboundHandlerAdapter {

    private static final Throwable HANDSHAKE_FAILURE = new RuntimeException("Handshake failed");
    final ChannelPromise promise;
    final QuicCodec codec;
    final InetSocketAddress remoteAddress;
    final long connAddr;
    final ChannelHandler handler;
    QuicheQuicChannel channel;

    public QuicHandshakeHandler(
            ChannelPromise promise, QuicCodec codec, long connAddr, InetSocketAddress remote, ChannelHandler handler) {
        this.promise = promise;
        this.codec = codec;
        this.connAddr = connAddr;
        this.remoteAddress = remote;
        this.handler = handler;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        channel = new QuicheQuicChannel(codec, ctx.channel(), connAddr, false, remoteAddress, handler);
        ctx.channel().eventLoop().register(channel);
        // xxx: not sure if it's a good idea to reuse generic flush
        // but should work as of now
        channel.writeAndFlushEgress(true);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        DatagramPacket packet = (DatagramPacket) msg;
        try {
            channel.recv(packet.content());
            if (Quiche.quiche_conn_is_established(connAddr)) {
                // xxx: this is stupid
                codec.clientChannel(channel);
                promise.setSuccess();
                ctx.pipeline().remove(this);
            } else if (Quiche.quiche_conn_is_closed(connAddr)) {
                promise.setFailure(HANDSHAKE_FAILURE);
                ctx.pipeline().remove(this);
            }
        } finally {
            packet.release();
        }
    }
}
