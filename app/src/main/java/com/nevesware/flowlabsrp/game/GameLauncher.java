package com.nevesware.flowlabsrp.game;

import android.content.Context;

import com.nevesware.flowlabsrp.auth.Session;
import com.nevesware.flowlabsrp.net.ServerInfo;

/**
 * Ponto de entrada do JOGO. Hoje só faz as verificações antes de jogar.
 *
 * Próxima etapa (cliente multiplayer):
 *  1. Carregar o motor do GTA SA do próprio jogador (libGTASA.so do APK oficial instalado)
 *     junto com os dados importados em GameFiles.installDir().
 *  2. Aplicar os hooks do cliente FlowLabs (sincronização de jogadores, veículos, chat, HUD).
 *  3. Conectar no servidor escolhido com o token da sessão.
 */
public final class GameLauncher {

    private GameLauncher() {}

    public static final int OK = 0;
    public static final int NEED_LOGIN = 1;
    public static final int NEED_NICKNAME = 2;
    public static final int NEED_FILES = 3;
    public static final int SERVER_SOON = 4;
    public static final int SERVER_OFFLINE = 5;
    public static final int CLIENT_NOT_READY = 6;

    /** Retorna o primeiro motivo que impede de jogar, ou CLIENT_NOT_READY enquanto o cliente não existe. */
    public static int preflight(Context c, Session s, ServerInfo server) {
        if (s == null) return NEED_LOGIN;
        if (!s.hasNickname()) return NEED_NICKNAME;
        if (GameFiles.check(c).state != GameFiles.READY) return NEED_FILES;
        if (server == null || server.comingSoon) return SERVER_SOON;
        if (server.checked && !server.online) return SERVER_OFFLINE;
        return CLIENT_NOT_READY;
    }
}
