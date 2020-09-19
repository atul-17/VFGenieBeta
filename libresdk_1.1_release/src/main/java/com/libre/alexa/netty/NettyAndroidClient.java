package com.libre.alexa.netty;

/**
 * Created by praveena on 7/10/15.
 */

import android.util.Base64;
import android.util.Log;

import com.libre.alexa.LibreApplication;
import com.libre.alexa.util.LibreLogger;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;


public class NettyAndroidClient {

    private static final String TAG = NettyAndroidClient.class.getName();
    NettyClientHandler handler;
    String remotehost;
    int port;
    long lastNotifiedTime;

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {

        this.creationTime = creationTime;
    }

    long creationTime;

    private static NettyAndroidClient dummyCLient = new NettyAndroidClient();
    /*need not create every time for all clients*/
    private static EventLoopGroup workerGroup = new NioEventLoopGroup();


    private NettyAndroidClient() {

    }

    public static NettyAndroidClient getDummyInstance() {
        dummyCLient.setRemotehost("0.0.0.0");
        dummyCLient.creationTime = System.currentTimeMillis();
        return dummyCLient;
    }

    public NettyAndroidClient(InetAddress host, int port) throws Exception {


        handler = new NettyClientHandler(host.getHostAddress());
        remotehost = host.getHostAddress();
        port = port;


        Bootstrap b = new Bootstrap(); // (1)

        b.group(workerGroup); // (2)
        b.channel(NioSocketChannel.class); // (3)

        b.option(ChannelOption.TCP_NODELAY, true);
        b.option(ChannelOption.SO_KEEPALIVE, true); // (4)
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
//                ch.pipeline().addLast("IdleChecker", new IdleStateHandler(10, 10, 10));
//                ch.pipeline().addLast("IdleDisconnecter", new HeartbeatHandler(remotehost));
//                ch.pipeline().addLast(new NettyDecoder(), handler);
                SSLContext sslContext = null;
                SSLEngine sslEngine=null;
                sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, trustAllCerts.get(), null);
                sslEngine = sslContext.createSSLEngine();
                sslEngine.setUseClientMode(true);
                sslEngine.setNeedClientAuth(false);
                SslHandler sslHandler = new SslHandler(sslEngine);
                sslHandler.engine().setEnabledProtocols(new String[]{"TLSv1.2"});
                LibreLogger.d(this, TAG + " SUMA IN NETTY Exception Exception before  " );
//                if(LibreApplication.sslCertificateNotFound){
//                 LibreLogger.d(this,"suma in ssl exception found");
//                    LibreLogger.d(this, TAG + " SUMA IN NETTY Exception Exception before  IF" );
//
//                }
//                else{
                ch.pipeline().addFirst(sslHandler);
                LibreLogger.d(this,"suma in ssl exception  not found before else");
                LibreLogger.d(this, TAG + " SUMA IN NETTY Exception Exception before ELSE" );

                //  }
                ch.pipeline().addLast("IdleChecker", new IdleStateHandler(10, 10, 10));
                ch.pipeline().addLast("IdleDisconnecter", new HeartbeatHandler(remotehost));
                ch.pipeline().addLast(new NettyDecoder(), handler);
            }
        });

        // Start the client.
        ChannelFuture f = b.connect(host, port).sync();
        setCreationTime(System.currentTimeMillis());
        /*f.addListener(new ChannelFutureListener() {
                          @Override
                          public void operationComplete(ChannelFuture channelFuture) throws Exception {

                              if (channelFuture.isSuccess()) {
                                  LibreLogger.d(this, "Channel success for " + channelFuture.channel().remoteAddress());

                              } else {
                                  LibreLogger.d(this, "CHANNEL FAILED for " + channelFuture.channel().remoteAddress());

                              }
                          }
                      }
        );*/

        /*    ChannelFuture f = b.connect(host, port).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if(channelFuture.isSuccess()){
                        Log.d(TAG,"Creation of Socket Successful"+ channelFuture.channel().id()+":"+
                                remotehost);
                        socketCreated = true;
                    }else{
                        Log.d(TAG,"Creation of Socket UnSuccessful");
                        socketCreated = false;
                    }
                }
            }).await();*/
        Log.e("Scan_Netty", "Created a Netty socket to listen ");


    }

    public NettyClientHandler getHandler(){
        return handler;
    }
    public String getRemotehost() {
        return remotehost;
    }

    public void setRemotehost(String remotehost) {
        this.remotehost = remotehost;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void write(byte[] bytes) {
        if (handler != null) {
            handler.write(bytes);

        }
    }

    private final ThreadLocal<TrustManager[]> trustAllCerts = new ThreadLocal<TrustManager[]>() {
        @Override
        protected TrustManager[] initialValue() {
            return new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }

                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                    LibreLogger.d(this, "suma in netty client trusted certi");
                    convertToX509Cert(LibreApplication.getMYPEMstring);

                }

                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                    LibreLogger.d(this, "suma in netty server trusted certi");
                    convertToX509Cert(LibreApplication.getMYPEMstring);

                }
            }
            };
        }
    };

    public static void convertToX509Cert(String certificateString) throws CertificateException {
        X509Certificate certificate = null;
        CertificateFactory cf = null;
        try {
            if (certificateString != null); // NEED FOR PEM FORMAT CERT STRING
            byte[] certificateData= Base64.decode(certificateString, Base64.DEFAULT);

            cf = CertificateFactory.getInstance("X509");
            certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certificateData));
            Log.d("suma certi","suma in generate "+certificate);
        } catch (CertificateException e) {
            throw new CertificateException(e);
        }
    }
    public long getLastNotifiedTime() {
        return lastNotifiedTime;
    }

    public void setLastNotifiedTime(long notifiedTime) {
        lastNotifiedTime = notifiedTime;
    }


    /**
     * LatestDiscoveryChanges
     * this method will close existing NettyAndroidClient
     */
    public void closeSocket() {
        if (handler != null && handler.mChannelContext != null)
            handler.mChannelContext.close();
    }


}
