package com.samourai.wallet.send;

import com.samourai.wallet.util.Hash;

import org.bitcoinj.core.Utils;

import java.util.ArrayList;
import java.util.List;

public class BitcoinScript {

    final List<byte[]> chunks;
    final byte[] program;
    int cursor;

    public static final int ScriptOutTypeStrange = 0;
    public static final int ScriptOutTypeAddress = 1;
    public static final int ScriptOutTypePubKey = 2;
    public static final int ScriptOutTypeP2SH = 3;
    public static final int ScriptOutTypeMultiSig = 4;
    public static final int ScriptInTypeStrange = 5;
    public static final int ScriptInTypeAddress = 6;
    public static final int ScriptInTypePubKey = 7;

    // push value

    public static final int OP_0 = 0;
    public static final int OP_FALSE = 0;
    public static final int OP_PUSHDATA1 = 76;
    public static final int OP_PUSHDATA2 = 77;
    public static final int OP_PUSHDATA4 = 78;
    public static final int OP_1NEGATE = 79;
    public static final int OP_RESERVED = 80;
    public static final int OP_1 = 81;
    public static final int OP_2 = 82;
    public static final int OP_3 = 83;
    public static final int OP_4 = 84;
    public static final int OP_5 = 85;
    public static final int OP_6 = 86;
    public static final int OP_7 = 87;
    public static final int OP_8 = 88;
    public static final int OP_9 = 89;
    public static final int OP_10 = 90;
    public static final int OP_11 = 91;
    public static final int OP_12 = 92;
    public static final int OP_13 = 93;
    public static final int OP_14 = 94;
    public static final int OP_15 = 95;
    public static final int OP_16 = 96;

    // control
    public static final int OP_NOP = 97;
    public static final int OP_VER = 98;
    public static final int OP_IF = 99;
    public static final int OP_NOTIF = 100;
    public static final int OP_VERIF = 101;
    public static final int OP_VERNOTIF = 102;
    public static final int OP_ELSE = 103;
    public static final int OP_ENDIF = 104;
    public static final int OP_VERIFY = 105;
    public static final int OP_RETURN = 106;

    // stack ops
    public static final int OP_TOALTSTACK = 107;
    public static final int OP_FROMALTSTACK = 108;
    public static final int OP_2DROP = 109;
    public static final int OP_2DUP = 110;
    public static final int OP_3DUP = 111;
    public static final int OP_2OVER = 112;
    public static final int OP_2ROT = 113;
    public static final int OP_2SWAP = 114;
    public static final int OP_IFDUP = 115;
    public static final int OP_DEPTH = 116;
    public static final int OP_DROP = 117;
    public static final int OP_DUP = 118;
    public static final int OP_NIP = 119;
    public static final int OP_OVER = 120;
    public static final int OP_PICK = 121;
    public static final int OP_ROLL = 122;
    public static final int OP_ROT = 123;
    public static final int OP_SWAP = 124;
    public static final int OP_TUCK = 125;

    // splice ops
    public static final int OP_CAT = 126;
    public static final int OP_SUBSTR = 127;
    public static final int OP_LEFT = 128;
    public static final int OP_RIGHT = 129;
    public static final int OP_SIZE = 130;

    // bit logic
    public static final int OP_INVERT = 131;
    public static final int OP_AND = 132;
    public static final int OP_OR = 133;
    public static final int OP_XOR = 134;
    public static final int OP_EQUAL = 135;
    public static final int OP_EQUALVERIFY = 136;
    public static final int OP_RESERVED1 = 137;
    public static final int OP_RESERVED2 = 138;

    // numeric
    public static final int OP_1ADD = 139;
    public static final int OP_1SUB = 140;
    public static final int OP_2MUL = 141;
    public static final int OP_2DIV = 142;
    public static final int OP_NEGATE = 143;
    public static final int OP_ABS = 144;
    public static final int OP_NOT = 145;
    public static final int OP_0NOTEQUAL = 146;

    public static final int OP_ADD = 147;
    public static final int OP_SUB = 148;
    public static final int OP_MUL = 149;
    public static final int OP_DIV = 150;
    public static final int OP_MOD = 151;
    public static final int OP_LSHIFT = 152;
    public static final int OP_RSHIFT = 153;

