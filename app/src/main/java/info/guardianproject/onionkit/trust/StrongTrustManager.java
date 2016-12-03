
package info.guardianproject.onionkit.trust;

/**
 * $RCSfile$ $Revision: $ $Date: $
 *
 * Copyright 2003-2005 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import android.content.Context;
import android.util.Log;

import com.samourai.R;

import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.ASN1String;
import org.spongycastle.asn1.DERSequence;
import org.spongycastle.asn1.x509.GeneralName;
import org.spongycastle.asn1.x509.X509Extensions;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.X509TrustManager;

/**
 * Updated multifaceted StrongTrustManager Based on TrustManager from Jive:
 * Trust manager that checks all certificates presented by the server. This
 * class is used during TLS negotiation. It is possible to disable/enable some
 * or all checkings by configuring the {@link ConnectionConfiguration}. The
 * truststore file that contains knows and trusted CA root certificates can also
 * be configure in {@link ConnectionConfiguration}.
 *
 * @autor n8fr8
 * @author Gaston Dombiak
 */
public abstract class StrongTrustManager implements X509TrustManager {

    private static final String TAG = "ONIONKIT";
    public static boolean SHOW_DEBUG_OUTPUT = true;

    private final static Pattern cnPattern = Pattern.compile("(?i)(cn=)([^,]*)");

    private final static String TRUSTSTORE_TYPE = "BKS";
    private final static String TRUSTSTORE_PASSWORD = "changeit";

    /** Holds the domain of the remote server we are trying to connect */
    private String mServer;
    private String mDomain;

    private KeyStore mTrustStore; // root CAs
    private Context mContext;

    boolean mExpiredCheck = true;
    boolean mVerifyChain = true;
    boolean mVerifyRoot = true;
    boolean mSelfSignedAllowed = false;
    boolean mCheckMatchingDomain = true;
    boolean mCheckChainCrypto = true;

    boolean mNotifyVerificationSuccess = false;
    boolean mNotifyVerificationFail = true;

    /**
     * Construct a trust manager for XMPP connections. Certificates are
     * considered verified if:
     * <ul>
     * <li>The root certificate is in our trust store
     * <li>The chain is valid
     * <li>The leaf certificate contains the identity of the domain or the
     * requested server
     * </ul>
     *
     * @param mContext - the Android mContext for presenting notifications
     * @param configuration - the XMPP configuration
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     */
    public StrongTrustManager(Context context) throws KeyStoreException, NoSuchAlgorithmException,
            CertificateException, IOException {

        mContext = context;

        InputStream in = null;

        mTrustStore = KeyStore.getInstance(TRUSTSTORE_TYPE);
        // load our bundled cacerts from raw assets
        in = mContext.getResources().openRawResource(R.raw.debiancacerts);
        mTrustStore.load(in, TRUSTSTORE_PASSWORD.toCharArray());
    }

    public StrongTrustManager(Context context, KeyStore keystore) throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException {

        mContext = context;
        mTrustStore = keystore;
    }

    public KeyStore getKeyStore()
    {
        return mTrustStore;
    }

