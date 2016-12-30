package com.samourai.wallet.util;

import java.security.SecureRandom;
import java.util.Arrays;

public class CharSequenceX implements CharSequence {

    private char[] chars;
    private int rounds = 10;

    private CharSequenceX(CharSequence charSequence, int start, int end) {
        zap();
        int len = end - start;
        chars = new char[len];
        for(int i = start; i < end; i++) {
            chars[i - start] = charSequence.charAt(i);
        }
    }

    public CharSequenceX(int len) {
        chars = new char[len];
    }

    public CharSequenceX(CharSequence charSequence) {
        this(charSequence, 0, charSequence.length());
    }

    public CharSequenceX(char[] chars) {
        zap();
        this.chars = chars;
    }

    public void zap() {
        if(chars != null) {
            for(int i = 0; i < rounds; i++) {
                fill('0');
                rfill();
                fill('0');
            }
        }
    }

    public void setRounds(int rounds) {
        if(rounds < 10) {
            this.rounds = 10;
        }
        else {
            this.rounds = rounds;
        }
    }

    @Override
    public char charAt(int index) {
        if(chars != null) {
            return chars[index];
        }
        else {
            return 0;
        }
    }

    @Override
    public int length() {
        if(chars != null) {
            return chars.length;
        }
        else {
            return 0;
        }
    }

    @Override
    public String toString() {
        return new String(chars);
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof CharSequenceX) {
            return Arrays.equals(chars, ((CharSequenceX) o).chars);
        }
        else {
            return false;
        }
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        CharSequenceX s = new CharSequenceX(this, start, end);
        return s;
    }

    protected void finalize() {
        zap();
    }

    private void fill(char c) {
        for(int i = 0; i < chars.length; i++) {
            chars[i] = c;
        }
    }

    private void rfill() {
        SecureRandom r = new SecureRandom();
        byte[] b = new byte[chars.length];
        r.nextBytes(b);
        for(int i = 0; i < chars.length; i++) {
            chars[i] = (char)b[i];
        }
    }

}