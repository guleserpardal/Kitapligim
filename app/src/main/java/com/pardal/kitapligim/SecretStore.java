package com.pardal.kitapligim;

public interface SecretStore {
    String get() throws Exception;
    void save(String secret) throws Exception;
    void clear();
}
