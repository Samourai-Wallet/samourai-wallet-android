package com.samourai.wallet.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class PrefsUtil {

	public static final String GUID = "guid";
	public static final String GUID_V = "guid_version";
	public static final String ACCESS_HASH = "accessHash";
	public static final String ACCESS_HASH2 = "accessHash2";
	public static final String FP = "fp";
	public static final String ICON_HIDDEN = "iconHidden";
	public static final String ACCEPT_REMOTE = "acceptRemote";
	public static final String CURRENT_FEE_TYPE = "currentFeeType";
 	public static final String WALLET_ORIGIN = "origin";
	public static final String FIRST_RUN = "1stRun";
	public static final String SIM_IMSI = "IMSI";
	public static final String CHECK_SIM = "checkSIM";
	public static final String ALERT_MOBILE_NO = "alertSMSNo";
	public static final String TRUSTED_LOCK = "trustedMobileOnly";
	public static final String SCRAMBLE_PIN = "scramblePin";
	public static final String AUTO_BACKUP = "autoBackup";
	public static final String SPEND_TYPE = "spendType";
	public static final String USE_RICOCHET = "useRicochet";
	public static final String USE_TRUSTED_NODE = "useTrustedNode";
	public static final String RBF_OPT_IN = "rbfOptIn";
	public static final String FEE_PROVIDER_SEL = "feeProviderSel";
	public static final String BROADCAST_TX = "broadcastTx";
	public static final String TESTNET = "testnet";
	public static final String USE_SEGWIT = "useSegwit";
	public static final String USE_LIKE_TYPED_CHANGE = "useLikeTypedChange";
	public static final String XPUB44LOCK = "xpub44lock";
	public static final String XPUB49LOCK = "xpub49lock";
	public static final String XPUB84LOCK = "xpub84lock";
//	public static final String XPUB44REG = "xpub44reg";
	public static final String XPUB49REG = "xpub49reg";
	public static final String XPUB84REG = "xpub84reg";
	public static final String PAYNYM_CLAIMED = "paynymClaimed";
	public static final String PAYNYM_REFUSED = "paynymRefused";
	public static final String PAYNYM_FEATURED_SEGWIT = "paynymFeatured_v1";
	public static final String IS_RESTORE = "isRestore";
	public static final String HAPTIC_PIN = "hapticPin";
	public static final String RICOCHET_STAGGERED = "ricochetStaggeredDelivery";
	public static final String ENABLE_TOR = "enable_tor";
	public static final String OFFLINE = "offline";

	private static Context context = null;
	private static PrefsUtil instance = null;

	private PrefsUtil() { ; }

	public static PrefsUtil getInstance(Context ctx) {

		context = ctx;

		if(instance == null) {
			instance = new PrefsUtil();
		}

		return instance;
	}

	public String getValue(String name, String value) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getString(name, (value == null || value.length() < 1) ? "" : value);
	}

	public boolean setValue(String name, String value) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putString(name, (value == null || value.length() < 1) ? "" : value);
		return editor.commit();
	}

	public int getValue(String name, int value) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getInt(name, 0);
	}

	public boolean setValue(String name, int value) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putInt(name, (value < 0) ? 0 : value);
		return editor.commit();
	}

	public long getValue(String name, long value) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getLong(name, 0L);
	}

	public boolean setValue(String name, long value) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putLong(name, (value < 0L) ? 0L : value);
		return editor.commit();
	}

	public boolean getValue(String name, boolean value) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getBoolean(name, value);
	}

	public boolean setValue(String name, boolean value) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(name, value);
		return editor.commit();
	}

	public boolean removeValue(String name) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.remove(name);
		return editor.commit();
	}

	public boolean has(String name) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.contains(name);
	}

	public boolean clear() {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.clear();
		return editor.commit();
	}

}
