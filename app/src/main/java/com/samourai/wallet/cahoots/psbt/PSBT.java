package com.samourai.wallet.cahoots.psbt;

import com.samourai.wallet.util.Z85;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.*;
import org.bitcoinj.params.MainNetParams;
import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.encoders.Hex;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

//
// Partially Signed Bitcoin Transaction Format
//
public class PSBT {

    public static final int ENCODING_HEX = 0;
    public static final int ENCODING_BASE64 = 1;
    public static final int ENCODING_Z85 = 2;

    public static final byte PSBT_GLOBAL_UNSIGNED_TX = 0x00;
    //
    public static final byte PSBT_GLOBAL_REDEEM_SCRIPT = 0x01;
    public static final byte PSBT_GLOBAL_WITNESS_SCRIPT = 0x02;
    public static final byte PSBT_GLOBAL_BIP32_PUBKEY = 0x03;
    public static final byte PSBT_GLOBAL_NB_INPUTS = 0x04;

    public static final byte PSBT_IN_NON_WITNESS_UTXO = 0x00;
    public static final byte PSBT_IN_WITNESS_UTXO = 0x01;
    public static final byte PSBT_IN_PARTIAL_SIG = 0x02;
    public static final byte PSBT_IN_SIGHASH_TYPE = 0x03;
    public static final byte PSBT_IN_REDEEM_SCRIPT = 0x04;
    public static final byte PSBT_IN_WITNESS_SCRIPT = 0x05;
    public static final byte PSBT_IN_BIP32_DERIVATION = 0x06;
    public static final byte PSBT_IN_FINAL_SCRIPTSIG = 0x07;
    public static final byte PSBT_IN_FINAL_SCRIPTWITNESS = 0x08;

    public static final byte PSBT_OUT_REDEEM_SCRIPT = 0x00;
    public static final byte PSBT_OUT_WITNESS_SCRIPT = 0x01;
    public static final byte PSBT_OUT_BIP32_DERIVATION = 0x02;

    public static final String PSBT_MAGIC = "70736274";

    private static final int HARDENED = 0x80000000;

    private static final int STATE_START = 0;
    private static final int STATE_GLOBALS = 1;
    private static final int STATE_INPUTS = 2;
    private static final int STATE_OUTPUTS = 3;
    private static final int STATE_END = 4;

    private int currentState = 0;
    private int inputs = 0;
    private int outputs = 0;
    private boolean parseOK = false;

    private String strPSBT = null;
    private byte[] psbtBytes = null;
    private ByteBuffer psbtByteBuffer = null;
    private Transaction transaction = null;
    private List<PSBTEntry> psbtInputs = null;
    private List<PSBTEntry> psbtOutputs = null;

    private StringBuilder sbLog = null;

    public PSBT(String strPSBT, NetworkParameters params)   {

        if(!isPSBT(strPSBT))    {
            return;
        }

        psbtInputs = new ArrayList<PSBTEntry>();
        psbtOutputs = new ArrayList<PSBTEntry>();

        if(isBase64(strPSBT) && !isHex(strPSBT))    {
            this.strPSBT = Hex.toHexString(Base64.decode(strPSBT));
        }
        else if(Z85.getInstance().isZ85(strPSBT) && !PSBT.isHex(strPSBT))   {
            this.strPSBT = Hex.toHexString(Z85.getInstance().decode(strPSBT));
        }
        else    {
            this.strPSBT = strPSBT;
        }

        psbtBytes = Hex.decode(this.strPSBT);
        psbtByteBuffer = ByteBuffer.wrap(psbtBytes);

        this.transaction = new Transaction(params);

        sbLog = new StringBuilder();
    }

