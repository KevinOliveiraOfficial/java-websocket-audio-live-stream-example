package com.redes.app;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.File;

public class AudioWebSocketServer extends WebSocketServer
{
    private List<Track> tracks = new ArrayList<Track>();
    private Track currentTrack;

    public AudioWebSocketServer(int port) throws UnsupportedAudioFileException, IOException
    {
        super(new InetSocketAddress(port));
        //this.capture = new AudioCapture();
        this.tracks.add(new Track(new File("src/main/resources/martingarrix_smile.wav"), 78));
        this.tracks.add(new Track(new File("src/main/resources/radiofm.wav"), 77));
        
        
        
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Closed connection: " + conn.getRemoteSocketAddress());
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
    public void onStart() {
        System.out.println("Server started successfully");
        new Thread(() -> {
            try {
                streamAudio();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection: " + conn.getRemoteSocketAddress());
        conn.send(this.currentTrack.header);
    }

    private void streamAudio() throws InterruptedException
    {
        for ( Track track : this.tracks )
        {
            try (FileInputStream fis = new FileInputStream(track.file))
            {
                System.out.println("reading file");
                this.currentTrack = track;

                // Read header
                byte[] header = new byte[track.headerSize]; // WAV header size
                fis.read(header); // Read the WAV header
                this.currentTrack.header = header;

                // Broadcast header
                broadcast(header);

                // Broadcast track
                byte[] buffer = new byte[track.calcBps()];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1)
                {
                    System.out.println("broadcasting");
                    //conn.send(buffer);
                    broadcast(buffer);
                    Thread.sleep(1000);
                }
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
