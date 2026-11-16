package com.nevesware.flowlabsrp.net;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Mede se o servidor responde e o ping (tempo de conexão TCP).
 * Quando o servidor FlowLabs tiver uma porta de consulta própria (players online etc.),
 * é aqui que essa consulta entra.
 */
public final class ServerPinger {

    private ServerPinger() {}

    public interface Callback {
        void onDone(ServerInfo server);
    }

    public static void ping(final ServerInfo s, final Callback cb) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (s.comingSoon) {
                    s.online = false;
                    s.pingMs = -1;
                } else {
                    Socket sock = new Socket();
                    long start = System.nanoTime();
                    try {
                        sock.connect(new InetSocketAddress(s.host, s.port), 2500);
                        s.pingMs = (int) ((System.nanoTime() - start) / 1000000L);
                        s.online = true;
                    } catch (Exception e) {
                        s.online = false;
                        s.pingMs = -1;
                    } finally {
                        try { sock.close(); } catch (Exception ignored) {}
                    }
                }
                s.checked = true;
                cb.onDone(s);
            }
        }, "ping-" + s.id).start();
    }
}
