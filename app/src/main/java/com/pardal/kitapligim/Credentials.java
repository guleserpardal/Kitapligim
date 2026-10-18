package com.pardal.kitapligim;

import android.content.Context;
import android.security.keystore.*;
import android.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.security.KeyStore;
import java.nio.charset.StandardCharsets;

/** A secret (Notion token, assistant server key) stays on this device, encrypted by a non-exportable Android key. */
public final class Credentials implements SecretStore {
    private final Context context;
    private final String prefs,alias;
    public Credentials(Context c){this(c,"connection","kitapligim-notion");}
    public Credentials(Context c,String prefsName,String keyAlias){context=c.getApplicationContext();prefs=prefsName;alias=keyAlias;}
    private SecretKey key() throws Exception {KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);if(ks.containsAlias(alias))return (SecretKey)ks.getKey(alias,null);KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());return g.generateKey();}
    @Override public String get() throws Exception {String s=context.getSharedPreferences(prefs,0).getString("secret","");if(s.isEmpty())return "";String[] p=s.split(":");Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(p[0],Base64.NO_WRAP)));return new String(c.doFinal(Base64.decode(p[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);}
    @Override public void save(String token) throws Exception {Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());String s=Base64.encodeToString(c.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(c.doFinal(token.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);if(!context.getSharedPreferences(prefs,0).edit().putString("secret",s).commit())throw new Exception("Bağlantı bilgisi kaydedilemedi.");}
    @Override public void clear(){context.getSharedPreferences(prefs,0).edit().clear().commit();}
}