    /**
     * This method is deprecated! Use
     * {@link StrongTrustManager#StrongTrustManager(Context)},
     * {@link StrongTrustManager#StrongTrustManager(Context, KeyStore)}, or
     * {@link StrongTrustManager#StrongTrustManager(Context, String, int)} instead.
     */
    @Deprecated
    public StrongTrustManager(Context mContext, String appName, int appIcon)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
    {
        this(mContext);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0]; // we accept anyone now, but this should
                                       // return the list from our trust Root CA
                                       // Store
    }

    public void setNotifyVerificationSuccess(boolean notifyVerificationSuccess)
    {
        mNotifyVerificationSuccess = notifyVerificationSuccess;
    }

    public void setNotifyVerificationFail(boolean notifyVerificationFail)
    {
        mNotifyVerificationFail = notifyVerificationFail;
    }

    static boolean checkMatchingDomain(String domain, String server,
            Collection<String> peerIdentities) {
        boolean found = false;

        for (String peerIdentity : peerIdentities) {
            // Check if the certificate uses a wildcard.
            // This indicates that immediate subdomains are valid.
            if (peerIdentity.startsWith("*.")) {
                // Remove wildcard: *.foo.info -> .foo.info
                String stem = peerIdentity.substring(1);

                // Remove a single label: baz.bar.foo.info -> .bar.foo.info and
                // compare
                if (server.replaceFirst("[^.]+", "").equalsIgnoreCase(stem)
                        || domain.replaceFirst("[^.]+", "").equalsIgnoreCase(stem)) {
                    found = true;
                    break;
                }
            } else if (server.equalsIgnoreCase(peerIdentity)
                    || domain.equalsIgnoreCase(peerIdentity)) {
                found = true;
                break;
            }
        }
        return found;
    }

    /**
     * Returns the identity of the remote server as defined in the specified
     * certificate. The identity is defined in the subjectDN of the certificate
     * and it can also be defined in the subjectAltName extension of type
     * "xmpp". When the extension is being used then the identity defined in the
     * extension in going to be returned. Otherwise, the value stored in the
     * subjectDN is returned.
     *
     * @param x509Certificate the certificate the holds the identity of the
     *            remote server.
     * @return the identity of the remote server as defined in the specified
     *         certificate.
     */
    public static Collection<String> getPeerIdentity(X509Certificate x509Certificate) {
        // Look the identity in the subjectAltName extension if available
        Collection<String> names = getSubjectAlternativeNames(x509Certificate);
        if (names.isEmpty()) {
            String name = x509Certificate.getSubjectDN().getName();
            Matcher matcher = cnPattern.matcher(name);
            if (matcher.find()) {
                name = matcher.group(2);
            }
            // Create an array with the unique identity
            names = new ArrayList<String>();
            names.add(name);
        }
        return names;
    }

    /**
     * Returns the JID representation of an XMPP entity contained as a
     * SubjectAltName extension in the certificate. If none was found then
     * return <tt>null</tt>.
     *
     * @param certificate the certificate presented by the remote entity.
     * @return the JID representation of an XMPP entity contained as a
     *         SubjectAltName extension in the certificate. If none was found
     *         then return <tt>null</tt>.
     */
    static Collection<String> getSubjectAlternativeNames(X509Certificate certificate) {
        List<String> identities = new ArrayList<String>();
        try {
            byte[] extVal = certificate.getExtensionValue(X509Extensions.SubjectAlternativeName
                    .getId());
            // Check that the certificate includes the SubjectAltName extension
            if (extVal == null) {
                return Collections.emptyList();
            }

            ASN1OctetString octs = (ASN1OctetString) ASN1Primitive.fromByteArray(extVal);

            @SuppressWarnings("rawtypes")
            Enumeration it = DERSequence.getInstance(ASN1Primitive.fromByteArray(octs.getOctets()))
                    .getObjects();

            while (it.hasMoreElements())
            {
                GeneralName genName = GeneralName.getInstance(it.nextElement());
                switch (genName.getTagNo())
                {
                    case GeneralName.dNSName:
                        identities.add(((ASN1String) genName.getName()).getString());
                        break;
                }
            }
            return Collections.unmodifiableCollection(identities);

        } catch (Exception e) {
            Log.e(TAG, "getSubjectAlternativeNames()", e);
        }

        return identities;
    }

    public String getFingerprint(X509Certificate cert, String type)
            throws NoSuchAlgorithmException, CertificateEncodingException
    {
        MessageDigest md = MessageDigest.getInstance(type);
        byte[] publicKey = md.digest(cert.getEncoded());

        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < publicKey.length; i++) {

            String appendString = Integer.toHexString(0xFF & publicKey[i]);

            if (appendString.length() == 1)
                hexString.append("0");
            hexString.append(appendString);
            hexString.append(' ');
        }

        return hexString.toString();

    }

    public KeyStore getTrustStore() {
        return mTrustStore;
    }

    public String getTrustStorePassword()
    {
        return TRUSTSTORE_PASSWORD;
    }

    public void setTrustStore(KeyStore mTrustStore) {
        this.mTrustStore = mTrustStore;
    }

    public boolean isExpiredCheck() {
        return mExpiredCheck;
    }

    public void setExpiredCheck(boolean mExpiredCheck) {
        this.mExpiredCheck = mExpiredCheck;
    }

    public boolean isVerifyChain() {
        return mVerifyChain;
    }

    public void setVerifyChain(boolean mVerifyChain) {
        this.mVerifyChain = mVerifyChain;
    }

    public boolean isVerifyRoot() {
        return mVerifyRoot;
    }

    public void setVerifyRoot(boolean mVerifyRoot) {
        this.mVerifyRoot = mVerifyRoot;
    }

    public boolean isSelfSignedAllowed() {
        return mSelfSignedAllowed;
    }

    public void setSelfSignedAllowed(boolean mSelfSignedAllowed) {
        this.mSelfSignedAllowed = mSelfSignedAllowed;
    }

    public boolean isCheckMatchingDomain() {
        return mCheckMatchingDomain;
    }

    public void setCheckMatchingDomain(boolean mCheckMatchingDomain) {
        this.mCheckMatchingDomain = mCheckMatchingDomain;
    }

    public String getServer() {
        return mServer;
    }

    public void setServer(String server) {
        this.mServer = server;
    }

    public String getDomain() {
        return mDomain;
    }

    public void setDomain(String domain) {
        this.mDomain = domain;
    }

    public boolean hasCheckChainCrypto() {
        return mCheckChainCrypto;
    }

    public void setCheckChainCrypto(boolean mCheckChainCrypto) {
        this.mCheckChainCrypto = mCheckChainCrypto;
    }
}
