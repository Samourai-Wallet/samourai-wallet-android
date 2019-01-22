package com.samourai.boltzmann.linker;

import java.util.AbstractMap;
import java.util.List;

class Pack {
        private String lbl;
        private PackType packType;
        private List<AbstractMap.SimpleEntry<String, Long>> ins;
        private List<String> outs;

        Pack(String lbl, PackType packType, List<AbstractMap.SimpleEntry<String, Long>> ins, List<String> outs) {
            this.lbl = lbl;
            this.packType = packType;
            this.ins = ins;
            this.outs = outs;
        }

        public String getLbl() {
            return lbl;
        }

        public PackType getPackType() {
            return packType;
        }

        public List<AbstractMap.SimpleEntry<String, Long>> getIns() {
            return ins;
        }

        public List<String> getOuts() {
            return outs;
        }
    }