    public PSBT(byte[] psbt, NetworkParameters params) throws Exception   {

        String strPSBT = null;
        if(isGZIP(psbt))    {
            strPSBT = fromGZIP(psbt);
        }
        else    {
            strPSBT = Hex.toHexString(psbt);
        }

        if(!isPSBT(strPSBT))    {
            return;
        }

        psbtInputs = new ArrayList<PSBTEntry>();
        psbtOutputs = new ArrayList<PSBTEntry>();

        if(isBase64(strPSBT) && !isHex(strPSBT))    {
            this.strPSBT = Hex.toHexString(Base64.decode(strPSBT));
        }
        else if(Z85.getInstance().isZ85(strPSBT) && !PSBT.isHex(strPSBT))   {
            this.strPSBT = Hex.toHexString(Z85.getInstance().decode(strPSBT));
        }
        else    {
            this.strPSBT = strPSBT;
        }

        psbtBytes = Hex.decode(this.strPSBT);
        psbtByteBuffer = ByteBuffer.wrap(psbtBytes);

        this.transaction = new Transaction(params);

        sbLog = new StringBuilder();
    }

    public PSBT()   {
        psbtInputs = new ArrayList<PSBTEntry>();
        psbtOutputs = new ArrayList<PSBTEntry>();
        this.transaction = new Transaction(MainNetParams.get());
        sbLog = new StringBuilder();
    }

    public PSBT(Transaction transaction)   {
        psbtInputs = new ArrayList<PSBTEntry>();
        psbtOutputs = new ArrayList<PSBTEntry>();
        this.transaction = transaction;
        sbLog = new StringBuilder();
    }

    public PSBT(NetworkParameters params)   {
        psbtInputs = new ArrayList<PSBTEntry>();
        psbtOutputs = new ArrayList<PSBTEntry>();
        transaction = new Transaction(params);
        sbLog = new StringBuilder();
    }

    public PSBT(NetworkParameters params, int version)   {
        psbtInputs = new ArrayList<PSBTEntry>();
        psbtOutputs = new ArrayList<PSBTEntry>();
        transaction = new Transaction(params);
        transaction.setVersion(version);
        sbLog = new StringBuilder();
    }

    //
    // reader
    //
    public void read() throws Exception    {

        int seenInputs = 0;
        int seenOutputs = 0;

        psbtBytes = Hex.decode(strPSBT);
        psbtByteBuffer = ByteBuffer.wrap(psbtBytes);

        Log("--- ***** START ***** ---", true);
        Log("---  PSBT length:" + psbtBytes.length + " ---", true);
        Log("--- parsing header ---", true);

        byte[] magicBuf = new byte[4];
        psbtByteBuffer.get(magicBuf);
        if(!PSBT.PSBT_MAGIC.equalsIgnoreCase(Hex.toHexString(magicBuf)))    {
            throw new Exception("Invalid magic value");
        }

        byte sep = psbtByteBuffer.get();
        if(sep != (byte)0xff)    {
            throw new Exception("Bad 0xff separator:" + Hex.toHexString(new byte[] { sep }));
        }

        currentState = STATE_GLOBALS;

        while(psbtByteBuffer.hasRemaining()) {

            if(currentState == STATE_GLOBALS)    {
                Log("--- parsing globals ---", true);
            }
            else if(currentState == STATE_INPUTS)   {
                Log("--- parsing inputs ---", true);
            }
            else if(currentState == STATE_OUTPUTS)   {
                Log("--- parsing outputs ---", true);
            }
            else    {
                ;
            }

            PSBTEntry entry = parse();
            if(entry == null)    {
                Log("parse returned null entry", true);
//                exit(0);
            }
            entry.setState(currentState);

            if(entry.getKey() == null)    {         // length == 0
                switch (currentState)   {
                    case STATE_GLOBALS:
                        currentState = STATE_INPUTS;
                        break;
                    case STATE_INPUTS:
                        currentState = STATE_OUTPUTS;
                        break;
                    case STATE_OUTPUTS:
                        currentState = STATE_END;
                        break;
                    case STATE_END:
                        parseOK = true;
                        break;
                    default:
                        Log("unknown state", true);
                        break;
                }
            }
            else if(currentState == STATE_GLOBALS)    {
                switch(entry.getKeyType()[0])    {
                    case PSBT.PSBT_GLOBAL_UNSIGNED_TX:
                        Log("transaction", true);
                        transaction = new Transaction(getNetParams(), entry.getData());
                        inputs = transaction.getInputs().size();
                        outputs = transaction.getOutputs().size();
                        Log("inputs:" + inputs, true);
                        Log("outputs:" + outputs, true);
                        Log(transaction.toString(), true);
                        break;
                    default:
                        Log("not recognized key type:" + entry.getKeyType()[0], true);
                        break;
                }
            }
            else if(currentState == STATE_INPUTS)    {
                if(entry.getKeyType()[0] >= PSBT_IN_NON_WITNESS_UTXO && entry.getKeyType()[0] <= PSBT_IN_FINAL_SCRIPTWITNESS)    {
                    psbtInputs.add(entry);
                }
                else    {
                    Log("not recognized key type:" + entry.getKeyType()[0], true);
                }
            }
            else if(currentState == STATE_OUTPUTS)    {
                if(entry.getKeyType()[0] >= PSBT_OUT_REDEEM_SCRIPT && entry.getKeyType()[0] <= PSBT_OUT_BIP32_DERIVATION)    {
                    psbtInputs.add(entry);
                }
                else    {
                    Log("not recognized key type:" + entry.getKeyType()[0], true);
                }
            }
            else    {
                Log("panic", true);
            }

        }

        if(currentState == STATE_END)   {
            Log("--- ***** END ***** ---", true);
        }

        Log("", true);

    }

