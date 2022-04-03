### ActivityManagerService 的作用

AMS 是运行在 SystemServer 进程的。

### AMS的启动流程



SystemServer 的 run 方法。

ActivityManagerService 属于启动引导服务。是由 系统服务进程 SystemServer 启动的。


SystemServer 的 startBootstrapServices 方法。

```java
private void startBootstrapServices() {
    
    //注释1处 
    mActivityManagerService = mSystemServiceManager.startService(ActivityManagerService.Lifecycle.class).getService();
    mActivityManagerService.setSystemServiceManager(mSystemServiceManager);
}
```
注释1处，`mSystemServiceManager.startService(ActivityManagerService.Lifecycle.class)` 返回的是 ActivityManagerService.Lifecycle 对象，
然后调用 Lifecycle 的 getService 方法返回 ActivityManagerService 对象。


SystemServiceManager 的 startService 方法。 会创建服务并启动服务。

```java
    public <T extends SystemService> T startService(Class<T> serviceClass) {
        try {
            final String name = serviceClass.getName();
            Slog.i(TAG, "Starting " + name);
            Trace.traceBegin(Trace.TRACE_TAG_SYSTEM_SERVER, "StartService " + name);

            // Create the service.
            if (!SystemService.class.isAssignableFrom(serviceClass)) {
                throw new RuntimeException("Failed to create " + name
                        + ": service must extend " + SystemService.class.getName());
            }
            final T service;
            try {
                Constructor<T> constructor = serviceClass.getConstructor(Context.class);
                service = constructor.newInstance(mContext);
            } 
            //...
            startService(service);
            return service;
        } finally {
            Trace.traceEnd(Trace.TRACE_TAG_SYSTEM_SERVER);
        }
    }

```

```java
public static final class Lifecycle extends SystemService {

        private final ActivityManagerService mService;

        public Lifecycle(Context context) {
            super(context);
            mService = new ActivityManagerService(context);
        }

        @Override
        public void onStart() {
            mService.start();
        }

        //

        public ActivityManagerService getService() {
            return mService;
        }
    }

```


### AMS 家族

#### ActivityManager


### ActivityStarter

用来解释如何启动Activity的控制器。ActivityStarter会收集所有的逻辑来决定一个 intent 和 flags 应该如何转化为一个 activity 和相关的任务和栈。



ActivityManager：提供有关 Activity 、Service 和包含进程的信息，并与之交互。大多数应用不需要使用该类。

### ActivityStackSupervisor


### ActivityStack

一系列活动的状态和管理。




* [Android解析ActivityManagerService（一）AMS启动流程和AMS家族](https://blog.csdn.net/itachi85/article/details/76405596)
