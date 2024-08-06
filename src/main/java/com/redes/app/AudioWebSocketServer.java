package com.redes.app;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.json.JSONObject;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.File;

public class AudioWebSocketServer extends WebSocketServer
{
    private List<Channel> channels = new ArrayList<Channel>();

    public AudioWebSocketServer(int port) throws UnsupportedAudioFileException, IOException
    {
        super(new InetSocketAddress(port));
        
        Channel pop = new Channel("POP");
        pop.tracks.add(new Track("Vintage Culture - Deep Inside", new File("src/main/resources/vintage_culture_deep_inside.wav"), 262));
        pop.tracks.add(new Track("Martin Garrix - Smile", new File("src/main/resources/martingarrix_smile.wav"), 78));
       
        
        
        

        Channel sertanejo = new Channel("SERTANEJO");
        sertanejo.tracks.add(new Track("Radio FM Vinheta", new File("src/main/resources/radiofm.wav"), 77));
        //sertanejo.tracks.add(new Track("Martin Garrix - Smile", new File("src/main/resources/martingarrix_smile.wav"), 78));

        channels.add(pop);
        channels.add(sertanejo);
    }

    private void broadcastEvent( String event, String data )
    {
        JSONObject message = new JSONObject();
        message.put("event", event);
        message.put("data", data);
        broadcast(message.toString());
    }

    private void broadcastEvent( String event, String channel, byte[] data )
    {
        JSONObject message = new JSONObject();
        message.put("event", event);
        message.put("channel", channel);
        message.put("data", data);
        broadcast(message.toString());
    }

    private void sendChannelHeader( WebSocket conn, Channel channel )
    {
        JSONObject message = new JSONObject();
        message.put("event", "CHANNEL_HEADER");
        message.put("channel", channel.name);
        message.put("playing", channel.playing.name);
        message.put("data", channel.playing.header);

        if ( conn != null)
            conn.send(message.toString());
        else
            broadcast(message.toString());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Closed connection: " + conn.getRemoteSocketAddress());
        broadcastEvent("GUEST_DISCONNECT", conn.getRemoteSocketAddress().toString());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // No need to handle messages from clients in this case
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart()
    {
        System.out.println("Server started successfully");
        for ( Channel channel : this.channels )
        {
            new Thread(() ->
            {
                try {
                    this.streamAudio( channel );
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection: " + conn.getRemoteSocketAddress());

        broadcastEvent("GUEST_CONNECT", conn.getRemoteSocketAddress().toString());

        // Send channels header
        for ( Channel channel : this.channels )
        {
            sendChannelHeader(conn, channel);
        }
    }

    private void streamAudio( Channel channel ) throws InterruptedException
    {
        for ( Track track : channel.tracks )
        {
            try (FileInputStream fis = new FileInputStream(track.file))
            {
                System.out.println("broadcasting " + track.name);
                
                // Read header
                byte[] header = new byte[track.headerSize]; // WAV header size
                fis.read(header); // Read the WAV header
                track.header = header;

                // Set current playing track
                channel.playing = track;

                // Broadcast header
                sendChannelHeader(null, channel);

                // Broadcast track
                byte[] buffer = new byte[track.calcBps()];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1)
                {
                    broadcastEvent("STREAM", channel.name, buffer);
                    

                    Thread.sleep(1000);
                }
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        streamAudio(channel);
    }
}
