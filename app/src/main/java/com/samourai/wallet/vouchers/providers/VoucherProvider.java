package com.samourai.wallet.vouchers.providers;

import io.reactivex.Single;

public abstract class VoucherProvider {

    public abstract Single<ValidateResponse> validate(String email, String voucher);

    public abstract Single<Boolean> redeem(ValidateResponse response,String address);

    public abstract String getProviderName();

}
