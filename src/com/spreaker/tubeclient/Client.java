package com.spreaker.tubeclient;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import static org.jboss.netty.buffer.ChannelBuffers.*;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.base64.Base64;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

/**
 * @author Marco Pracucci <marco.pracucci@spreaker.com>
 */
public class Client extends SimpleChannelHandler
{
    private String  host;
    private int     port;
    private File    file;
    private int     userId;
    private String  userPassword;
    
    
    /**
     * @param host          Tube hostname
     * @param port          Tube port
     * @param file          File to stream
     * @param userId        Auth user id
     * @param userPassword  Auth user password
     */
    public Client(String host, int port, File file, int userId, String userPassword)
    {
        this.host          = host;
        this.port          = port;
        this.file          = file;
        this.userId        = userId;
        this.userPassword  = userPassword;
    }
    
    public void run()
    {
        // Create channel factory and pipeline
        ChannelFactory factory = new NioClientSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool());

        ClientBootstrap bootstrap = new ClientBootstrap(factory);
        bootstrap.setPipeline(Channels.pipeline(
            new HttpRequestEncoder(),
            new HttpResponseDecoder(),
            this
        ));
        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setOption("keepAlive", true);
        
        // Connect
        this.log("Connect", "Connecting to " + this.host + ":" + this.port);
        ChannelFuture future = bootstrap.connect(new InetSocketAddress(this.host, this.port));
        
        // Wait connected (does not introduce latencies)
        Channel channel = future.awaitUninterruptibly().getChannel();
        
        // Connection error
        if (!future.isSuccess())
        {
            this.log("Connect", "Unable to connect to " + this.host + ":" + this.port);
            
            future.getCause().printStackTrace();
            bootstrap.releaseExternalResources();
            return;
        }
        
        // Prepare HTTP request
        HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_0, new HttpMethod("SOURCE"), "/user/" + this.userId);
        request.setHeader(HttpHeaders.Names.AUTHORIZATION, "Basic " + this.getAuthenticationDigest());
        request.setHeader("Content-Type", ClientUtil.getFileMime(this.file));
        request.setHeader("ice-name", "Test episode (uploaded with tube client)");
        
        this.log("Send", "HTTP request SOURCE /user/" + this.userId);
        
        // Send HTTP request
        channel.write(request).awaitUninterruptibly();
    }
    
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception
    {
        if (!(e.getMessage() instanceof HttpResponse)) {
            super.messageReceived(ctx, e);
            return;
        }

        HttpResponse response = (HttpResponse) e.getMessage();
        this.log("Receive", "HTTP response " + response.getStatus());
        
        // Exit if the response is a non 200
        if (!response.getStatus().equals(HttpResponseStatus.OK))
        {
            e.getChannel().close();
            return;
        }
        
        this.log("Send", "Start streaming file: " + this.file.getAbsolutePath());
        
        FileInputStream stream = new FileInputStream(this.file);
        int bytesPerSecond = ClientUtil.getBytesPerSecond(this.file);
        byte[] data = new byte[Math.round(bytesPerSecond / 20)];
        
        while(true)
        {
            int bytes = stream.read(data);
            if (bytes == -1) {
                break;
            }
            
            ChannelBuffer buffer = ChannelBuffers.dynamicBuffer(data.length);
            buffer.writeBytes(data, 0, bytes);
            
            e.getChannel().write(buffer);
            
            // Sleep to be "simulate" a real streaming
            Thread.sleep(50);
        }

        this.log("Send", "Streaming done, closing connection");
        
        // Close the connection
        e.getChannel().close();
    }
    
    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
    {
        this.log("Close", "Connection closed, done");
        System.exit(0);
    }
    
    private String getAuthenticationDigest()
    {
        ChannelBuffer decodedBuffer = copiedBuffer((this.userId + ":" + this.userPassword).getBytes());
        return new String(Base64.encode(decodedBuffer).array());
    }
    
    private void log(String section, String msg)
    {
        System.out.println(section + ": \t" + msg);
    }
    
}
