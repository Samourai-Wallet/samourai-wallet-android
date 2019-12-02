package com.samourai.wallet.util;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class VouchersTest {

    @Test
    public void testFastBitcoinsCode() {

        Assert.assertTrue(VouchersUtil.getInstance().isValidFastBitcoinsCode("QX86F4A8BAH5"));

        Assert.assertFalse(VouchersUtil.getInstance().isValidFastBitcoinsCode("QX986F4A8BAH5"));
        Assert.assertFalse(VouchersUtil.getInstance().isValidFastBitcoinsCode("Q286F4A8BAH5"));
        Assert.assertFalse(VouchersUtil.getInstance().isValidFastBitcoinsCode("QX86F4A8bAH5"));
        Assert.assertFalse(VouchersUtil.getInstance().isValidFastBitcoinsCode("QX-86F4A8BAH5"));
    }

}
