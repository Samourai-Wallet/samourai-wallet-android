package com.samourai.wallet.segwit.bech32;

import org.apache.commons.lang3.tuple.Pair;

public class Bech32Segwit {

    public static Pair<Byte, byte[]> decode(String hrp, String addr) throws Exception {

        Pair<String, byte[]> p = Bech32.bech32Decode(addr);

        String hrpgotStr =  p.getLeft();
        if(hrpgotStr == null)  {
            return null;
        }
        if (!hrp.equalsIgnoreCase(hrpgotStr))    {
            return null;
        }
        if (!hrpgotStr.equalsIgnoreCase("bc") && !hrpgotStr.equalsIgnoreCase("tb"))    {
            throw new Exception("invalid segwit human readable part");
        }

        byte[] data = p.getRight();
        byte[] decoded = new byte[getOutSize(data.length - 1, 5, 8, false)];
        convertBits(data, 1, decoded, 0, 5, 8, false);
        if(decoded.length < 2 || decoded.length > 40)   {
            throw new Exception("invalid decoded data length");
        }

        byte witnessVersion = data[0];
        if (witnessVersion > 16)   {
            throw new Exception("invalid decoded witness version");
        }

        if (witnessVersion == 0 && decoded.length != 20 && decoded.length != 32)   {
            throw new Exception("decoded witness version 0 with unknown length");
        }

        return Pair.of(witnessVersion, decoded);
    }

    public static String encode(String hrp, byte witnessVersion, byte[] witnessProgram) throws Exception    {

        byte[] data = new byte[1 + getOutSize(witnessProgram.length, 8, 5, true)];
        data[0] = witnessVersion;
        convertBits(witnessProgram, 0, data, 1, 8, 5, true);

        String ret = Bech32.bech32Encode(hrp, data);

        return ret;
    }

    private static int getOutSize(int dataSize, int fromBits, int toBits, boolean pad) {
        int outSize = dataSize * fromBits / toBits;
        if (pad && outSize * toBits < dataSize * fromBits) {
            outSize++;
        }
        return outSize;
    }

    private static void convertBits(byte[] src, int srcIdx, byte[] dest, int destIdx,
                                    int fromBits, int toBits, boolean pad) throws Exception    {

        int acc = 0;
        int bits = 0;
        int maxv = (1 << toBits) - 1;

        for(; srcIdx < src.length; srcIdx++) {
            short b = (short)(src[srcIdx] & 0xff);

            if (b < 0) {
                throw new Exception();
            }
            else if ((b >> fromBits) > 0) {
                throw new Exception();
            }
            else    {
                ;
            }

            acc = (acc << fromBits) | b;
            bits += fromBits;
            while (bits >= toBits)  {
                bits -= toBits;
                dest[destIdx++] = (byte)((acc >> bits) & maxv);
            }
        }

        if(pad && (bits > 0))    {
            dest[destIdx++] = (byte)((acc << (toBits - bits)) & maxv);
        }
        else if (bits >= fromBits || (byte)(((acc << (toBits - bits)) & maxv)) != 0)    {
            throw new Exception();
        }
        else    {
            ;
        }
    }

    public static byte[] getScriptPubkey(byte witver, byte[] witprog) {

        byte v = (witver > 0) ? (byte)(witver + 0x50) : (byte)0;
        byte[] ver = new byte[] { v, (byte)witprog.length };

        byte[] ret = new byte[witprog.length + ver.length];
        System.arraycopy(ver, 0, ret, 0, ver.length);
        System.arraycopy(witprog, 0, ret, ver.length, witprog.length);

        return ret;
    }

}
