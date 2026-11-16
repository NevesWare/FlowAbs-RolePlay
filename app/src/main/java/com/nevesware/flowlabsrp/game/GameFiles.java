package com.nevesware.flowlabsrp.game;

import android.content.Context;
import android.content.pm.PackageInfo;

import com.nevesware.flowlabsrp.Config;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Arquivos do GTA San Andreas DO PRÓPRIO JOGADOR, importados para a pasta do launcher.
 * O launcher nunca baixa nem distribui esses arquivos.
 *
 * ATENÇÃO: a lista de itens obrigatórios é PROVISÓRIA, montada a partir da estrutura
 * conhecida da versão Android (Android/data/com.rockstargames.gtasa/files). Quando os
 * arquivos reais forem importados, use "Gerar relatório da estrutura" e ajuste aqui.
 */
public final class GameFiles {

    private GameFiles() {}

    public static final int NOT_IMPORTED = 0;
    public static final int INCOMPLETE = 1;
    public static final int READY = 2;

    /** Um item que precisa existir. Qualquer um dos caminhos alternativos serve. */
    public static class Requirement {
        public final String label;
        public final String[] paths;
        public final boolean directory;

        Requirement(String label, boolean directory, String... paths) {
            this.label = label;
            this.directory = directory;
            this.paths = paths;
        }
    }

    public static class Check {
        public final Requirement req;
        public final boolean found;
        public final String foundAt;

        Check(Requirement req, boolean found, String foundAt) {
            this.req = req;
            this.found = found;
            this.foundAt = foundAt;
        }
    }

    public static class Status {
        public int state;
        public List<Check> checks = new ArrayList<Check>();
        public long totalBytes;
        public int fileCount;

        public int foundCount() {
            int n = 0;
            for (Check c : checks) if (c.found) n++;
            return n;
        }
    }

    public static final Requirement[] REQUIRED = {
        new Requirement("Configura\u00E7\u00E3o do mundo (gta.dat)", false, "data/gta.dat"),
        new Requirement("F\u00EDsica dos ve\u00EDculos (handling.cfg)", false, "data/handling.cfg"),
        new Requirement("Lista de ve\u00EDculos (vehicles.ide)", false, "data/vehicles.ide"),
        new Requirement("Lista de personagens (peds.ide)", false, "data/peds.ide"),
        new Requirement("Anima\u00E7\u00F5es (ped.ifp)", false, "anim/ped.ifp"),
        new Requirement("Modelos do mapa (gta3.img)", false,
                "models/gta3.img", "texdb/gta3.img", "data/models/gta3.img"),
        new Requirement("Interiores (gta_int.img)", false,
                "models/gta_int.img", "texdb/gta_int.img", "data/models/gta_int.img"),
        new Requirement("Texturas (texdb)", true, "texdb"),
        new Requirement("\u00C1udio", true, "audio"),
        new Requirement("Textos do jogo", true, "text"),
    };

    // ---------------- Pastas ----------------

