package com.monst.bankingplugin.utils;

public abstract class Triple<T, K, R> {

    private T first;
    private K second;
    private R third;

    public Triple(T t, K k, R r) {
        this.first = t;
        this.second = k;
        this.third = r;
    }

    protected T getFirst() {
        return first;
    }

    protected void setFirst(T first) {
        this.first = first;
    }

    protected K getSecond() {
        return second;
    }

    protected void setSecond(K second) {
        this.second = second;
    }

    protected R getThird() {
        return third;
    }

    protected void setThird(R third) {
        this.third = third;
    }

    @Override
    public String toString() {
        return String.format("(%s, %s, %s)", first, second, third);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (getClass() != o.getClass())
            return false;
        Triple<?, ?, ?> other = (Triple<?, ?, ?>) o;
        return first.equals(other.first) && second.equals(other.second) && third.equals(other.third);
    }

    @Override
    public int hashCode() {
        return (((first.hashCode() * 31) + second.hashCode()) * 31) + third.hashCode() * 31;
    }
}