    public static final int OP_BOOLAND = 154;
    public static final int OP_BOOLOR = 155;
    public static final int OP_NUMEQUAL = 156;
    public static final int OP_NUMEQUALVERIFY = 157;
    public static final int OP_NUMNOTEQUAL = 158;
    public static final int OP_LESSTHAN = 159;
    public static final int OP_GREATERTHAN = 160;
    public static final int OP_LESSTHANOREQUAL = 161;
    public static final int OP_GREATERTHANOREQUAL = 162;
    public static final int OP_MIN = 163;
    public static final int OP_MAX = 164;

    public static final int OP_WITHIN = 165;

    // crypto
    public static final int OP_RIPEMD160 = 166;
    public static final int OP_SHA1 = 167;
    public static final int OP_SHA256 = 168;
    public static final int OP_HASH160 = 169;
    public static final int OP_HASH256 = 170;
    public static final int OP_CODESEPARATOR = 171;
    public static final int OP_CHECKSIG = 172;
    public static final int OP_CHECKSIGVERIFY = 173;
    public static final int OP_CHECKMULTISIG = 174;
    public static final int OP_CHECKMULTISIGVERIFY = 175;

    // expansion
    public static final int OP_NOP1 = 176;
    public static final int OP_NOP2 = 177;
    public static final int OP_NOP3 = 178;
    public static final int OP_NOP4 = 179;
    public static final int OP_NOP5 = 180;
    public static final int OP_NOP6 = 181;
    public static final int OP_NOP7 = 182;
    public static final int OP_NOP8 = 183;
    public static final int OP_NOP9 = 184;
    public static final int OP_NOP10 = 185;

    // template matching params
    public static final int OP_PUBKEYHASH = 253;
    public static final int OP_PUBKEY = 254;
    public static final int OP_INVALIDOPCODE = 255;

    public static String[] op_names = new String[256];

