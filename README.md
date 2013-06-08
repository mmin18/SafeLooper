SafeLooper
==========

SafeLooper catches unexpected exceptions in Android applications to avoid showing force close dialog.

A normal Android application (except games) is driven by event loop, known as android.os.Looper. When you throw a uncaught exception in the main thread, the main thread's looper will stop and Android will show a force close dialog. You won't have a chance to recover the main thread's looper since it has already stopped.

If you can create a sub looper, pull message from event queue and process it with a try-catch block, you can catch the unexpected exceptions and avoid the application from crash.

Here is how a SafeLooper works:

![SafeLooper](https://raw.github.com/mmin18/SafeLooper/master/SafeLooper.png)

All you need to do is import SafeLooper.java into your project and call SafeLooper.install() when your application start.


	// install SafeLooper in main thread
	SafeLooper.install();
	
	// receive the uncaught exception if you don't want to ignore it
	SafeLooper.setUncaughtExceptionHandler(...);

You can also test the <https://raw.github.com/mmin18/SafeLooper/master/safelooper.apk> on your device.
