package net.runelite.client.plugins.socket.plugins.worldhopperextended.ping;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import net.runelite.client.util.OSType;
import net.runelite.http.api.worlds.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ping {
    private static final Logger log = LoggerFactory.getLogger(Ping.class);

    private static final String RUNELITE_PING = "RuneLitePing";

    private static final int TIMEOUT = 2000;

    private static final int PORT = 43594;

    public static int ping(World world) {
        try {
            switch (OSType.getOSType()) {
                case Windows:
                    return windowsPing(world);
            }
            return tcpPing(world);
        } catch (IOException ex) {
            log.warn("error pinging", ex);
            return -1;
        }
    }

    private static int windowsPing(World world) throws UnknownHostException {
        IPHlpAPI ipHlpAPI = IPHlpAPI.INSTANCE;
        Pointer ptr = ipHlpAPI.IcmpCreateFile();
        InetAddress inetAddress = InetAddress.getByName(world.getAddress());
        byte[] address = inetAddress.getAddress();
        String dataStr = "RuneLitePing";
        int dataLength = dataStr.length() + 1;
        Memory memory = new Memory(dataLength);
        memory.setString(0L, dataStr);
        IcmpEchoReply icmpEchoReply = new IcmpEchoReply((Pointer)new Memory((IcmpEchoReply.SIZE + dataLength)));
        assert icmpEchoReply.size() == IcmpEchoReply.SIZE;
        int packed = address[0] & 0xFF | (address[1] & 0xFF) << 8 | (address[2] & 0xFF) << 16 | (address[3] & 0xFF) << 24;
        int ret = ipHlpAPI.IcmpSendEcho(ptr, packed, (Pointer)memory, (short)dataLength, Pointer.NULL, icmpEchoReply, IcmpEchoReply.SIZE + dataLength, 2000);
        if (ret != 1) {
            ipHlpAPI.IcmpCloseHandle(ptr);
            return -1;
        }
        int rtt = Math.toIntExact(icmpEchoReply.roundTripTime.longValue());
        ipHlpAPI.IcmpCloseHandle(ptr);
        return rtt;
    }

    private static int tcpPing(World world) throws IOException {
        Socket socket = new Socket();
        try {
            socket.setSoTimeout(2000);
            InetAddress inetAddress = InetAddress.getByName(world.getAddress());
            long start = System.nanoTime();
            socket.connect(new InetSocketAddress(inetAddress, 43594));
            long end = System.nanoTime();
            int i = (int)((end - start) / 1000000L);
            socket.close();
            return i;
        } catch (Throwable throwable) {
            try {
                socket.close();
            } catch (Throwable throwable1) {
                throwable.addSuppressed(throwable1);
            }
            throw throwable;
        }
    }
}