    public static File installDir(Context c) {
        File base = c.getExternalFilesDir(null);
        if (base == null) base = c.getFilesDir();
        File d = new File(base, "gta");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    /** Versão do GTA SA instalado pela Play Store, ou null se não estiver instalado. */
    public static String installedGtaVersion(Context c) {
        try {
            PackageInfo pi = c.getPackageManager().getPackageInfo(Config.GTA_PACKAGE, 0);
            return pi.versionName == null ? "?" : pi.versionName;
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------- Verificação ----------------

    public static Status check(Context c) {
        File root = installDir(c);
        Status s = new Status();
        long[] acc = new long[2];
        walkSize(root, acc);
        s.totalBytes = acc[0];
        s.fileCount = (int) acc[1];

        for (Requirement r : REQUIRED) {
            String at = null;
            for (String p : r.paths) {
                File f = resolve(root, p);
                if (f != null && (r.directory ? f.isDirectory() : f.isFile())) {
                    at = p;
                    break;
                }
            }
            s.checks.add(new Check(r, at != null, at));
        }
        if (s.fileCount == 0) s.state = NOT_IMPORTED;
        else if (s.foundCount() == REQUIRED.length) s.state = READY;
        else s.state = INCOMPLETE;
        return s;
    }

    /** Resolve um caminho relativo ignorando maiúsculas/minúsculas. */
    public static File resolve(File root, String rel) {
        File cur = root;
        for (String seg : rel.split("/")) {
            if (seg.length() == 0) continue;
            File exact = new File(cur, seg);
            if (exact.exists()) {
                cur = exact;
                continue;
            }
            File[] kids = cur.listFiles();
            if (kids == null) return null;
            File match = null;
            for (File k : kids) {
                if (k.getName().equalsIgnoreCase(seg)) {
                    match = k;
                    break;
                }
            }
            if (match == null) return null;
            cur = match;
        }
        return cur;
    }

    public static boolean deleteAll(Context c) {
        return deleteRecursive(installDir(c));
    }

    // ---------------- Relatório da estrutura ----------------

    /**
     * Lista nomes, tamanhos e o SHA-1 de arquivos pequenos de configuração.
     * Não inclui conteúdo dos arquivos: serve para ajustar a lista de requisitos e o
     * carregamento do jogo sem precisar compartilhar o jogo.
     */
    public static String report(Context c) {
        File root = installDir(c);
        StringBuilder sb = new StringBuilder();
        sb.append("FlowLabs Launcher ").append(Config.VERSION_NAME).append(" \u2014 relat\u00F3rio da estrutura\n");
        String v = installedGtaVersion(c);
        sb.append("GTA SA (Play Store): ").append(v == null ? "n\u00E3o instalado" : "v" + v).append('\n');
        sb.append("Pasta: ").append(root.getAbsolutePath()).append("\n\n");
        int[] lines = {0};
        listTree(root, "", sb, lines);
        if (lines[0] >= 4000) sb.append("... (lista cortada em 4000 itens)\n");
        sb.append("\nSHA-1 dos arquivos de configura\u00E7\u00E3o:\n");
        String[] keys = {"data/gta.dat", "data/handling.cfg", "data/vehicles.ide", "data/peds.ide",
                "data/default.ide", "data/default.dat", "data/timecyc.dat"};
        for (String k : keys) {
            File f = resolve(root, k);
            sb.append("  ").append(k).append(": ")
              .append(f != null && f.isFile() ? sha1(f) : "ausente").append('\n');
        }
        return sb.toString();
    }

    private static void listTree(File dir, String prefix, StringBuilder sb, int[] lines) {
        File[] kids = dir.listFiles();
        if (kids == null) return;
        java.util.Arrays.sort(kids);
        for (File k : kids) {
            if (lines[0]++ >= 4000) return;
            String rel = prefix + k.getName();
            if (k.isDirectory()) {
                sb.append(rel).append("/\n");
                listTree(k, rel + "/", sb, lines);
            } else {
                sb.append(rel).append("  (").append(formatSize(k.length())).append(")\n");
            }
        }
    }

    // ---------------- Utilidades ----------------

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb);
        return String.format(Locale.US, "%.2f GB", mb / 1024.0);
    }

    private static void walkSize(File f, long[] acc) {
        File[] kids = f.listFiles();
        if (kids == null) return;
        for (File k : kids) {
            if (k.isDirectory()) walkSize(k, acc);
            else {
                acc[0] += k.length();
                acc[1]++;
            }
        }
    }

    private static boolean deleteRecursive(File f) {
        boolean ok = true;
        File[] kids = f.listFiles();
        if (kids != null) {
            for (File k : kids) {
                if (k.isDirectory()) ok &= deleteRecursive(k);
                else ok &= k.delete();
            }
        }
        return ok;
    }

    private static String sha1(File f) {
        if (f.length() > 16L * 1024 * 1024) return "(arquivo grande, ignorado)";
        FileInputStream in = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            in = new FileInputStream(f);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "erro";
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) {}
        }
    }
}
