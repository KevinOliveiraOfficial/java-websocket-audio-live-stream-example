package com.redes.app;

import java.util.ArrayList;
import java.util.List;

public class Channel {
    public String name;
    public List<Track> tracks = new ArrayList<Track>();
    public Track playing;

    public Channel( String name ) {
        this.name = name;
    }
}
