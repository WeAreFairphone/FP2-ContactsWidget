package com.fairphone.mycontacts.utils;

public class Triple<F,S,T> {
	public F first;
	public S second;
	public T third;

	public static <F,S,T> Triple create(F f, S s, T t) {
		return new Triple(f, s, t);
	}

	private Triple(F f, S s, T t) {
		first = f;
		second = s;
		third = t;
	}

}
