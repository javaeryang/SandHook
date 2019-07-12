package com.swift.sandhook;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.util.Log;

import com.swift.sandhook.nativehook.NativeHook;
import com.swift.sandhook.test.TestClass;
import com.swift.sandhook.testHookers.ActivityHooker;
import com.swift.sandhook.testHookers.CtrHook;
import com.swift.sandhook.testHookers.CustmizeHooker;
import com.swift.sandhook.testHookers.JniHooker;
import com.swift.sandhook.testHookers.LogHooker;
import com.swift.sandhook.testHookers.NewAnnotationApiHooker;
import com.swift.sandhook.testHookers.ObjectHooker;
import com.swift.sandhook.wrapper.HookErrorException;
import com.swift.sandhook.xposedcompat.XposedCompat;

import dalvik.system.DexClassLoader;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class MyApp extends Application {

    //if you want test Android Q, please Set true, because SDK_INT of Android Q is still 28
    public final static boolean testAndroidQ = false;

    @Override
    public void onCreate() {
        super.onCreate();


        SandHookConfig.DEBUG = BuildConfig.DEBUG;

        if (testAndroidQ) {
            SandHookConfig.SDK_INT = 29;
        }

        SandHook.disableVMInline();
        SandHook.tryDisableProfile(getPackageName());
        SandHook.disableDex2oatInline(false);

        if (SandHookConfig.SDK_INT >= Build.VERSION_CODES.P) {
            SandHook.passApiCheck();
        }

        try {
            SandHook.addHookClass(JniHooker.class,
                    CtrHook.class,
                    LogHooker.class,
                    CustmizeHooker.class,
                    ActivityHooker.class,
                    ObjectHooker.class,
                    NewAnnotationApiHooker.class);
        } catch (HookErrorException e) {
            e.printStackTrace();
        }

        //for xposed compat(no need xposed comapt new)
        XposedCompat.cacheDir = getCacheDir();


        //for load xp module(sandvxp)
        XposedCompat.context = this;
        XposedCompat.classLoader = getClassLoader();
        XposedCompat.isFirstApplication= true;

        XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Log.e("XposedCompat", "beforeHookedMethod: " + param.method.getName());
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Log.e("XposedCompat", "afterHookedMethod: " + param.method.getName());
            }
        });


        XposedHelpers.findAndHookMethod(MainActivity.class, "testStub", TestClass.class, int.class, String.class, boolean.class, char.class, String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                param.args[1] = 2;
                Log.e("XposedCompat", "beforeHookedMethod: " + param.method.getName());
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Log.e("XposedCompat", "afterHookedMethod: " + param.method.getName());
            }
        });

        try {
            ClassLoader classLoader = getClassLoader();
            DexClassLoader dexClassLoader = new DexClassLoader("/sdcard/hookers-debug.apk",
                    getCacheDir().getAbsolutePath(), null, classLoader);
            Class absHookerClass = Class.forName("com.swift.sandhook.hookers.AbsHooker", true, dexClassLoader);
            Class pluginHookerClass = Class.forName("com.swift.sandhook.hookers.PluginHooker", true, dexClassLoader);
            SandHook.addHookClass(getClassLoader(), absHookerClass, pluginHookerClass);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (HookErrorException e) {
            e.printStackTrace();
        }

    }
}
