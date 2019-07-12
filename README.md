# SandHook
- Android ART Hook
- Native Inline Hook
## Version 

[ ![Version](https://api.bintray.com/packages/ganyao114/maven/hooklib/images/download.svg) ](https://bintray.com/ganyao114/maven/hooklib/_latestVersion)

## Chinese

[中文文档以及实现](https://github.com/ganyao114/SandHook/blob/master/doc/doc.md)

[中文 Blog](https://blog.csdn.net/ganyao939543405/article/details/86661040)  

QQ Group：756071167

# arch support 

- ARM64
- ARM32(no tested)
- Thumb-2

# OS

4.4(ART Runtime) - 10.0

# Project Struct

- annotation<br/>
annotation api
- hooklib<br/>
core lib of art hook
- nativehook<br/>
lib of native hook
- xposedcompat<br/>
stable implement of xposed api compat for sandhook
- xposedcompat_new<br/>
annother implement of xposed api compat for sandhook(hook more fast first time)
- hookers<br/>
hook plugin demo for annotation api

# how to use

```gradle
implementation 'com.swift.sandhook:hooklib:4.0.0'
```

## Annotation API

--------------------------------------------------------------------

- hook method must be a static method
- first par must be this if method is not static
- method description must "same"(can be isAssignableFrom) with origin method
- backup method same with above

```java
@HookClass(Activity.class)
//@HookReflectClass("android.app.Activity")
public class ActivityHooker {

    @HookMethodBackup("onCreate")
    @MethodParams(Bundle.class)
    static Method onCreateBackup;

    @HookMethodBackup("onPause")
    static HookWrapper.HookEntity onPauseBackup;

    @HookMethod("onCreate")
    @MethodParams(Bundle.class)
    public static void onCreate(Activity thiz, Bundle bundle) throws Throwable {
        Log.e("ActivityHooker", "hooked onCreate success " + thiz);
        SandHook.callOriginByBackup(onCreateBackup, thiz, bundle);
    }

    @HookMethod("onPause")
    public static void onPause(@ThisObject Activity thiz) throws Throwable {
        Log.e("ActivityHooker", "hooked onPause success " + thiz);
        onPauseBackup.callOrigin(thiz);
    }

}



//or like this:

@HookClass(TestClass.class)
public class NewAnnotationApiHooker {

    @HookMethod("testNewHookApi")
    public static void onTestNewHookApi(@ThisObject TestClass thiz, @Param("com.swift.sandhook.MainActivity") Activity activity, int a) {
        Log.e("TestClassHook", "testNewHookApi been hooked");
        onTestNewHookApiBackup(thiz, activity, a);
    }

    @HookMethodBackup("testNewHookApi")
    public static void onTestNewHookApiBackup(@ThisObject TestClass thiz, @Param("com.swift.sandhook.MainActivity") Activity activity, int a) {
        onTestNewHookApiBackup(thiz, activity, a);
    }

}



//first set debuggable
SandHookConfig.DEBUG = BuildConfig.DEBUG;

and

//add hookers
SandHook.addHookClass(CtrHook.class, LogHooker.class, CustmizeHooker.class, ActivityHooker.class, ObjectHooker.class);

you can also use:
SanHook.public static boolean hook(Member target, Method hook, Method backup) {}

```

if hookers is in plugin(like xposed):  

```groovy
provided 'com.swift.sandhook:hookannotation:4.0.0'
```
  
in your plugin

if OS <= 5.1 
backup method can call itself to avoid be inlining

## Xposed API

--------------------------------------------------------------------

Now you can use Xposed api:

We have two different implements:
```groovy
//stable
implementation 'com.swift.sandhook:xposedcompat:4.0.0'

//or

//hook fast first time
implementation 'com.swift.sandhook:xposedcompat_new:4.0.0'
```

```java

//setup for xposed
//for xposed compat only(no need xposed comapt new)
XposedCompat.cacheDir = getCacheDir();

//for load xp module(sandvxp)
XposedCompat.context = this;
XposedCompat.classLoader = getClassLoader();
XposedCompat.isFirstApplication= true;

//do hook
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

```

# Notice

## Call Origin

!!!!!!!!  

when OS >= 8.0
you must call backup method in hook method, if you want call it in other method, please call  SandHook.compileMethod(otherMethod) before call backup method.
    
because when ART trigger JIT from profiling, JIT will invoke -> ResolveCompilingMethodsClass -> ClassLinker::ResolveMethod -> CheckIncompatibleClassChange -> ThrowIncompatibleClassChangeError finally!!!


## Disable Inline

### JIT inline

We can do nothing to prevent some methods been inlined before app start, but we can try to disable VM Jit Inline after launch.

if you will hook some method that could be inlined, please call SandHook.disableVMInline()(OS >= 7.0) in Application.OnCreate()

### Inline by dex2oat

#### Background dex2oat

SandHook.tryDisableProfile(getPackageName());

#### dex2oat by DexClassLoader

SandHook.disableDex2oatInline(fullyDisableDex2oat);

or

ArtDexOptimizer.dexoatAndDisableInline to dex2oat manuly 

### Deoptimize(Boot Image)

You can also deoptimize a caller that inlined your hook method by SandHook.deCompile(caller), just implement >= 7.0

## Hidden API

SandHook.passApiCheck();

To bypass hidden api on P & Q

# Native Hook

## simple hook(no backup)
#include "includes/sandhook.h"  

bool nativeHookNoBackup(void* origin, void* hook);

## need backup origin method
#include "sanhook_native.h"  

void* SandInlineHook(void* origin, void* replace);  

void* SandInlineHookSym(const char* so, const char* symb, void* replace);  


return is backup method

## break point

you can insert a break point in body of method(not only start of method), so you can read/write registers in break point.  


bool SandBreakpoint(void* origin, void (*callback)(REG[]));

## more

- disassembler (only implement important instructions)
- assembler (only implement important instructions)

# Demo

## SandVXPosed

non-Root Xposed Environment Demo (VirtualApp with SandHook):

https://github.com/ganyao114/SandVXposed

## EdXposed(SandHook Brunch)

Unofficial xposed framework >= 8.0

See release above

https://github.com/ElderDrivers/EdXposed

# Android Q(10.0)

in MyApp.java

//if you want test Android Q, please set true, because SDK_INT of Android Q is still 28
public final static boolean testAndroidQ = false;
 

# References

- Epic:https://github.com/tiann/epic
- Yahfa:https://github.com/rk700/YAHFA