    static {
        op_names[OP_0] = "OP_0";
        op_names[OP_FALSE] = "OP_FALSE";
        op_names[OP_PUSHDATA1] = "OP_PUSHDATA1";
        op_names[OP_PUSHDATA2] = "OP_PUSHDATA2";
        op_names[OP_PUSHDATA4] = "OP_PUSHDATA4";
        op_names[OP_1NEGATE] = "OP_1NEGATE";
        op_names[OP_RESERVED] = "OP_RESERVED";
        op_names[OP_1] = "OP_1";
        op_names[OP_2] = "OP_2";
        op_names[OP_3] = "OP_3";
        op_names[OP_4] = "OP_4";
        op_names[OP_5] = "OP_5";
        op_names[OP_6] = "OP_6";
        op_names[OP_7] = "OP_7";
        op_names[OP_8] = "OP_8";
        op_names[OP_9] = "OP_9";
        op_names[OP_10] = "OP_10";
        op_names[OP_11] = "OP_11";
        op_names[OP_12] = "OP_12";
        op_names[OP_13] = "OP_13";
        op_names[OP_14] = "OP_14";
        op_names[OP_15] = "OP_15";
        op_names[OP_16] = "OP_16";

        op_names[OP_NOP] = "OP_NOP";
        op_names[OP_VER] = "OP_VER";
        op_names[OP_IF] = "OP_IF";
        op_names[OP_NOTIF] = "OP_NOTIF";
        op_names[OP_VERIF] = "OP_VERIF";
        op_names[OP_VERNOTIF] = "OP_VERNOTIF";
        op_names[OP_ELSE] = "OP_ELSE";
        op_names[OP_ENDIF] = "OP_ENDIF";
        op_names[OP_VERIFY] = "OP_VERIFY";
        op_names[OP_RETURN] = "OP_RETURN";

        op_names[OP_TOALTSTACK] = "OP_TOALTSTACK";
        op_names[OP_FROMALTSTACK] = "OP_FROMALTSTACK";
        op_names[OP_2DROP] = "OP_2DROP";
        op_names[OP_2DUP] = "OP_2DUP";
        op_names[OP_3DUP] = "OP_3DUP";
        op_names[OP_2OVER] = "OP_2OVER";
        op_names[OP_2ROT] = "OP_2ROT";
        op_names[OP_2SWAP] = "OP_2SWAP";
        op_names[OP_IFDUP] = "OP_IFDUP";
        op_names[OP_DEPTH] = "OP_DEPTH";
        op_names[OP_DUP] = "OP_DUP";
        op_names[OP_DROP] = "OP_DROP";
        op_names[OP_NIP] = "OP_NIP";
        op_names[OP_OVER] = "OP_OVER";
        op_names[OP_PICK] = "OP_PICK";
        op_names[OP_ROLL] = "OP_ROLL";
        op_names[OP_ROT] = "OP_ROT";
        op_names[OP_SWAP] = "OP_SWAP";
        op_names[OP_TUCK] = "OP_TUCK";

        op_names[OP_CAT] = "OP_CAT";
        op_names[OP_SUBSTR] = "OP_SUBSTR";
        op_names[OP_LEFT] = "OP_LEFT";
        op_names[OP_RIGHT] = "OP_RIGHT";
        op_names[OP_SIZE] = "OP_SIZE";

        op_names[OP_INVERT] = "OP_INVERT";
        op_names[OP_AND] = "OP_AND";
        op_names[OP_OR] = "OP_OR";
        op_names[OP_XOR] = "OP_XOR";
        op_names[OP_EQUAL] = "OP_EQUAL";
        op_names[OP_EQUALVERIFY] = "OP_EQUALVERIFY";
        op_names[OP_RESERVED1] = "OP_RESERVED1";
        op_names[OP_RESERVED2] = "OP_RESERVED2";

        op_names[OP_1ADD] = "OP_1ADD";
        op_names[OP_1SUB] = "OP_1SUB";
        op_names[OP_2MUL] = "OP_2MUL";
        op_names[OP_2DIV] = "OP_2DIV";
        op_names[OP_NEGATE] = "OP_NEGATE";
        op_names[OP_ABS] = "OP_ABS";
        op_names[OP_NOT] = "OP_NOT";
        op_names[OP_0NOTEQUAL] = "OP_0NOTEQUAL";

        op_names[OP_ADD] = "OP_ADD";
        op_names[OP_SUB] = "OP_SUB";
        op_names[OP_MUL] = "OP_MUL";
        op_names[OP_DIV] = "OP_DIV";
        op_names[OP_MOD] = "OP_MOD";
        op_names[OP_LSHIFT] = "OP_LSHIFT";
        op_names[OP_RSHIFT] = "OP_RSHIFT";

        op_names[OP_BOOLAND] = "OP_BOOLAND";
        op_names[OP_BOOLOR] = "OP_BOOLOR";
        op_names[OP_NUMEQUAL] = "OP_NUMEQUAL";
        op_names[OP_NUMEQUALVERIFY] = "OP_NUMEQUALVERIFY";
        op_names[OP_NUMNOTEQUAL] = "OP_NUMNOTEQUAL";
        op_names[OP_LESSTHAN] = "OP_LESSTHAN";
        op_names[OP_GREATERTHAN] = "OP_GREATERTHAN";
        op_names[OP_LESSTHANOREQUAL] = "OP_LESSTHANOREQUAL";
        op_names[OP_GREATERTHANOREQUAL] = "OP_GREATERTHANOREQUAL";
        op_names[OP_MIN] = "OP_MIN";
        op_names[OP_MAX] = "OP_MAX";

        op_names[OP_WITHIN] = "OP_WITHIN";
        op_names[OP_RIPEMD160] = "OP_RIPEMD160";
        op_names[OP_SHA1] = "OP_SHA1";
        op_names[OP_SHA256] = "OP_SHA256";
        op_names[OP_HASH160] = "OP_HASH160";
        op_names[OP_HASH256] = "OP_HASH256";
        op_names[OP_CODESEPARATOR] = "OP_CODESEPARATOR";
        op_names[OP_CHECKSIG] = "OP_CHECKSIG";
        op_names[OP_CHECKSIGVERIFY] = "OP_CHECKSIGVERIFY";
        op_names[OP_CHECKMULTISIG] = "OP_CHECKMULTISIG";
        op_names[OP_CHECKMULTISIGVERIFY] = "OP_CHECKMULTISIGVERIFY";

        op_names[OP_NOP1] = "OP_NOP1";
        op_names[OP_NOP2] = "OP_NOP2";
        op_names[OP_NOP3] = "OP_NOP3";
        op_names[OP_NOP4] = "OP_NOP4";
        op_names[OP_NOP5] = "OP_NOP5";
        op_names[OP_NOP6] = "OP_NOP6";
        op_names[OP_NOP7] = "OP_NOP7";
        op_names[OP_NOP8] = "OP_NOP8";
        op_names[OP_NOP9] = "OP_NOP9";
        op_names[OP_NOP10] = "OP_NOP10";

        op_names[OP_PUBKEYHASH] = "OP_PUBKEYHASH";
        op_names[OP_PUBKEY] = "OP_PUBKEY";
        op_names[OP_INVALIDOPCODE] = "OP_INVALIDOPCODE";
    }

