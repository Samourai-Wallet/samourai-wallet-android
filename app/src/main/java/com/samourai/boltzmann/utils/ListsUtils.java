package com.samourai.boltzmann.utils;

import java.util.*;

public class ListsUtils {

    /**
     * Checks if sets from a list of sets share common elements
     * and merge sets when common elements are detected
     * @param sets list of sets
     * @return Returns the list with merged sets.
     */
    public static List<Set<String>> mergeSets(Collection<Set<String>> sets) {
        LinkedList<Set<String>> tmp_sets = new LinkedList<>(sets);
        boolean merged = true;
        while(merged) {
            merged = false;
            LinkedList<Set<String>> res = new LinkedList<>();
            while (!tmp_sets.isEmpty()) {
                Set<String> current = tmp_sets.poll();
                LinkedList<Set<String>> rest = tmp_sets;
                tmp_sets = new LinkedList<>();
                for (Set<String> x : rest) {
                    if(Collections.disjoint(current, x)) {
                        tmp_sets.add(x);
                    }
                    else {
                        merged = true;
                        current.addAll(x);
                    }
                }
                res.add(current);
            }
            tmp_sets = res;
        }
        return tmp_sets;
    }

    public static int[][] powerSet(Integer[] a) {
        int max = 1 << a.length;
        int[][] result = new int[max][];
        for (int i = 0; i < max; ++i) {
            result[i] = new int[Integer.bitCount(i)];
            for (int j = 0, b = i, k = 0; j < a.length; ++j, b >>= 1)
                if ((b & 1) != 0)
                    result[i][k++] = a[j];
        }
        return result;
    }
}
