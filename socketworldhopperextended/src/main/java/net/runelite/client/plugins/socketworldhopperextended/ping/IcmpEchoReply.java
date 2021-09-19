package net.runelite.client.plugins.socket.plugins.worldhopperextended.ping;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;
import java.util.Arrays;
import java.util.List;

public class IcmpEchoReply extends Structure {
    private static final int IP_OPTION_INFO_SIZE = 4 + ((Native.POINTER_SIZE == 8) ? 12 : 4);

    public static final int SIZE = 16 + Native.POINTER_SIZE + IP_OPTION_INFO_SIZE;

    public WinDef.ULONG address;

    public WinDef.ULONG status;

    public WinDef.ULONG roundTripTime;

    public WinDef.USHORT dataSize;

    public WinDef.USHORT reserved;

    public WinDef.PVOID data;

    public WinDef.UCHAR ttl;

    public WinDef.UCHAR tos;

    public WinDef.UCHAR flags;

    public WinDef.UCHAR optionsSize;

    public WinDef.PVOID optionsData;

    IcmpEchoReply(Pointer p) {
        super(p);
    }

    protected List<String> getFieldOrder() {
        return Arrays.asList(new String[] {
                "address", "status", "roundTripTime", "dataSize", "reserved", "data", "ttl", "tos", "flags", "optionsSize",
                "optionsData" });
    }
}