    private byte[] getData(int len) throws Exception {
        try {
            byte[] buf = new byte[len];
            System.arraycopy(program, cursor, buf, 0, len);
            cursor += len;
            return buf;
        }
        catch (ArrayIndexOutOfBoundsException e) {
            // We want running out of data in the array to be treated as a
            // handleable script parsing exception,
            // not something that abnormally terminates the app.
            throw new Exception("Failed read of " + len + " bytes", e);
        }
    }

    public byte[] getChunk(int chunk) {
        return chunks.get(chunk);
    }

    private int readByte() {
        return 0xFF & program[cursor++];
    }

    public static int unsignedByteToInt(byte b) {
        return b & 0xFF;
    }

    public static void writeOpcode(List<Byte> bytes, int op0) {
        bytes.add((byte) op0);
    }

    public static void writeBytes(List<Byte> bytes, byte[] data) {

        if (data.length < OP_PUSHDATA1) {
            bytes.add((byte) data.length);
        }
        else if (data.length <= 0xff) {
            bytes.add((byte) OP_PUSHDATA1);
            bytes.add((byte) data.length);
        }
        else if (data.length <= 0xffff) {
            bytes.add((byte) OP_PUSHDATA2);
            bytes.add((byte) (data.length & 0xff));
            bytes.add((byte) ((data.length >>> 8) & 0xff));
        }
        else {
            bytes.add((byte) OP_PUSHDATA4);
            bytes.add((byte) (data.length & 0xff));
            bytes.add((byte) ((data.length >>> 8) & 0xff));
            bytes.add((byte) ((data.length >>> 16) & 0xff));
            bytes.add((byte) ((data.length >>> 24) & 0xff));
        }

        for(byte b : data) {
            bytes.add(b);
        }
    }

    public static BitcoinScript createSimpleOutBitcoinScript(BitcoinAddress address) throws Exception {

        // Now create the redemption script
        List<Byte> bytes = new ArrayList<Byte>();

        if (address.getVersion() == 0) {
            BitcoinScript.writeOpcode(bytes, BitcoinScript.OP_DUP);
            BitcoinScript.writeOpcode(bytes, BitcoinScript.OP_HASH160);
            BitcoinScript.writeBytes(bytes, address.getHash160().getBytes());
            BitcoinScript.writeOpcode(bytes, BitcoinScript.OP_EQUALVERIFY);
            BitcoinScript.writeOpcode(bytes, BitcoinScript.OP_CHECKSIG);

            return new BitcoinScript(bytes);
        }
        else if (address.getVersion() == 5) {
            BitcoinScript.writeOpcode(bytes, BitcoinScript.OP_HASH160);
            BitcoinScript.writeBytes(bytes, address.getHash160().getBytes());
            BitcoinScript.writeOpcode(bytes, BitcoinScript.OP_EQUAL);

            return new BitcoinScript(bytes);
        }
        else {
            throw new Exception("Bitcoin address version " + address.getVersion() + " not supported yet");
        }
    }

    public BitcoinScript(List<Byte> bytes) throws Exception {

        byte[] scriptBytes = new byte[bytes.size()];
        for (int i = 0; i < scriptBytes.length; i++) {
            scriptBytes[i] = bytes.get(i);
        }

        this.program = scriptBytes;
        this.chunks = new ArrayList<byte[]>(); // Arbitrary choice of initial size.

        init();
    }

