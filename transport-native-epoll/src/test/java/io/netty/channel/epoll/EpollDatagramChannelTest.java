package io.netty.channel.epoll;

import java.net.InetSocketAddress;
import java.util.Random;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EpollDatagramChannelTest {
    private final Logger logger = LoggerFactory.getLogger(EpollDatagramChannelTest.class.getName());
    
    @Test
    public void epollDatagramChannelSimpleTest() throws Exception {
        Native.sizeofEpollEvent();
        //Server 
        EventLoopGroup group = new NioEventLoopGroup();
        EventLoopGroup groupC = new NioEventLoopGroup();

        int port = 5768;
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                .channel(NioDatagramChannel.class)
                .handler(new QuoteServerHandler());
            b.bind(port).sync().channel().closeFuture();
            
        
        //Client
            Bootstrap bC = new Bootstrap();
            bC.group(groupC)
                .channel(NioDatagramChannel.class)
                .handler(new QuoteClientHandler());
            bC.option(ChannelOption.RCVBUF_ALLOCATOR,new FixedRecvByteBufAllocator(4096));
            Channel ch = bC.bind(0).sync().channel();

            // Broadcast the QOTM request to port 8080.
            ch.writeAndFlush(new DatagramPacket(
                Unpooled.copiedBuffer("Ping", CharsetUtil.UTF_8),
                new InetSocketAddress("localhost", port))).sync();


            // QuoteOfTheMomentClientHandler will close the DatagramChannel when a
            // response is received.  If the channel is not closed within 5 seconds,
            // print an error message and quit.
            System.in.read();
            if (!ch.closeFuture().await(5000)) {
                System.err.println("Quote request timed out.");
            }
        } finally {
            group.shutdownGracefully();

            groupC.shutdownGracefully();
        }

    }

    private class QuoteServerHandler  extends SimpleChannelInboundHandler<DatagramPacket> {
        
        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
            int numbytes = 4096;
            byte[] bytes = new byte[numbytes];
            for (int i = 0; i < numbytes; i++) {
                bytes[i] = 'A';
            }
            ctx.write(new DatagramPacket(Unpooled.copiedBuffer(bytes), packet.sender()));
        }
    }

    public class QuoteClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
            String response = msg.content().toString(CharsetUtil.UTF_8);
            System.out.println(response.length());
        }
    }

}
