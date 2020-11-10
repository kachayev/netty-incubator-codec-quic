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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.PlatformDependent;

import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.Random;

public final class QuicClientExample {

    private QuicClientExample() { }

    public static void main(String[] args) throws Exception {
        byte[] proto = new byte[] {
                0x05, 'h', 'q', '-', '2', '9',
                0x05, 'h', 'q', '-', '2', '8',
                0x05, 'h', 'q', '-', '2', '7',
                0x08, 'h', 't', 't', 'p', '/', '0', '.', '9'
        };

        NioEventLoopGroup group = new NioEventLoopGroup(1);
        final QuicCodec codec = new QuicCodecBuilder()
                .certificateChain("./src/test/resources/cert.crt")
                .privateKey("./src/test/resources/cert.key")
                .applicationProtocols(proto)
                .maxIdleTimeout(5000)
                .maxUdpPayloadSize(Quic.MAX_DATAGRAM_SIZE)
                .initialMaxData(10000000)
                .initialMaxStreamDataBidirectionalLocal(1000000)
                .initialMaxStreamDataBidirectionalRemote(1000000)
                .initialMaxStreamsBidirectional(100)
                .initialMaxStreamsUnidirectional(100)
                .disableActiveMigration(true)
                .enableEarlyData()
                .build(InsecureQuicTokenHandler.INSTANCE, new QuicChannelInitializer(
                        new ChannelInboundHandlerAdapter() {

                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                QuicStreamChannel channel = (QuicStreamChannel) ctx.channel();
                                System.out.println(channel.isLocalCreated() + " => " +
                                        channel.isBidirectional() + " == " + channel.streamId());
                                // xxx: for some reason, this write does not work
                                // ByteBuf buffer = ctx.alloc().directBuffer();
                                // buffer.writeCharSequence("GET /", CharsetUtil.US_ASCII);
                                // ctx.write(buffer);
                            }

                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                ByteBuf byteBuf = (ByteBuf) msg;
                                System.out.println(byteBuf.toString(CharsetUtil.US_ASCII));
                                byteBuf.release();
                            }

                            @Override
                            public void channelReadComplete(ChannelHandlerContext ctx) {
                                ctx.flush();
                            }

                            @Override
                            public boolean isSharable() {
                                return true;
                            }
                }));
        try {
            final Bootstrap bs = new Bootstrap();
            final Channel channel = bs.group(group)
                    .channel(NioDatagramChannel.class)
                    .handler(codec)
                    .localAddress(new InetSocketAddress(0))
                    // .remoteAddress(new InetSocketAddress("45.77.96.66", 8443))
                    .remoteAddress(new InetSocketAddress("127.0.0.1", 9999))
                    .connect()
                    // .bind()
                    .sync()
                    .channel();

            // xxx: do we need to wait for handshake to be finished? or just to
            // fire write operations and rely on all write operations being delayed
            // before handshake is done?
            // it would be cool if "channel" that I get here is a `QuicChannel`
            // rather than just a `DatagramChannel`

            // xxx: okay... so in the current implemention it works this way:
            // connection promise is resolve only when handshake is already done,
            // is it fair? is it useful?

            // xxx: do we want to have a single codec for client & server or different
            // codecs? some code for the client has nothing to do with server-side
            // implementation, e.g. connection, handshake, pending writes etc
            // -- from what I've got so far, seems like having client connection handler
            // separately from server accept is must have. even if we still want to
            // wrap all of them into a "codec" that decides which handlers to setup

            // create a new stream
            // xxx: seems like not a very smart API :( or very not smart
            // not only because of werid machinery with the codec but also
            // because I can't get stream channel here. doing this in a handler
            // works... to some extent. but if i'm already using handler,
            // why can't I catch e.g. connect there?
            codec.clientChannel().connect(QuicStreamAddress.unidirectional()).sync();

            // xxx: nice option for the client would be something like
            // b.connect().sync.channel().newStream().write()
            // ... or ...
            // b.connect().sync.channel().connect(StreamAddress.bidirectional()).write()
            // both options requires ability to return a new channel from
            // the bootstrap.connect (or general notion of "child" channel???)

            // xxx: it seems like binding between StreamChannel and Codec (with
            // setting up a handler from the codec) is not always necessary for
            // the client, there might be the case where I want all packages to
            // be processed in a single handler (without multiplexing them into
            // stream handlers). another question in this API... how do I create
            // a new stream from the context of current (if i'm inside handler)
            // would it be something like
            // 
            // ((QuicStreamChannel) ctx.channel()).parent().connect(StreamAddress.bidirectional())
            //
            // ... looks good, though what if i need a different set of handlers for 
            // a new stream? seems like a logical thing to do if i'm trying
            // to put all of my logic into a single connection


            // todo 2: process the response (wait, I assume)

            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}