    private void init() throws Exception {
        cursor = 0;
        int length = program.length;

        while (cursor < length) {
            int opcode = readByte();

            if (opcode > 0 && opcode < OP_PUSHDATA1) {
                // Read some bytes of data, where how many is the opcode value itself
                chunks.add(getData(opcode)); // opcode == len here.
            }
            else if (opcode == OP_PUSHDATA1) {
                int len = readByte();
                chunks.add(getData(len));
            }
            else if (opcode == OP_PUSHDATA2) {
                // Read a short, then read that many bytes of data.
                int len = readByte() | (readByte() << 8);
                chunks.add(getData(len));
            }
            else if (opcode == OP_PUSHDATA4) {
                // Read a uint32, then read that many bytes of data.
                throw new Exception("PUSHDATA4: Unimplemented");
            }
            else {
                chunks.add(new byte[] { (byte) opcode });
            }
        }
    }

    public BitcoinScript(byte[] bytes) throws Exception {

        this.program = bytes;
        this.chunks = new ArrayList<byte[]>(); // Arbitrary choice of initial
        // size.

        init();
    }

    public byte[] getProgram() {
        return program;
    }

    public int getInType() {

        if (this.chunks.size() == 1) {
            // Direct IP to IP transactions only have the public key in their scriptSig
            return ScriptInTypeAddress;
        }
        else if (this.chunks.size() == 2 && this.chunks.get(0).length > 1) {
            return ScriptInTypePubKey;
        }
        else {
            return ScriptInTypeStrange;
        }
    }

    public BitcoinScript getP2SHScript() throws Exception {
        return new BitcoinScript(this.chunks.get(0));
    }

    public int getOutType() {

        try {
            if (this.chunks.size() > 3 && unsignedByteToInt(this.program[this.program.length - 1]) == OP_CHECKMULTISIG) {
                // Transfer to Bitcoin address
                return ScriptOutTypeMultiSig;

            }
            else if (this.program.length == 25
                    && unsignedByteToInt(this.program[0]) == OP_DUP
                    && unsignedByteToInt(this.program[1]) == OP_HASH160
                    && this.program[2] == 20
                    && unsignedByteToInt(this.program[23]) == OP_EQUALVERIFY
                    && unsignedByteToInt(this.program[24]) == OP_CHECKSIG) {

                // Transfer to Bitcoin address
                return ScriptOutTypeAddress;

            }
            else if (this.program.length == 23
                    && unsignedByteToInt(this.program[0]) == OP_HASH160
                    && unsignedByteToInt(this.program[1]) == 0x14
                    && unsignedByteToInt(this.program[22]) == OP_EQUAL) {

                // Pay to Script Hash
                return ScriptOutTypeP2SH;
            }
            else if (this.chunks.size() == 2
                    && unsignedByteToInt(this.chunks.get(1)[0]) == OP_CHECKSIG) {
                // Transfer to Public Key
                return ScriptOutTypePubKey;
            }
            else {
                return ScriptOutTypeStrange;
            }
        }
        catch (Exception e) {
            return ScriptOutTypeStrange;
        }

    }

    public Hash getSimpleInPubKey() {

        switch (this.getInType()) {
            case ScriptInTypePubKey:
                return new Hash(this.chunks.get(1));
            default:
                return null;
        }

    }

    public BitcoinAddress getAddress() {

        try {

            switch (this.getOutType()) {
                case ScriptOutTypeAddress:
                    return new BitcoinAddress(new Hash(this.chunks.get(2)),
                            (short) 0);
                case ScriptOutTypePubKey:
                    return new BitcoinAddress(new Hash(
                            Utils.sha256hash160(this.chunks.get(0))), (short) 0);
                case ScriptOutTypeP2SH:
                    return new BitcoinAddress(new Hash(
                            Utils.sha256hash160(this.chunks.get(0))), (short) 5);
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public int extractPubKeys(List<Hash> pubkeys) {

        switch (this.getOutType()) {

            case ScriptOutTypePubKey:

                pubkeys.add(new Hash(this.chunks.get(0)));
                return 1;

            case ScriptOutTypeMultiSig:

                int n = unsignedByteToInt(this.chunks.get(this.chunks.size() - 2)[0]) - OP_1 + 1;

                for (int i = 0; i < n; i++) {
                    pubkeys.add(0, new Hash(this.chunks.get(this.chunks.size() - 3 - i)));
                }

                int m = unsignedByteToInt(this.chunks.get(this.chunks.size() - 3 - n)[0]) - OP_1 + 1;

                return m;
        }

        return 0;
    }

}
