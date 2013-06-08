// The MIT License Header
// 
// Copyright (c) 2013 Tu Yimin
// 
// http://github.com/mmin18
// 
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
// 
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.github.mmin18.safelooper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;

/**
 * SafeLooper catches unexpected exceptions in Looper to avoid showing force
 * close dialog.
 * <p>
 * After call SafeLooper.install(), the current thread's looper will be take
 * over. Uncaught exceptions will be send to uncaughtExceptionHandler and the
 * looper will continues.<br>
 * <p>
 * Normally you will call SafeLooper.install() in main thread.
 * 
 * @author yimin.tu
 * 
 */
public class SafeLooper implements Runnable {
	private static final Object EXIT = new Object();
	private static final ThreadLocal<SafeLooper> RUNNINGS = new ThreadLocal<SafeLooper>();
	private static Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

	/**
	 * Install SafeLooper in the given Looper
	 * <p>
	 * Notice the action will take effect in the next event loop
	 */
	public static void install(Looper looper) {
		Handler handler = new Handler(looper);
		handler.removeMessages(0, EXIT);
		handler.post(new SafeLooper());
	}

	/**
	 * Install SafeLooper in the current thread
	 * <p>
	 * Notice the action will take effect in the next event loop
	 */
	public static void install() {
		Looper l = Looper.myLooper();
		if (l == null) {
			throw new IllegalStateException(
					"Looper not exists in current thread");
		}
		install(l);
	}

	/**
	 * Exit SafeLooper after millis in the given Looper
	 * <p>
	 * Notice the action will take effect in the next event loop
	 */
	public static void uninstallDelay(Looper looper, long millis) {
		Handler handler = new Handler(looper);
		handler.removeMessages(0, EXIT);
		handler.sendMessageDelayed(handler.obtainMessage(0, EXIT), millis);
	}

	/**
	 * Exit SafeLooper after millis in the current thread
	 * <p>
	 * Notice the action will take effect in the next event loop
	 */
	public static void uninstallDelay(long millis) {
		Looper l = Looper.myLooper();
		if (l == null) {
			throw new IllegalStateException(
					"Looper not exists in current thread");
		}
		uninstallDelay(l, millis);
	}

	/**
	 * Exit SafeLooper in the given Looper
	 * <p>
	 * Notice the action will take effect in the next event loop
	 */
	public static void uninstall(Looper looper) {
		uninstallDelay(looper, 0);
	}

	/**
	 * Exit SafeLooper in the current thread
	 * <p>
	 * Notice the action will take effect in the next event loop
	 */
	public static void uninstall() {
		Looper l = Looper.myLooper();
		if (l == null) {
			throw new IllegalStateException(
					"Looper not exists in current thread");
		}
		uninstall(l);
	}

	/**
	 * Tell if the SafeLooper is running in the current thread
	 */
	public static boolean isSafe() {
		return RUNNINGS.get() != null;
	}

	/**
	 * The same as Thread.setDefaultUncaughtExceptionHandler
	 */
	public static void setUncaughtExceptionHandler(
			Thread.UncaughtExceptionHandler h) {
		uncaughtExceptionHandler = h;
	}

	@Override
	public void run() {
		if (RUNNINGS.get() != null)
			return;

		Method next;
		Field target;
		try {
			Method m = MessageQueue.class.getDeclaredMethod("next");
			m.setAccessible(true);
			next = m;
			Field f = Message.class.getDeclaredField("target");
			f.setAccessible(true);
			target = f;
		} catch (Exception e) {
			return;
		}

		RUNNINGS.set(this);
		MessageQueue queue = Looper.myQueue();
		Binder.clearCallingIdentity();
		final long ident = Binder.clearCallingIdentity();

		while (true) {
			try {
				Message msg = (Message) next.invoke(queue);
				if (msg == null || msg.obj == EXIT)
					break;

				Handler h = (Handler) target.get(msg);
				h.dispatchMessage(msg);
				final long newIdent = Binder.clearCallingIdentity();
				if (newIdent != ident) {
				}
				msg.recycle();
			} catch (Exception e) {
				Thread.UncaughtExceptionHandler h = uncaughtExceptionHandler;
				Throwable ex = e;
				if (e instanceof InvocationTargetException) {
					ex = ((InvocationTargetException) e).getCause();
					if (ex == null) {
						ex = e;
					}
				}
				// e.printStackTrace(System.err);
				if (h != null) {
					h.uncaughtException(Thread.currentThread(), ex);
				}
				new Handler().post(this);
				break;
			}
		}

		RUNNINGS.set(null);
	}
}