    private PSBTEntry parse()    {

        PSBTEntry entry = new PSBTEntry();

        try {
            int keyLen = PSBT.readCompactInt(psbtByteBuffer);
            Log("key length:" + keyLen, true);

            if(keyLen == 0x00)    {
                Log("separator 0x00", true);
                return entry;
            }

            byte[] key = new byte[keyLen];
            psbtByteBuffer.get(key);
            Log("key:" + Hex.toHexString(key), true);

            byte[] keyType = new byte[1];
            keyType[0] = key[0];
            Log("key type:" + Hex.toHexString(keyType), true);

            byte[] keyData = null;
            if(key.length > 1)    {
                keyData = new byte[key.length - 1];
                System.arraycopy(key, 1, keyData, 0, keyData.length);
                Log("key data:" + Hex.toHexString(keyData), true);
            }

            int dataLen = PSBT.readCompactInt(psbtByteBuffer);
            Log("data length:" + dataLen, true);

            byte[] data = new byte[dataLen];
            psbtByteBuffer.get(data);
            Log("data:" + Hex.toHexString(data), true);

            entry.setKey(key);
            entry.setKeyType(keyType);
            entry.setKeyData(keyData);
            entry.setData(data);

            return entry;

        }
        catch(Exception e) {
            Log("Exception:" + e.getMessage(), true);
            e.printStackTrace();
            return null;
        }

    }

    //
    // writer
    //
    public void addInput(byte type, byte[] keydata, byte[] data) throws Exception {
        psbtInputs.add(populateEntry(type, keydata, data));
    }

    public void addOutput(byte type, byte[] keydata, byte[] data) throws Exception {
        psbtOutputs.add(populateEntry(type, keydata, data));
    }

    private PSBTEntry populateEntry(byte type, byte[] keydata, byte[] data) throws Exception {

        PSBTEntry entry = new PSBTEntry();
        entry.setKeyType(new byte[] { type });
        entry.setKey(new byte[] { type });
        if(keydata != null)    {
            entry.setKeyData(keydata);
        }
        entry.setData(data);

        System.out.println("PSBT entry type:" + type);
        if(keydata != null)    {
            System.out.println("PSBT entry keydata:" + Hex.toHexString(keydata));
            System.out.println("PSBT entry keydata length:" + keydata.length);
        }
        System.out.println("PSBT entry data:" + Hex.toHexString(data));
        System.out.println("PSBT entry data length:" + data.length);

        return entry;
    }

