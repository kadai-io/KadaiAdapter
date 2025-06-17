package io.kadai.adapter.util;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.TreeSet;

public class LowerMedian<T extends Comparable<T>> implements Collection<T> {

  private final TreeSet<T> sample = new TreeSet<>();
  private final long maxSampleSize;

  public LowerMedian(long maxSampleSize) {
    this.maxSampleSize = maxSampleSize;
  }

  @SuppressWarnings("unchecked")
  public Optional<T> get() {
    if (isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.ofNullable((T) sample.toArray()[(sample.size() - 1) / 2]);
    }
  }

  @Override
  public int size() {
    return sample.size();
  }

  @Override
  public boolean isEmpty() {
    return sample.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return sample.contains(o);
  }

  @Override
  @Nonnull
  public Iterator<T> iterator() {
    return sample.iterator();
  }

  @Override
  @Nonnull
  public Object[] toArray() {
    return sample.toArray();
  }

  @Override
  @Nonnull
  public <T1> T1[] toArray(@Nonnull T1[] a) {
    return sample.toArray(a);
  }

  @Override
  public boolean add(T datum) {
    if (datum == null) {
      throw new IllegalArgumentException("The given sample datum is null.");
    }
    if (sample.size() == maxSampleSize) {
      final T last = sample.last();
      if (last != null) {
        sample.remove(last);
      }
    }
    return sample.add(datum);
  }

  @Override
  public boolean remove(Object o) {
    return sample.remove(o);
  }

  @Override
  public boolean containsAll(@Nonnull Collection<?> c) {
    return sample.containsAll(c);
  }

  @Override
  public boolean addAll(@Nonnull Collection<? extends T> c) {
    return sample.addAll(c);
  }

  @Override
  public boolean removeAll(@Nonnull Collection<?> c) {
    return sample.removeAll(c);
  }

  @Override
  public boolean retainAll(@Nonnull Collection<?> c) {
    return sample.retainAll(c);
  }

  @Override
  public void clear() {
    sample.clear();
  }
}
