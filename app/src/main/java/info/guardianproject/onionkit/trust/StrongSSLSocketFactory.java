package info.guardianproject.onionkit.trust;

import info.guardianproject.onionkit.OnionKitHelper;

import java.io.IOException;
import java.net.Proxy;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import ch.boye.httpclientandroidlib.conn.scheme.LayeredSchemeSocketFactory;
import ch.boye.httpclientandroidlib.params.HttpParams;

public class StrongSSLSocketFactory extends
		ch.boye.httpclientandroidlib.conn.ssl.SSLSocketFactory implements
		LayeredSchemeSocketFactory {

	private SSLSocketFactory mFactory = null;

	private Proxy mProxy = null;

	public static final String TLS = "TLS";
	public static final String SSL = "SSL";
	public static final String SSLV2 = "SSLv2";

	// private X509HostnameVerifier mHostnameVerifier = new
	// StrictHostnameVerifier();
	// private final HostNameResolver mNameResolver = new
	// StrongHostNameResolver();

	private boolean mEnableStongerDefaultSSLCipherSuite = true;
	private boolean mEnableStongerDefaultProtocalVersion = true;

	private TrustManager mTrustManager;
	
	private String[] mProtocols;
	private String[] mCipherSuites;

	public StrongSSLSocketFactory(Context context,
			TrustManager trustManager, KeyStore keyStore, String keyStorePassword)
			throws KeyManagementException, UnrecoverableKeyException,
			NoSuchAlgorithmException, KeyStoreException, CertificateException,
			IOException {
		super(keyStore);

		mTrustManager = trustManager;

		SSLContext sslContext = SSLContext.getInstance("TLS");
		TrustManager[] tm = new TrustManager[] { mTrustManager };
		KeyManager[] km = createKeyManagers(
				keyStore,
				keyStorePassword);
		sslContext.init(km, tm, new SecureRandom());

		mFactory = sslContext.getSocketFactory();

	}

	private void readSSLParameters(SSLSocket sslSocket) {
		List<String> protocolsToEnable = new ArrayList<String>();
		List<String> supportedProtocols = Arrays.asList(sslSocket.getSupportedProtocols());
		for(String enabledProtocol : OnionKitHelper.ENABLED_PROTOCOLS) {
			if(supportedProtocols.contains(enabledProtocol)) {
				protocolsToEnable.add(enabledProtocol);
			}
		}
		this.mProtocols = protocolsToEnable.toArray(new String[protocolsToEnable.size()]);
		
		List<String> cipherSuitesToEnable = new ArrayList<String>();
		List<String> supportedCipherSuites = Arrays.asList(sslSocket.getSupportedCipherSuites());
		for(String enabledCipherSuite : OnionKitHelper.ENABLED_CIPHERS) {
			if(supportedCipherSuites.contains(enabledCipherSuite)) {
				cipherSuitesToEnable.add(enabledCipherSuite);
			}
		}
		this.mCipherSuites = cipherSuitesToEnable.toArray(new String[cipherSuitesToEnable.size()]);
	}
	
	private KeyManager[] createKeyManagers(final KeyStore keystore,
			final String password) throws KeyStoreException,
			NoSuchAlgorithmException, UnrecoverableKeyException {
		if (keystore == null) {
			throw new IllegalArgumentException("Keystore may not be null");
		}
		KeyManagerFactory kmfactory = KeyManagerFactory
				.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmfactory.init(keystore, password != null ? password.toCharArray()
				: null);
		return kmfactory.getKeyManagers();
	}

	@Override
	public Socket createSocket() throws IOException {
		Socket newSocket = mFactory.createSocket();
		enableStrongerDefaults(newSocket);
		return newSocket;
	}

	@Override
	public Socket createSocket(Socket socket, String host, int port,
			boolean autoClose) throws IOException, UnknownHostException {

		Socket newSocket = mFactory.createSocket(socket, host, port, autoClose);

		enableStrongerDefaults(newSocket);

		return newSocket;
	}

	/**
	 * Defaults the SSL connection to use a strong cipher suite and TLS version
	 * 
	 * @param socket
	 */
	private void enableStrongerDefaults(Socket socket) {
		if (isSecure(socket)) {
			SSLSocket sslSocket = (SSLSocket) socket;
			readSSLParameters(sslSocket);

			if (mEnableStongerDefaultProtocalVersion && mProtocols != null) {
				sslSocket.setEnabledProtocols(mProtocols);
			}

			if (mEnableStongerDefaultSSLCipherSuite && mCipherSuites != null) {
				sslSocket.setEnabledCipherSuites(mCipherSuites);
			}
		}
	}

	@Override
	public boolean isSecure(Socket sock) throws IllegalArgumentException {
		return (sock instanceof SSLSocket);
	}

	public void setProxy(Proxy proxy) {
		mProxy = proxy;
	}

	public Proxy getProxy() {
		return mProxy;
	}

	public boolean isEnableStongerDefaultSSLCipherSuite() {
		return mEnableStongerDefaultSSLCipherSuite;
	}

	public void setEnableStongerDefaultSSLCipherSuite(boolean enable) {
		this.mEnableStongerDefaultSSLCipherSuite = enable;
	}

	public boolean isEnableStongerDefaultProtocalVersion() {
		return mEnableStongerDefaultProtocalVersion;
	}

	public void setEnableStongerDefaultProtocalVersion(boolean enable) {
		this.mEnableStongerDefaultProtocalVersion = enable;
	}

	@Override
	public Socket createSocket(HttpParams httpParams) throws IOException {
		Socket newSocket = mFactory.createSocket();

		enableStrongerDefaults(newSocket);

		return newSocket;

	}

	@Override
	public Socket createLayeredSocket(Socket arg0, String arg1, int arg2,
			boolean arg3) throws IOException, UnknownHostException {
		return ((LayeredSchemeSocketFactory) mFactory).createLayeredSocket(
				arg0, arg1, arg2, arg3);
	}

}
