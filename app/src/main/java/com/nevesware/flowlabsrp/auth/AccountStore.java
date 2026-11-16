package com.nevesware.flowlabsrp.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Patterns;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Contas LOCAIS (salvas só neste aparelho), usadas enquanto o Supabase não está conectado.
 * A senha nunca é guardada: só um hash PBKDF2 com salt.
 * Quando o Supabase entrar, este é o arquivo a trocar por chamadas à API de auth.
 */
public final class AccountStore {

    private static final String PREFS = "flowlabs_accounts";
    private static final int ITERATIONS = 12000;

    /** Padrão de nome de RP: Nome_Sobrenome (ex.: Joao_Silva). */
    private static final Pattern NICK = Pattern.compile("^[A-Z][a-zA-Z]{1,15}_[A-Z][a-zA-Z]{1,15}$");

    private final SharedPreferences p;

    public AccountStore(Context c) {
        p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------- Validação (retorna mensagem de erro ou null) ----------

    public static String validateNickname(String nick) {
        if (nick == null || nick.length() == 0) return "Digite seu nome no RP.";
        if (!NICK.matcher(nick).matches()) {
            return "Use o formato Nome_Sobrenome (ex.: Joao_Silva), só letras, com iniciais maiúsculas.";
        }
        return null;
    }

    public static String validateEmail(String email) {
        if (email == null || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "E-mail inválido.";
        }
        return null;
    }

    public static String validatePassword(String pass) {
        if (pass == null || pass.length() < 6) return "A senha precisa ter pelo menos 6 caracteres.";
        boolean letter = false, digit = false;
        for (int i = 0; i < pass.length(); i++) {
            char ch = pass.charAt(i);
            if (Character.isLetter(ch)) letter = true;
            if (Character.isDigit(ch)) digit = true;
        }
        if (!letter || !digit) return "Use letras e números na senha.";
        return null;
    }

    // ---------- Operações ----------

    /** Cria a conta. Retorna a sessão ou lança IllegalArgumentException com a mensagem para o usuário. */
    public Session register(String nickname, String email, String password) {
        String err = validateNickname(nickname);
        if (err == null) err = validateEmail(email);
        if (err == null) err = validatePassword(password);
        if (err != null) throw new IllegalArgumentException(err);

        JSONArray all = all();
        for (int i = 0; i < all.length(); i++) {
            JSONObject a = all.optJSONObject(i);
            if (a.optString("email").equalsIgnoreCase(email)) {
                throw new IllegalArgumentException("Já existe uma conta com esse e-mail.");
            }
            if (a.optString("nickname").equalsIgnoreCase(nickname)) {
                throw new IllegalArgumentException("Esse nome de RP já está em uso.");
            }
        }

        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            JSONObject a = new JSONObject();
            a.put("id", UUID.randomUUID().toString());
            a.put("nickname", nickname);
            a.put("email", email.toLowerCase());
            a.put("salt", Base64.encodeToString(salt, Base64.NO_WRAP));
            a.put("hash", hash(password, salt));
            a.put("created_at", System.currentTimeMillis());
            all.put(a);
            p.edit().putString("list", all.toString()).apply();
            return toSession(a);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Não foi possível criar a conta.");
        }
    }

    /** Login por e-mail OU nome de RP. */
    public Session login(String user, String password) {
        if (user == null || user.trim().length() == 0 || password == null || password.length() == 0) {
            throw new IllegalArgumentException("Preencha usuário e senha.");
        }
        user = user.trim();
        JSONArray all = all();
        for (int i = 0; i < all.length(); i++) {
            JSONObject a = all.optJSONObject(i);
            if (a.optString("email").equalsIgnoreCase(user) || a.optString("nickname").equalsIgnoreCase(user)) {
                try {
                    byte[] salt = Base64.decode(a.optString("salt"), Base64.NO_WRAP);
                    if (constantTimeEquals(hash(password, salt), a.optString("hash"))) {
                        return toSession(a);
                    }
                } catch (Exception ignored) {}
                throw new IllegalArgumentException("Senha incorreta.");
            }
        }
        throw new IllegalArgumentException("Conta não encontrada neste aparelho.");
    }

    public void updateNickname(String userId, String nickname) {
        JSONArray all = all();
        for (int i = 0; i < all.length(); i++) {
            JSONObject a = all.optJSONObject(i);
            if (!a.optString("id").equals(userId) && a.optString("nickname").equalsIgnoreCase(nickname)) {
                throw new IllegalArgumentException("Esse nome de RP já está em uso.");
            }
        }
        for (int i = 0; i < all.length(); i++) {
            JSONObject a = all.optJSONObject(i);
            if (a.optString("id").equals(userId)) {
                try { a.put("nickname", nickname); } catch (Exception ignored) {}
            }
        }
        p.edit().putString("list", all.toString()).apply();
    }

    public static Session guest() {
        Session s = new Session();
        s.type = Session.GUEST;
        s.userId = "guest-" + UUID.randomUUID().toString().substring(0, 8);
        s.nickname = "Convidado_" + (1000 + new Random().nextInt(9000));
        s.displayName = s.nickname;
        s.email = "";
        s.createdAt = System.currentTimeMillis();
        return s;
    }

    // ---------- Internos ----------

    private JSONArray all() {
        try {
            return new JSONArray(p.getString("list", "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static Session toSession(JSONObject a) {
        Session s = new Session();
        s.type = Session.LOCAL;
        s.userId = a.optString("id");
        s.nickname = a.optString("nickname");
        s.email = a.optString("email");
        s.displayName = s.nickname;
        s.createdAt = a.optLong("created_at");
        return s;
    }

    private static String hash(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);
        byte[] out = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
        return Base64.encodeToString(out, Base64.NO_WRAP);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}
