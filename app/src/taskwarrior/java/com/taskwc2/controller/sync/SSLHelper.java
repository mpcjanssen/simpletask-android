package com.taskwc2.controller.sync;

import android.util.Base64;

import com.taskwc2.controller.sync.der.DerInputStream;
import com.taskwc2.controller.sync.der.DerValue;

import nl.mpcjanssen.simpletask.Logger;
import nl.mpcjanssen.simpletask.remote.Compat;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class SSLHelper {


    protected static byte[] fromStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int size = 0;
        while ((size = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, size);
        }
        inputStream.close();
        outputStream.close();
        return outputStream.toByteArray();
    }

    protected static byte[] parseDERFromPEM(String data) throws IOException {
        String[] tokens = data.split("\n");
        StringBuilder b64 = new StringBuilder();
        int delimiters = 0;
        for (String token : tokens) {
            if (token.startsWith("-----") && token.trim().endsWith("-----")) {
                delimiters++;
                if (delimiters  == 2) {
                    break;
                }
                continue;
            }
            if (delimiters == 1) {
                b64.append(token.trim());
            }
        }
        return Base64.decode(b64.toString(), Base64.DEFAULT);
    }

    protected static X509Certificate loadCertificate(InputStream stream) throws CertificateException, FileNotFoundException {
        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        return (X509Certificate) fact.generateCertificate(stream);
    }

    protected static PrivateKey loadPrivateKey(InputStream stream) throws IOException, GeneralSecurityException {
        byte[] key = fromStream(stream);
        DerInputStream derReader = new DerInputStream(parseDERFromPEM(new String(key)));
        DerValue[] seq = derReader.getSequence(0);
        if (seq.length < 9) {
            throw new GeneralSecurityException("Could not parse a PKCS1 private key.");
        }
        BigInteger modulus = seq[1].getBigInteger();
        BigInteger publicExp = seq[2].getBigInteger();
        BigInteger privateExp = seq[3].getBigInteger();
        BigInteger prime1 = seq[4].getBigInteger();
        BigInteger prime2 = seq[5].getBigInteger();
        BigInteger exp1 = seq[6].getBigInteger();
        BigInteger exp2 = seq[7].getBigInteger();
        BigInteger crtCoef = seq[8].getBigInteger();

        RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(modulus, publicExp, privateExp, prime1, prime2, exp1, exp2, crtCoef);

        KeyFactory factory = KeyFactory.getInstance("RSA");

        return factory.generatePrivate(keySpec);
    }

    protected static KeyManager[] keyManagerFactoryPEM(InputStream certStream, InputStream keyStream) throws GeneralSecurityException, IOException {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        Certificate cert = loadCertificate(certStream);
        keyStore.load(null);
//        logger.d("Keystore:", cert.getPublicKey().getAlgorithm(), cert.getPublicKey().getFormat());
        keyStore.setCertificateEntry("certificate", cert);
        keyStore.setKeyEntry("private-key", loadPrivateKey(keyStream), "".toCharArray(), new Certificate[]{cert});
        kmf.init(keyStore, "".toCharArray());
        return kmf.getKeyManagers();
    }

    public static TrustType parseTrustType(String trust) {
        TrustType result = TrustType.Strict;
        if ("ignore hostname".equals(trust)) {
            result = SSLHelper.TrustType.Hostname;
        }
        if ("allow all".equals(trust)) {
            result = SSLHelper.TrustType.All;
        }
        return result;
    }

    public enum TrustType {Strict, Hostname, All};

    protected static TrustManager[] trustManagerFactoryPEM(InputStream stream, final TrustType trustType) throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null);
        final X509Certificate cert = loadCertificate(stream);
        keyStore.setCertificateEntry("ca", cert);
        tmf.init(keyStore);
        TrustManager[] orig = tmf.getTrustManagers();
        TrustManager[] result = new TrustManager[orig.length+1];
        System.arraycopy(orig, 0, result, 1, orig.length);
        result[0] = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                for (X509Certificate c : chain) { // Check every cert

                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {

                return new X509Certificate[]{cert};
            }
        };
        return orig; // Not finished
    }

    protected static SSLSocketFactory tlsSocket(KeyManager[] kmf, TrustManager[] tmf) throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException, InvalidKeySpecException, UnrecoverableKeyException, KeyManagementException, UnrecoverableKeyException {

        SSLContext context = Compat.Companion.produceLevelAware(16, new Compat.Producer<SSLContext>() {
            @Override
            public SSLContext produce() {
                try {
                    return SSLContext.getInstance("TLSv1.2");
                } catch (NoSuchAlgorithmException e) {
                    return null;
                }
            }
        }, new Compat.Producer<SSLContext>() {
            @Override
            public SSLContext produce() {
                try {
                    return SSLContext.getInstance("TLSv1");
                } catch (NoSuchAlgorithmException e) {
                    return null;
                }
            }
        });
        context.init(kmf, tmf, null);
        return context.getSocketFactory();
    }

    public static SSLSocketFactory tlsSocket(InputStream caStream, InputStream certStream, InputStream keyStream, TrustType trustType)
        throws GeneralSecurityException, IOException {
        return tlsSocket(keyManagerFactoryPEM(certStream, keyStream), trustManagerFactoryPEM(caStream, trustType));
    }
}