    public byte[] serialize() throws IOException {

        byte[] serialized = transaction.bitcoinSerialize();
        byte[] txLen = PSBT.writeCompactInt(serialized.length);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // magic
        baos.write(Hex.decode(PSBT.PSBT_MAGIC), 0, Hex.decode(PSBT.PSBT_MAGIC).length);
        // separator
        baos.write((byte)0xff);

        // globals
        baos.write(writeCompactInt(1L));                                // key length
        baos.write((byte)0x00);                                             // key
        baos.write(txLen, 0, txLen.length);                             // value length
        baos.write(serialized, 0, serialized.length);                   // value
        baos.write((byte)0x00);

        // inputs
        for(PSBTEntry entry : psbtInputs)   {
            int keyLen = 1;
            if(entry.getKeyData() != null)    {
                keyLen += entry.getKeyData().length;
            }
            baos.write(writeCompactInt(keyLen));
            baos.write(entry.getKey());
            if(entry.getKeyData() != null)    {
                baos.write(entry.getKeyData());
            }
            baos.write(writeCompactInt(entry.getData().length));
            baos.write(entry.getData());
        }
        baos.write((byte)0x00);

        // outputs
        for(PSBTEntry entry : psbtOutputs)   {
            int keyLen = 1;
            if(entry.getKeyData() != null)    {
                keyLen += entry.getKeyData().length;
            }
            baos.write(writeCompactInt(keyLen));
            baos.write(entry.getKey());
            if(entry.getKeyData() != null)    {
                baos.write(entry.getKeyData());
            }
            baos.write(writeCompactInt(entry.getData().length));
            baos.write(entry.getData());
        }
        baos.write((byte)0x00);

        // eof
        baos.write((byte)0x00);

        psbtBytes = baos.toByteArray();
        strPSBT = Hex.toHexString(psbtBytes);
//        Log("psbt:" + strPSBT, true);

        return psbtBytes;
    }

    //
    //
    //
    public void setTransactionVersion(int version) {
        if(transaction != null)    {
            transaction.setVersion(version);
        }
    }

    public NetworkParameters getNetParams() {
        if(transaction != null)    {
            return transaction.getParams();
        }
        else    {
            return MainNetParams.get();
        }
    }

    public List<PSBTEntry> getPsbtInputs() {
        return psbtInputs;
    }

    public void setPsbtInputs(List<PSBTEntry> psbtInputs) {
        this.psbtInputs = psbtInputs;
    }

    public List<PSBTEntry> getPsbtOutputs() {
        return psbtOutputs;
    }

    public void setPsbtOutputs(List<PSBTEntry> psbtOutputs) {
        this.psbtOutputs = psbtOutputs;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public void clear()  {
        transaction = new Transaction(getNetParams());
        psbtInputs.clear();
        psbtOutputs.clear();
        strPSBT = null;
        psbtBytes = null;
        psbtByteBuffer.clear();
    }

    //
    // utils
    //
    public String toString()    {
        try {
            return Hex.toHexString(serialize());
        }
        catch(IOException ioe) {
            return null;
        }
    }

    public String toBase64String() throws IOException    {
        return Base64.toBase64String(serialize());
    }

    public String toZ85String() throws IOException    {
        return Z85.getInstance().encode(serialize());
    }

    public byte[] toGZIP() throws IOException {

        String data = toString();

        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length());
        GZIPOutputStream gzip = new GZIPOutputStream(bos);
        gzip.write(data.getBytes());
        gzip.close();
        byte[] compressed = bos.toByteArray();
        bos.close();

        return compressed;
    }

    public static int readCompactInt(ByteBuffer psbtByteBuffer) throws Exception  {

        byte b = psbtByteBuffer.get();

        switch(b)    {
            case (byte)0xfd: {
                byte[] buf = new byte[2];
                psbtByteBuffer.get(buf);
                ByteBuffer byteBuffer = ByteBuffer.wrap(buf);
//                log("value:" + Hex.toHexString(buf), true);
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                return byteBuffer.getShort();
            }
            case (byte)0xfe: {
                byte[] buf = new byte[4];
                psbtByteBuffer.get(buf);
                ByteBuffer byteBuffer = ByteBuffer.wrap(buf);
//                log("value:" + Hex.toHexString(buf), true);
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                return byteBuffer.getInt();
            }
            case (byte)0xff: {
                byte[] buf = new byte[8];
                psbtByteBuffer.get(buf);
                ByteBuffer byteBuffer = ByteBuffer.wrap(buf);
//                log("value:" + Hex.toHexString(buf), true);
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                throw new Exception("Data too long:" + byteBuffer.getLong());
            }
            default:
//                Log("compact int value:" + "value:" + Hex.toHexString(new byte[] { b }), true);
                return (int)(b & 0xff);
        }

    }

