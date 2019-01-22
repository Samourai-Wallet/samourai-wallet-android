package com.samourai.boltzmann.utils;

import java.util.*;
import java.util.Map.Entry;

public class ListsUtils {

  /**
   * Checks if sets from a list of sets share common elements and merge sets when common elements
   * are detected
   *
   * @param sets list of sets
   * @return Returns the list with merged sets.
   */
  public static List<Set<String>> mergeSets(Collection<Set<String>> sets) {
    LinkedList<Set<String>> tmp_sets = new LinkedList<Set<String>>(sets);
    boolean merged = true;
    while (merged) {
      merged = false;
      LinkedList<Set<String>> res = new LinkedList<Set<String>>();
      while (!tmp_sets.isEmpty()) {
        Set<String> current = tmp_sets.poll();
        LinkedList<Set<String>> rest = tmp_sets;
        tmp_sets = new LinkedList<Set<String>>();
        for (Set<String> x : rest) {
          if (Collections.disjoint(current, x)) {
            tmp_sets.add(x);
          } else {
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
        if ((b & 1) != 0) result[i][k++] = a[j];
    }
    return result;
  }

  public static <K, V extends Comparable<? super V>> Map<K, V> sortMap(
      Map<K, V> map, Comparator<Map.Entry<K, V>> comparator) {
    List<Entry<K, V>> list = new ArrayList<Entry<K, V>>(map.entrySet());
    Collections.sort(list, comparator);

    Map<K, V> result = new LinkedHashMap<K, V>();
    for (Entry<K, V> entry : list) {
      result.put(entry.getKey(), entry.getValue());
    }

    return result;
  }

  public static <K, V extends Comparable<? super V>>
      Comparator<Map.Entry<K, V>> comparingByValue() {
    return new Comparator<Map.Entry<K, V>>() {
      @Override
      public int compare(Entry<K, V> c1, Entry<K, V> c2) {
        return c1.getValue().compareTo(c2.getValue());
      }
    };
  }

  public static long[] toPrimitiveArray(Collection<Long> items) {
    long[] arr = new long[items.size()];
    Iterator<Long> iter = items.iterator();
    for (int i = 0; iter.hasNext(); i++) {
      arr[i] = iter.next();
    }
    return arr;
  }
}
