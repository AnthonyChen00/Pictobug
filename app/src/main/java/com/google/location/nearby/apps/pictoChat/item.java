package com.google.location.nearby.apps.pictoChat;

public class item {
    String Message;
    byte[] Drawing;
    private boolean mode;

    item(String string) {
        this.Message = string;
        mode = false;
    }

    item(byte[] Array){
        this.Drawing = Array;
        mode = true;
    }

    public boolean getMode(){
        return mode;
    }

    public byte[] getDrawing() {
        return Drawing;
    }

    public String getMessage() {
        return Message;
    }
}
