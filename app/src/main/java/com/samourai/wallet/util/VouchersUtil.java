package com.samourai.wallet.util;

public class VouchersUtil {

    private static VouchersUtil instance = null;

    private String fastBitcoinsAPI = "https://wallet-api.fastbitcoins.com/w-api/v1/samourai/";
    private static String fastBitcoinsEmail = "no.reply@vouchers.com";
    private static String fastBitcoinsRegex = "^[A-Za]{2}[A-Z0-9]{10}$";

    private VouchersUtil()   {
        ;
    }

    public static VouchersUtil getInstance() {

        if(instance == null) {
            instance = new VouchersUtil();
        }

        return instance;
    }

    public String getFastBitcoinsAPI() {
        return fastBitcoinsAPI;
    }

    public String getFastBitcoinsEmail() {
        return fastBitcoinsEmail;
    }

    public boolean isValidFastBitcoinsCode(String vcode) {

        if(vcode.matches(fastBitcoinsRegex)) {
            return true;
        }
        else {
            return false;
        }

    }

}
