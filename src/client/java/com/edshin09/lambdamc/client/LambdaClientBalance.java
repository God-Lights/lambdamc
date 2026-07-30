package com.edshin09.lambdamc.client;

/** Client-side cache of the local player's Lambda balance, kept in sync via network packets. */
public final class LambdaClientBalance {
	private static volatile long balance = 0L;
	private static volatile boolean known = false;

	private LambdaClientBalance() {
	}

	public static long get() {
		return balance;
	}

	public static boolean isKnown() {
		return known;
	}

	public static void set(long value) {
		balance = value;
		known = true;
	}

	public static void reset() {
		known = false;
		balance = 0L;
	}
}
