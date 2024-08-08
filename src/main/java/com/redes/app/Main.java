package com.redes.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.UnsupportedAudioFileException;

public class Main {

    public static WavHeaderInfo getWavHeaderInfo(File file) throws IOException
    {
        try (FileInputStream fis = new FileInputStream(file))
        {
            byte[] header = new byte[80]; // Read enough bytes to cover additional chunks
            int bytesRead = fis.read(header);

            if (bytesRead < 44) {
                throw new IOException("File is too small to be a valid WAV file");
            }

            // Check for RIFF header
            if (header[0] != 'R' || header[1] != 'I' || header[2] != 'F' || header[3] != 'F') {
                throw new IOException("File is not a valid RIFF file");
            }

            // Check for WAV format
            if (header[8] != 'W' || header[9] != 'A' || header[10] != 'V' || header[11] != 'E') {
                throw new IOException("File is not a valid WAV file");
            }

            // Extract the size of the format chunk
            int formatChunkSize = (header[16] & 0xFF) |
                                  ((header[17] & 0xFF) << 8) |
                                  ((header[18] & 0xFF) << 16) |
                                  ((header[19] & 0xFF) << 24);

            int headerSize = 20 + formatChunkSize; // Standard header size before data chunk

            Map<String, Integer> additionalChunks = new HashMap<>();
            int position = 20 + formatChunkSize;

            while (position + 8 <= bytesRead) {
                String chunkId = new String(header, position, 4);
                int chunkSize = (header[position + 4] & 0xFF) |
                                 ((header[position + 5] & 0xFF) << 8) |
                                 ((header[position + 6] & 0xFF) << 16) |
                                 ((header[position + 7] & 0xFF) << 24);

                if (chunkId.equals("data")) {
                    // Found data chunk
                    headerSize = position + 8 + chunkSize;
                    break;
                } else {
                    // Store size of additional chunks
                    additionalChunks.put(chunkId, chunkSize);
                    position += 8 + chunkSize; // Move to the next chunk
                }
            }

            // Handle case where data chunk is not found in the read bytes
            if (position + 8 > bytesRead) {
                throw new IOException("Data chunk not found in the header");
            }

            return new WavHeaderInfo(headerSize, additionalChunks);
        }
    }

    static class WavHeaderInfo {
        int headerSize;
        Map<String, Integer> additionalChunks;
        int dataChunkSize;

        WavHeaderInfo(int headerSize, Map<String, Integer> additionalChunks) {
            this.headerSize = headerSize;
            this.additionalChunks = additionalChunks;
            this.dataChunkSize = additionalChunks.getOrDefault("data", 0);
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException, UnsupportedAudioFileException
    {
        File wavFile = new File("music/martingarrix_smile.wav");
        WavHeaderInfo headerInfo = getWavHeaderInfo(wavFile);
        System.out.println("Header size: " + headerInfo.headerSize + " bytes");
        System.out.println("Data chunk size: " + headerInfo.dataChunkSize + " bytes");
        System.out.println("Additional chunks: " + headerInfo.additionalChunks);
        AudioWebSocketServer server = new AudioWebSocketServer(3307);
        server.start();
    }
}