    public static byte[] writeCompactInt(long val)   {

        ByteBuffer bb = null;

        if(val < 0xfdL)    {
            bb = ByteBuffer.allocate(1);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.put((byte)val);
        }
        else if(val < 0xffffL)   {
            bb = ByteBuffer.allocate(3);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.put((byte)0xfd);
            /*
            bb.put((byte)(val & 0xff));
            bb.put((byte)((val >> 8) & 0xff));
            */
            bb.putShort((short)val);
        }
        else if(val < 0xffffffffL)   {
            bb = ByteBuffer.allocate(5);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.put((byte)0xfe);
            bb.putInt((int)val);
        }
        else    {
            bb = ByteBuffer.allocate(9);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.put((byte)0xff);
            bb.putLong(val);
        }

        return bb.array();
    }

    public static Pair<Long, Byte[]> readSegwitInputUTXO(byte[] utxo)    {
        byte[] val = new byte[8];
        byte[] scriptPubKey = new byte[utxo.length - val.length];

        System.arraycopy(utxo, 0, val, 0, val.length);
        System.arraycopy(utxo, val.length, scriptPubKey, 0, scriptPubKey.length);

        ArrayUtils.reverse(val);
        long lval = Long.parseLong(Hex.toHexString(val), 16);

        int i = 0;
        Byte[] scriptPubKeyBuf = new Byte[scriptPubKey.length];
        for(byte b : scriptPubKey)   {
            scriptPubKeyBuf[i++] = b;
        }

        return Pair.of(Long.valueOf(lval), scriptPubKeyBuf);
    }

    public static byte[] writeSegwitInputUTXO(long value, byte[] scriptPubKey)    {

        byte[] ret = new byte[scriptPubKey.length + Long.BYTES];

        // long to byte array
        ByteBuffer xlat = ByteBuffer.allocate(Long.BYTES);
        xlat.order(ByteOrder.LITTLE_ENDIAN);
        xlat.putLong(0, value);
        byte[] val = new byte[Long.BYTES];
        xlat.get(val);

        System.arraycopy(val, 0, ret, 0, Long.BYTES);
        System.arraycopy(scriptPubKey, 0, ret, Long.BYTES, scriptPubKey.length);

        return ret;
    }

    public static String readBIP32Derivation(byte[] path) {

        byte[] dbuf = new byte[path.length];
        System.arraycopy(path, 0, dbuf, 0, path.length);
        ByteBuffer bb = ByteBuffer.wrap(dbuf);
        byte[] buf = new byte[4];

        // fingerprint
        bb.get(buf);
        byte[] fingerprint = new byte[4];
        System.arraycopy(buf, 0, fingerprint, 0, fingerprint.length);
//        System.out.println("fingerprint:" + Hex.toHexString(fingerprint));

        // purpose
        bb.get(buf);
        ArrayUtils.reverse(buf);
        ByteBuffer pbuf = ByteBuffer.wrap(buf);
        int purpose = pbuf.getInt();
        if(purpose >= HARDENED)    {
            purpose -= HARDENED;
        }
//        System.out.println("purpose:" + purpose);

        // coin type
        bb.get(buf);
        ArrayUtils.reverse(buf);
        ByteBuffer tbuf = ByteBuffer.wrap(buf);
        int type = tbuf.getInt();
        if(type >= HARDENED)    {
            type -= HARDENED;
        }
//        System.out.println("type:" + type);

        // account
        bb.get(buf);
        ArrayUtils.reverse(buf);
        ByteBuffer abuf = ByteBuffer.wrap(buf);
        int account = abuf.getInt();
        if(account >= HARDENED)    {
            account -= HARDENED;
        }
//        System.out.println("account:" + account);

        // chain
        bb.get(buf);
        ArrayUtils.reverse(buf);
        ByteBuffer cbuf = ByteBuffer.wrap(buf);
        int chain = cbuf.getInt();
//        System.out.println("chain:" + chain);

        // index
        bb.get(buf);
        ArrayUtils.reverse(buf);
        ByteBuffer ibuf = ByteBuffer.wrap(buf);
        int index = ibuf.getInt();
//        System.out.println("index:" + index);

        String ret = "m/" + purpose + "'/" + type + "'/" + account + "'/" + chain + "/" + index;

        return ret;
    }

