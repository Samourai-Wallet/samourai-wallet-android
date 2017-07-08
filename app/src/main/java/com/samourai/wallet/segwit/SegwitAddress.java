package com.samourai.wallet.segwit;

import com.samourai.wallet.bech32.Bech32;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.ArrayUtils;

public class SegwitAddress {

    public static Pair<Byte, byte[]> decode(String hrp, String addr) throws Exception {

        Pair<byte[], byte[]> p = Bech32.bech32Decode(addr);

        byte[] hrpgot = p.getLeft();
        String hrpgotStr =  new String(hrpgot);
        if (!hrp.equals(hrpgotStr))    {
            throw new Exception("mismatching bech32 human readeable part");
        }
        if (!hrpgotStr.equalsIgnoreCase("bc") && !hrpgotStr.equalsIgnoreCase("tb"))    {
            throw new Exception("invalid segwit human readable part");
        }

        byte[] data = p.getRight();
        byte[] decoded = convertBits(Arrays.asList(ArrayUtils.toObject(Arrays.copyOfRange(data, 1, data.length))), 5, 8, false);
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

    public static String encode(byte[] hrp, byte witnessVersion, byte[] witnessProgram) throws Exception    {

        byte[] prog = convertBits(Arrays.asList(ArrayUtils.toObject(witnessProgram)), 8, 5, true);
        byte[] data = new byte[1 + prog.length];

        System.arraycopy(new byte[] { witnessVersion }, 0, data, 0, 1);
        System.arraycopy(prog, 0, data, 1, prog.length);

        String ret = Bech32.bech32Encode(hrp, data);

        assert(Arrays.equals(data, decode(new String(hrp), ret).getRight()));

        return ret;
    }

    private static byte[] convertBits(List<Byte> data, int fromBits, int toBits, boolean pad) throws Exception    {

        int acc = 0;
        int bits = 0;
        int maxv = (1 << toBits) - 1;
        List<Byte> ret = new ArrayList<Byte>();

        for(Byte value : data)  {

            short b = (short)(value.byteValue() & 0xff);

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
                ret.add((byte)((acc >> bits) & maxv));
            }
        }

        if(pad && (bits > 0))    {
            ret.add((byte)((acc << (toBits - bits)) & maxv));
        }
        else if (bits >= fromBits || (byte)(((acc << (toBits - bits)) & maxv)) != 0)    {
            throw new Exception("panic");
        }
        else    {
            ;
        }

        return ArrayUtils.toPrimitive(ret.toArray(new Byte[ret.size()]));
    }

    public static byte[] getScriptPubkey(byte witver, byte[] witprog) {

        byte v = (witver > 0) ? (byte)(witver + 0x80) : (byte)0;
        byte[] ver = new byte[] { v, (byte)witprog.length };

        byte[] ret = new byte[witprog.length + ver.length];
        System.arraycopy(ver, 0, ret, 0, ver.length);
        System.arraycopy(witprog, 0, ret, ver.length, witprog.length);

        return ret;
    }

}
