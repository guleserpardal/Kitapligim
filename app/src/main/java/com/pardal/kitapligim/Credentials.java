package com.pardal.kitapligim;

import android.content.Context;
import android.security.keystore.*;
import android.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.security.KeyStore;
import java.nio.charset.StandardCharsets;

/** The Notion token stays on this device, encrypted by a non-exportable Android key. */
public final class Credentials {
    private final Context context;
    public Credentials(Context c){context=c.getApplicationContext();}
    private SecretKey key() throws Exception {KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);if(ks.containsAlias("kitapligim-notion"))return (SecretKey)ks.getKey("kitapligim-notion",null);KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder("kitapligim-notion",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());return g.generateKey();}
    public String get() throws Exception {String s=context.getSharedPreferences("connection",0).getString("secret","");if(s.isEmpty())return "";String[] p=s.split(":");Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(p[0],Base64.NO_WRAP)));return new String(c.doFinal(Base64.decode(p[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);}
    public void save(String token) throws Exception {Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());String s=Base64.encodeToString(c.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(c.doFinal(token.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);if(!context.getSharedPreferences("connection",0).edit().putString("secret",s).commit())throw new Exception("Bağlantı bilgisi kaydedilemedi.");}
    public void clear(){context.getSharedPreferences("connection",0).edit().clear().commit();}
}