    public static byte[] writeBIP32Derivation(byte[] fingerprint, int purpose, int type, int account, int chain, int index) {

        // fingerprint and integer values to BIP32 derivation buffer
        byte[] bip32buf = new byte[24];

        System.arraycopy(fingerprint, 0, bip32buf, 0, fingerprint.length);

        ByteBuffer xlat = ByteBuffer.allocate(Integer.BYTES);
        xlat.order(ByteOrder.LITTLE_ENDIAN);
        xlat.putInt(0, purpose + HARDENED);
        byte[] out = new byte[Integer.BYTES];
        xlat.get(out);
//        System.out.println("purpose:" + Hex.toHexString(out));
        System.arraycopy(out, 0, bip32buf, fingerprint.length, out.length);

        xlat.clear();
        xlat.order(ByteOrder.LITTLE_ENDIAN);
        xlat.putInt(0, type + HARDENED);
        xlat.get(out);
//        System.out.println("type:" + Hex.toHexString(out));
        System.arraycopy(out, 0, bip32buf, fingerprint.length + out.length, out.length);

        xlat.clear();
        xlat.order(ByteOrder.LITTLE_ENDIAN);
        xlat.putInt(0, account + HARDENED);
        xlat.get(out);
//        System.out.println("account:" + Hex.toHexString(out));
        System.arraycopy(out, 0, bip32buf, fingerprint.length + (out.length * 2), out.length);

        xlat.clear();
        xlat.order(ByteOrder.LITTLE_ENDIAN);
        xlat.putInt(0, chain);
        xlat.get(out);
//        System.out.println("chain:" + Hex.toHexString(out));
        System.arraycopy(out, 0, bip32buf, fingerprint.length + (out.length * 3), out.length);

        xlat.clear();
        xlat.order(ByteOrder.LITTLE_ENDIAN);
        xlat.putInt(0, index);
        xlat.get(out);
//        System.out.println("index:" + Hex.toHexString(out));
        System.arraycopy(out, 0, bip32buf, fingerprint.length + (out.length * 4), out.length);

//        System.out.println("bip32 derivation:" + Hex.toHexString(bip32buf));

        return bip32buf;
    }

    public static boolean isHex(String s)   {
        String regex = "^[0-9A-Fa-f]+$";

        if(s.matches(regex))    {
            return true;
        }
        else    {
            return false;
        }

    }

    public static boolean isBase64(String s)   {
        String regex = "^[0-9A-Za-z\\\\+=/]+$";

        if(s.matches(regex))    {
            return true;
        }
        else    {
            return false;
        }

    }

    public static boolean isPSBT(String s)   {

        if(isHex(s) && s.startsWith(PSBT_MAGIC))    {
            return true;
        }
        else if(isBase64(s) && Hex.toHexString(Base64.decode(s)).startsWith(PSBT_MAGIC))    {
            return true;
        }
        else if(Z85.getInstance().isZ85(s) && Hex.toHexString(Z85.getInstance().decode(s)).startsWith(PSBT_MAGIC))    {
            return true;
        }
        else    {
            return false;
        }

    }

    public boolean isGZIP(byte[] buf)   {
        int head = ((int) buf[0] & 0xff) | ((buf[1] << 8 ) & 0xff00 );
        return java.util.zip.GZIPInputStream.GZIP_MAGIC == head;
    }

    public String fromGZIP(byte[] gzip) throws Exception    {
        ByteArrayInputStream bis = new ByteArrayInputStream(gzip);
        GZIPInputStream gis = new GZIPInputStream(bis);
        BufferedReader br = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        gis.close();
        bis.close();

        return sb.toString();
    }

    public void Log(String s, boolean eol)  {

        sbLog.append(s);
        System.out.print(s);
        if(eol)    {
            sbLog.append("\n");
            System.out.print("\n");
        }

    }

    public String dump()    {
        return sbLog.toString();
    }

}
