package com.jvoung;

import java.util.Objects;

/**
 * Pair of things.
 */
public class Pair<A, B> {
  public final A first;
  public final B second;

  public Pair(A a, B b) {
    first = a;
    second = b;
  }

  public static <A, B> Pair<A, B> of(A a, B b) {
    return new Pair<>(a, b);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (!(other instanceof Pair)) {
        return false;
    }
    Pair<?, ?> otherPair = (Pair)other;
    return Objects.equals(this.first, otherPair.first) && Objects.equals(this.second, otherPair.second);
  }

  @Override
  public int hashCode() {
    return Objects.hash(first, second);
  }

  @Override
  public String toString() {
    return first.toString() + ", " + second.toString();
  }

}
