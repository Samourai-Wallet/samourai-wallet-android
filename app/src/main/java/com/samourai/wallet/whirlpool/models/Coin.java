package com.samourai.wallet.whirlpool.models;

public class Coin {
    private String address = "";
    private Float value = 0F;
    //States for recycler view
    private Boolean isSelected = false, blocked = false;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Float getValue() {
        return value;
    }

    public void setValue(Float value) {
        this.value = value;
    }

    public Boolean getSelected() {
        return isSelected;
    }

    public Coin setSelected(Boolean selected) {
        isSelected = selected;
        return this;
    }

    @Override
    public String toString() {
        return this.getAddress().concat("\t");
    }

    public Boolean getBlocked() {
        return blocked;
    }

    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }

}
