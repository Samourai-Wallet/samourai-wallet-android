package com.samourai.wallet.paynym.api

import android.content.Context
import android.util.Log
import com.samourai.wallet.BuildConfig
import com.samourai.wallet.access.AccessFactory
import com.samourai.wallet.api.APIFactory
import com.samourai.wallet.api.AbstractApiService
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.BIP47Util
import com.samourai.wallet.bip47.rpc.NotSecp256k1Exception
import com.samourai.wallet.bip47.rpc.PaymentCode
import com.samourai.wallet.crypto.DecryptionException
import com.samourai.wallet.payload.PayloadUtil
import com.samourai.wallet.tor.TorManager
import com.samourai.wallet.tor.TorManager.getProxy
import com.samourai.wallet.util.CharSequenceX
import com.samourai.wallet.util.MessageSignUtil
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.bitcoinj.crypto.MnemonicException
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.spec.InvalidKeySpecException
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * samourai-wallet-android
 *
 */
class PayNymApiService(private val paynymCode: String, private val context: Context) : AbstractApiService() {


    private var payNymToken: String? = null

    override fun buildClient(url: HttpUrl): OkHttpClient {
        val builder = OkHttpClient.Builder()

        if (url.host.contains("onion")) {
            this.getHostNameVerifier(builder)
        }

        builder.connectTimeout(45, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .callTimeout(45, TimeUnit.SECONDS)

        if (TorManager.isRequired()) {
            builder.proxy(getProxy())
            builder.connectTimeout(120, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .callTimeout(120, TimeUnit.SECONDS)
        }
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        }
        builder.addInterceptor { chain ->
            val original = chain.request()

            val newBuilder = original.newBuilder()
            if (!payNymToken.isNullOrEmpty()) {
                newBuilder
                        .header("auth-token", payNymToken!!)
                        .header("client", "samourai-wallet")
            }
            newBuilder.method(original.method, original.body)
            chain.proceed(newBuilder.build())
        }
        return builder.build()
    }

    private suspend fun getToken() {
        val builder = Request.Builder();
        builder.url("$URL/token")
        val payload = JSONObject().apply {
            put("code", paynymCode)
        }
        val body: RequestBody = RequestBody.create(JSON, payload.toString())
        val response = this.executeRequest(builder.post(body).build())
        if (response.isSuccessful) {
            val status = response.body?.string()
            val tokenResponse = JSONObject(status)
            if (tokenResponse.has("token")) {
                this.payNymToken = tokenResponse.getString("token")
            } else {
                throw Exception("Invalid paynym token response")
            }
        } else {
            throw Exception("Unable to retrieve paynym token")
        }
    }


    suspend fun claim(): Response {
        createPayNym()
        if (payNymToken == null) {
            getToken()
        }
        val payload = JSONObject().apply {
            put("signature", getSig())
        }
        val builder = Request.Builder();
        val body: RequestBody = RequestBody.create(JSON, payload.toString())
        builder.url("$URL/claim")
        val response = this.executeRequest(builder.post(body).build())
        if (response.isSuccessful) {
            val jsonStr = response.body?.string();
            val json = JSONObject(jsonStr);
            if (json.has("token")) {
                payNymToken = json.getString("token")
            }
            return addPayNym()
        } else {
            throw IOException("Unable to claim paynym")
        }
    }


    suspend fun addPayNym(): Response {
        if (payNymToken == null) {
            getToken()
        }
        val paynymCode = BIP47Util.getInstance(context).paymentCode.toString()
        val paynymCodeFeat = BIP47Util.getInstance(context).featurePaymentCode
        val payload = JSONObject().apply {
            put("nym", paynymCode)
            put("code", paynymCodeFeat)
            put("signature", getSig())
        }
        val builder = Request.Builder();
        val body: RequestBody = RequestBody.create(JSON, payload.toString())
        builder.url("$URL/nym/add")
        return this.executeRequest(builder.post(body).build())
    }


    suspend fun createPayNym(): JSONObject {
        val payload = JSONObject().apply {
            put("code", paynymCode)
        }
        val builder = Request.Builder();
        val body: RequestBody = RequestBody.create(JSON, payload.toString())
        builder.url("$URL/create")
        val response = this.executeRequest(builder.post(body).build())
        if (response.isSuccessful) {
            val jsonStr = response.body?.string()
            val jsonObject = JSONObject(jsonStr)
            if (jsonObject.has("token")) {
                this.payNymToken = jsonObject.getString("token")
            }
            return jsonObject
        } else {
            val jsonStr = response.body?.string()
            val jsonObject = JSONObject(jsonStr)
            if (jsonObject.has("message")) {
                throw IOException(jsonObject.getString("message"))
            } else {
                throw IOException("Unable to register paynym")
            }
        }
    }

    suspend fun getNymInfo(): Response {
        if (payNymToken == null) {
            getToken()
        }
        val payload = JSONObject().apply {
            put("nym", paynymCode)
        }
        val builder = Request.Builder();
        val body: RequestBody = RequestBody
                .create(JSON, payload.toString())
        builder.url("$URL/nym")
        return executeRequest(builder.post(body).build())
    }


    public suspend fun follow(pcode: String): Response {

        if (payNymToken == null) {
            getToken()
        }
        val builder = Request.Builder();
        val obj = JSONObject()

        obj.put("target", pcode)
        obj.put("signature", getSig())

        val body: RequestBody = RequestBody
                .create(JSON, obj.toString())

        builder.url("$URL/follow")
        return executeRequest(builder.post(body).build())
    }

    public suspend fun unfollow(pcode: String): Response {

        if (payNymToken == null) {
            getToken()
        }
        val builder = Request.Builder();
        val obj = JSONObject()

        obj.put("target", pcode)
        obj.put("signature", getSig())

        val body: RequestBody = RequestBody
                .create(JSON, obj.toString())

        builder.url("$URL/unfollow")
        return executeRequest(builder.post(body).build())
    }


    public fun syncPcode(pcode: String) {
        try {
            val payment_code = PaymentCode(pcode)
            var idx = 0
            var loop = true
            val addrs = ArrayList<String>()
            while (loop) {
                addrs.clear()
                for (i in idx until idx + 20) {
//                            Log.i("BIP47Activity", "sync receive from " + i + ":" + BIP47Util.getInstance(BIP47Activity.this).getReceivePubKey(payment_code, i));
                    BIP47Meta.getInstance().idx4AddrLookup[BIP47Util.getInstance(context).getReceivePubKey(payment_code, i)] = i
                    BIP47Meta.getInstance().pCode4AddrLookup[BIP47Util.getInstance(context).getReceivePubKey(payment_code, i)] = payment_code.toString()
                    addrs.add(BIP47Util.getInstance(context).getReceivePubKey(payment_code, i))
                    //                            Log.i("BIP47Activity", "p2pkh " + i + ":" + BIP47Util.getInstance(BIP47Activity.this).getReceiveAddress(payment_code, i).getReceiveECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
                }
                val s = addrs.toTypedArray()
                val nb = APIFactory.getInstance(context).syncBIP47Incoming(s)
                //                        Log.i("BIP47Activity", "sync receive idx:" + idx + ", nb == " + nb);
                if (nb == 0) {
                    loop = false
                }
                idx += 20
            }
            idx = 0
            loop = true
            BIP47Meta.getInstance().setOutgoingIdx(pcode, 0)
            while (loop) {
                addrs.clear()
                for (i in idx until idx + 20) {
                    val sendAddress = BIP47Util.getInstance(context).getSendAddress(payment_code, i)
                    //                            Log.i("BIP47Activity", "sync send to " + i + ":" + sendAddress.getSendECKey().toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString());
//                            BIP47Meta.getInstance().setOutgoingIdx(payment_code.toString(), i);
                    BIP47Meta.getInstance().idx4AddrLookup[BIP47Util.getInstance(context).getSendPubKey(payment_code, i)] = i
                    BIP47Meta.getInstance().pCode4AddrLookup[BIP47Util.getInstance(context).getSendPubKey(payment_code, i)] = payment_code.toString()
                    addrs.add(BIP47Util.getInstance(context).getSendPubKey(payment_code, i))
                }
                val s = addrs.toTypedArray()
                val nb = APIFactory.getInstance(context).syncBIP47Outgoing(s)
                //                        Log.i("BIP47Activity", "sync send idx:" + idx + ", nb == " + nb);
                if (nb == 0) {
                    loop = false
                }
                idx += 20
            }
            BIP47Meta.getInstance().pruneIncoming()
            PayloadUtil.getInstance(context.applicationContext).saveWalletToJSON(CharSequenceX(AccessFactory.getInstance(context.applicationContext).guid + AccessFactory.getInstance(context.applicationContext).pin))
        } catch (ioe: IOException) {
        } catch (je: JSONException) {
        } catch (de: DecryptionException) {
        } catch (nse: NotSecp256k1Exception) {
        } catch (ikse: InvalidKeySpecException) {
        } catch (ike: InvalidKeyException) {
        } catch (nsae: NoSuchAlgorithmException) {
        } catch (nspe: NoSuchProviderException) {
        } catch (mle: MnemonicException.MnemonicLengthException) {
        } catch (ex: Exception) {
        }
    }

    private fun getSig(): String? {
        return MessageSignUtil.getInstance(context).signMessage(BIP47Util.getInstance(context).notificationAddress.ecKey, payNymToken)
    }


    //This will be replaced using DI injection in the future
    companion object {
        const val PAYNYM_API = "https://paynym.is/";
        const val URL = "${PAYNYM_API}api/v1";
        var payNymApiService: PayNymApiService? = null
        fun getInstance(code: String, context: Context): PayNymApiService {
            if (payNymApiService == null) {
                payNymApiService = PayNymApiService(code, context)
            } else {
                if (code != payNymApiService!!.paynymCode) {
                    payNymApiService = PayNymApiService(code, context)
                }
            }
            return payNymApiService!!
        }
    }


}