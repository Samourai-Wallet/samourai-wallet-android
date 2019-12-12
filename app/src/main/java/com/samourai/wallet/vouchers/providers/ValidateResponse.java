package com.samourai.wallet.vouchers.providers;

public class ValidateResponse {
    private Long amount;
    private int quotationId;
    private String quotationSecret;
    private String voucher;
    private String email;


    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public int getQuotationId() {
        return quotationId;
    }

    public void setQuotationId(int quotationId) {
        this.quotationId = quotationId;
    }

    public String getQuotationSecret() {
        return quotationSecret;
    }

    public void setQuotationSecret(String quotationSecret) {
        this.quotationSecret = quotationSecret;
    }

    public String getVoucher() {
        return voucher;
    }

    public void setVoucher(String voucher) {
        this.voucher = voucher;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
