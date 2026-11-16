package com.nevesware.flowlabsrp.game;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Copia os arquivos do GTA SA que o jogador escolheu (seletor de pastas do Android)
 * para a pasta do launcher. Se encontrar .obb, tenta extrair o conteúdo.
 *
 * Por que o jogador escolhe a pasta: desde o Android 11, um app não pode ler
 * Android/data nem Android/obb de outro app. Então o jogador copia a pasta do jogo
 * dele para um lugar livre (ex.: Download/GTA-SA) e escolhe essa pasta aqui.
 */
public final class GameImporter {

    public interface Listener {
        void onProgress(long done, long total, String current);
        void onFinished(boolean ok, String message);
    }

    private static final String[] KNOWN_TOP = {
            "data", "models", "texdb", "anim", "audio", "text", "files", "scache"};

    private static class Entry {
        String docId;
        String rel;
        long size;
    }

    private final Context ctx;
    private final Uri tree;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean cancelled;

    public GameImporter(Context ctx, Uri tree, Listener listener) {
        this.ctx = ctx.getApplicationContext();
        this.tree = tree;
        this.listener = listener;
    }

    public void cancel() {
        cancelled = true;
    }

    public void start() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runImport();
                } catch (Exception e) {
                    finish(false, "Erro na importação: " + e.getMessage());
                }
            }
        }, "gta-import").start();
    }

    private void runImport() throws Exception {
        ContentResolver cr = ctx.getContentResolver();
        String rootId = DocumentsContract.getTreeDocumentId(tree);

        // Se o jogador escolheu "com.rockstargames.gtasa", entra em "files".
        List<String[]> top = children(cr, rootId);
        String baseId = rootId;
        boolean recognized = false;
        for (String[] c : top) {
            String name = c[1].toLowerCase(Locale.US);
            if (name.endsWith(".obb")) recognized = true;
            for (String k : KNOWN_TOP) if (k.equals(name)) recognized = true;
        }
        for (String[] c : top) {
            if ("files".equalsIgnoreCase(c[1]) && DocumentsContract.Document.MIME_TYPE_DIR.equals(c[2])) {
                boolean hasData = false;
                for (String[] t : top) if ("data".equalsIgnoreCase(t[1])) hasData = true;
                if (!hasData) baseId = c[0];
            }
        }
        if (!recognized) {
            finish(false, "Essa pasta não parece ser do GTA San Andreas. Escolha a pasta que tem "
                    + "\"data\", \"models\"/\"texdb\", \"anim\" e \"audio\" (ou os arquivos .obb).");
            return;
        }

        // 1) Lista tudo
        List<Entry> files = new ArrayList<Entry>();
        collect(cr, baseId, "", files);
        long total = 0;
        for (Entry e : files) total += Math.max(0, e.size);

        File dest = GameFiles.installDir(ctx);
        long free = dest.getUsableSpace();
        if (total > 0 && free > 0 && total > free) {
            finish(false, "Espaço insuficiente: precisa de " + GameFiles.formatSize(total)
                    + ", livre " + GameFiles.formatSize(free) + ".");
            return;
        }

        // 2) Copia
        long done = 0;
        List<File> obbs = new ArrayList<File>();
        byte[] buf = new byte[128 * 1024];
        for (Entry e : files) {
            if (cancelled) {
                finish(false, "Importação cancelada.");
                return;
            }
            File out = safeChild(dest, e.rel);
            if (out == null) continue;
            out.getParentFile().mkdirs();
            Uri doc = DocumentsContract.buildDocumentUriUsingTree(tree, e.docId);
            InputStream in = cr.openInputStream(doc);
            OutputStream os = new FileOutputStream(out);
            try {
                int n;
                long lastReport = 0;
                while ((n = in.read(buf)) > 0) {
                    if (cancelled) break;
                    os.write(buf, 0, n);
                    done += n;
                    if (done - lastReport > 2 * 1024 * 1024) {
                        lastReport = done;
                        progress(done, total, e.rel);
                    }
                }
            } finally {
                in.close();
                os.close();
            }
            progress(done, total, e.rel);
            if (e.rel.toLowerCase(Locale.US).endsWith(".obb")) obbs.add(out);
        }

        // 3) Extrai .obb (são arquivos zip na versão Android)
        for (File obb : obbs) {
            if (cancelled) break;
            if (extractZip(obb, dest)) obb.delete();
        }

        GameFiles.Status st = GameFiles.check(ctx);
        if (st.state == GameFiles.READY) {
            finish(true, "Arquivos importados e verificados (" + GameFiles.formatSize(st.totalBytes) + ").");
        } else {
            finish(true, "Importação concluída, mas faltam " + (st.checks.size() - st.foundCount())
                    + " item(ns). Veja a lista e o relatório.");
        }
    }

    private void collect(ContentResolver cr, String parentId, String prefix, List<Entry> out) {
        for (String[] c : children(cr, parentId)) {
            if (cancelled) return;
            String rel = prefix + c[1];
            if (DocumentsContract.Document.MIME_TYPE_DIR.equals(c[2])) {
                collect(cr, c[0], rel + "/", out);
            } else {
                Entry e = new Entry();
                e.docId = c[0];
                e.rel = rel;
                try { e.size = Long.parseLong(c[3]); } catch (Exception ex) { e.size = 0; }
                out.add(e);
            }
        }
    }

    /** Retorna [docId, nome, mime, tamanho] de cada filho. */
    private List<String[]> children(ContentResolver cr, String parentId) {
        List<String[]> list = new ArrayList<String[]>();
        Uri uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId);
        Cursor cur = null;
        try {
            cur = cr.query(uri, new String[] {
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE}, null, null, null);
            while (cur != null && cur.moveToNext()) {
                list.add(new String[] {cur.getString(0), cur.getString(1), cur.getString(2),
                        cur.isNull(3) ? "0" : String.valueOf(cur.getLong(3))});
            }
        } catch (Exception ignored) {
        } finally {
            if (cur != null) cur.close();
        }
        return list;
    }

    private boolean extractZip(File zip, File dest) {
        ZipInputStream zin = null;
        boolean any = false;
        try {
            zin = new ZipInputStream(new java.io.FileInputStream(zip));
            ZipEntry ze;
            byte[] buf = new byte[128 * 1024];
            while ((ze = zin.getNextEntry()) != null) {
                if (cancelled) return false;
                File out = safeChild(dest, ze.getName());
                if (out == null) continue;
                if (ze.isDirectory()) {
                    out.mkdirs();
                    continue;
                }
                out.getParentFile().mkdirs();
                OutputStream os = new FileOutputStream(out);
                int n;
                while ((n = zin.read(buf)) > 0) os.write(buf, 0, n);
                os.close();
                any = true;
                progress(0, 0, "Extraindo " + ze.getName());
            }
        } catch (Exception e) {
            return false; // não era zip: mantém o .obb como está
        } finally {
            if (zin != null) try { zin.close(); } catch (Exception ignored) {}
        }
        return any;
    }

    /** Evita "zip slip": não deixa escrever fora da pasta do launcher. */
    private static File safeChild(File root, String rel) {
        try {
            File f = new File(root, rel);
            String base = root.getCanonicalPath() + File.separator;
            return f.getCanonicalPath().startsWith(base) ? f : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void progress(final long done, final long total, final String current) {
        main.post(new Runnable() {
            @Override public void run() { listener.onProgress(done, total, current); }
        });
    }

    private void finish(final boolean ok, final String msg) {
        main.post(new Runnable() {
            @Override public void run() { listener.onFinished(ok, msg); }
        });
    }
}
