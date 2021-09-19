package net.runelite.client.plugins.socket.plugins.worldhopperextended.ping;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

interface IPHlpAPI extends Library {
    public static final IPHlpAPI INSTANCE = (IPHlpAPI)Native.loadLibrary("IPHlpAPI", IPHlpAPI.class);

    Pointer IcmpCreateFile();

    boolean IcmpCloseHandle(Pointer paramPointer);

    int IcmpSendEcho(Pointer paramPointer1, int paramInt1, Pointer paramPointer2, short paramShort, Pointer paramPointer3, IcmpEchoReply paramIcmpEchoReply, int paramInt2, int paramInt3);
}
