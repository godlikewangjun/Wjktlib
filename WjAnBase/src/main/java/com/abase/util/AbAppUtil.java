/*
 * Copyright (C) 2012 www.amsoft.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.abase.util;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Context.TELEPHONY_SERVICE;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 *
 */
public class AbAppUtil {

    public static List<String[]> mProcessList = null;

    /**
     * ??????????????????????????????.
     *
     * @param context the context
     * @param file    apk????????????
     */
    @RequiresPermission(Manifest.permission.REQUEST_INSTALL_PACKAGES)
    public static void installApk(Context context, File file) {
        //???????????????8.0,8.0??????????????????????????????????????????,??????????????????
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName()+".fileProvider", file);
                intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            } else {
                intent.setDataAndType(Uri.fromFile(file),
                        "application/vnd.android.package-archive");
            }
//            intent.setClassName("com.android.packageinstaller", "com.android.packageinstaller.PackageInstallerActivity");
        } catch (Exception e) {
            e.printStackTrace();
        }
        context.startActivity(intent);
    }


    /**
     * ?????????????????????.
     *
     * @param context     the context
     * @param packageName ??????
     */
    public static void uninstallApk(Context context, String packageName) {
        Intent intent = new Intent(Intent.ACTION_DELETE);
        Uri packageURI = Uri.parse("package:" + packageName);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.setData(packageURI);
        context.startActivity(intent);
    }

    /**
     * ????????????
     *
     * @param context
     */
    public static void onClearMemory(Context context) {
        ActivityManager activityManger = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> list = activityManger
                .getRunningAppProcesses();
        if (list != null)
            for (int i = 0; i < list.size(); i++) {
                ActivityManager.RunningAppProcessInfo apinfo = list.get(i);
                String[] pkgList = apinfo.pkgList;
                if (apinfo.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (int j = 0; j < pkgList.length; j++) {
                        if (pkgList[j].equals(context.getPackageName())) {
                            continue;
                        }
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
                            activityManger.restartPackage(pkgList[j]);
                        } else {
                            activityManger.killBackgroundProcesses(pkgList[j]);
                        }
                    }
                }
            }
    }

    /**
     * ??????????????????????????????.
     *
     * @param context   the context
     * @param className ????????????????????? "com.xxx.xx..XXXService"
     * @return true ????????? false ????????????
     */
    public static boolean isServiceRunning(Context context, String className) {
        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        List<RunningServiceInfo> servicesList = activityManager.getRunningServices(Integer.MAX_VALUE);
        Iterator<RunningServiceInfo> l = servicesList.iterator();
        while (l.hasNext()) {
            RunningServiceInfo si = (RunningServiceInfo) l.next();
            if (className.equals(si.service.getClassName())) {
                isRunning = true;
            }
        }
        return isRunning;
    }

    /**
     * ??????????????????
     *
     * @param packageName ??????
     * @param context
     */
    public static void openApp(Context context, String packageName) {
        Intent intent = new Intent();
        PackageManager packageManager = context.getPackageManager();
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent = packageManager.getLaunchIntentForPackage(packageName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }


    /**
     * ????????????.
     *
     * @param context   the context
     * @param className the class name
     * @return true, if successful
     */
    public static boolean stopRunningService(Context context, String className) {
        Intent intent_service = null;
        boolean ret = false;
        try {
            intent_service = new Intent(context, Class.forName(className));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (intent_service != null) {
            ret = context.stopService(intent_service);
        }
        return ret;
    }


    /**
     * Gets the number of cores available in this device, across all processors.
     * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
     *
     * @return The number of cores, or 1 if failed to get result
     */
    public static int getNumCores() {
        try {
            //Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            //Filter to only list the devices we care about
            File[] files = dir.listFiles(new FileFilter() {

                @Override
                public boolean accept(File pathname) {
                    //Check if filename is "cpu", followed by a single digit number
                    if (Pattern.matches("cpu[0-9]", pathname.getName())) {
                        return true;
                    }
                    return false;
                }

            });
            //Return the number of cores (virtual CPU devices)
            return files.length;
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }


    /**
     * ?????????????????????????????????.
     *
     * @param context the context
     * @return true, if is network available
     */
    public static boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager connectivity = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity != null) {
                NetworkInfo info = connectivity.getActiveNetworkInfo();
                if (info != null && info.isConnected()) {
                    if (info.getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    /**
     * Gps????????????
     * ??????<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />??????
     *
     * @param context the context
     * @return true, if is gps enabled
     */
    public static boolean isGpsEnabled(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }


    /**
     * ?????????????????????????????????????????????.
     *
     * @param context the context
     * @return boolean
     */
    public static boolean isMobile(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null
                && activeNetInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
            return true;
        }
        return false;
    }

    /**
     * ???????????????.
     *
     * @param context the context
     * @param dbName  the db name
     * @param rawRes  the raw res
     * @return true, if successful
     */
    public static boolean importDatabase(Context context, String dbName, int rawRes) {
        int buffer_size = 1024;
        InputStream is = null;
        FileOutputStream fos = null;
        boolean flag = false;

        try {
            String dbPath = "/data/data/" + context.getPackageName() + "/databases/" + dbName;
            File dbfile = new File(dbPath);
            //?????????????????????????????????????????????????????????????????????????????????????????????
            if (!dbfile.exists()) {
                //?????????????????????
                if (!dbfile.getParentFile().exists()) {
                    dbfile.getParentFile().mkdirs();
                }
                dbfile.createNewFile();
                is = context.getResources().openRawResource(rawRes);
                fos = new FileOutputStream(dbfile);
                byte[] buffer = new byte[buffer_size];
                int count = 0;
                while ((count = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
                fos.flush();
            }
            flag = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
        }
        return flag;
    }

    /**
     * ???????????????????????????.
     *
     * @param context the context
     * @return mDisplayMetrics
     */
    public static DisplayMetrics getDisplayMetrics(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay()
                .getMetrics(displayMetrics);
        return displayMetrics;
    }

    /**
     * ????????????.
     *
     * @param context the context
     */
    public static void showSoftInput(Activity context) {
        try {
            View view = context.getWindow().getCurrentFocus();
            if (view != null) {
                InputMethodManager inputmanger = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputmanger.showSoftInput(view, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ????????????.
     */
    public static void showSoftInput(Activity activity, View view) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
    }

    /**
     * ??????????????????.
     *
     * @param context the context
     */
    public static void closeSoftInput(Activity context) {
        try {
            View view = context.getWindow().getCurrentFocus();
            if (view != null) {
                InputMethodManager inputmanger = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputmanger.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ??????????????????.
     */
    public static void closeSoftInput(Activity activity, View view) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * ???????????????.
     *
     * @param context the context
     */
    public static PackageInfo getPackageInfo(Context context) {
        PackageInfo info = null;
        try {
            String packageName = context.getPackageName();
            info = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return info;
    }


    /**
     * ??????????????????????????????????????????.
     *
     * @param context
     * @param processName
     * @return
     */
    public static ApplicationInfo getApplicationInfo(Context context, String processName) {
        if (processName == null) {
            return null;
        }

        PackageManager packageManager = context.getApplicationContext().getPackageManager();
        List<ApplicationInfo> appList = packageManager.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
        for (ApplicationInfo appInfo : appList) {
            if (processName.equals(appInfo.processName)) {
                return appInfo;
            }
        }
        return null;
    }

    /**
     * ?????????kill??????.
     *
     * @param context
     * @param pid
     */
    public static void killProcesses(Context context, int pid, String processName) {
        /*String cmd = "kill -9 "+pid;
        Process process = null;
	    DataOutputStream os = null;
    	try {
			process = Runtime.getRuntime().exec("su"); 
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes(cmd + "\n");
			os.writeBytes("exit\n");
			os.flush();
			process.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}
    	AbLogUtil.d(AbAppUtil.class, "#kill -9 "+pid);*/

        ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        String packageName = null;
        try {
            if (processName.indexOf(":") == -1) {
                packageName = processName;
            } else {
                packageName = processName.split(":")[0];
            }

            activityManager.killBackgroundProcesses(packageName);

            //
            Method forceStopPackage = activityManager.getClass().getDeclaredMethod("forceStopPackage", String.class);
            forceStopPackage.setAccessible(true);
            forceStopPackage.invoke(activityManager, packageName);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * ?????????????????????.
     *
     * @param command
     * @param workdirectory
     * @return
     */
    public static String runCommand(String[] command, String workdirectory) {
        String result = "";
        AbLogUtil.d(AbAppUtil.class, "#" + command);
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            // set working directory
            if (workdirectory != null) {
                builder.directory(new File(workdirectory));
            }
            builder.redirectErrorStream(true);
            Process process = builder.start();
            InputStream in = process.getInputStream();
            byte[] buffer = new byte[1024];
            while (in.read(buffer) != -1) {
                String str = new String(buffer);
                result = result + str;
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * ?????????????????????.
     *
     * @param script
     * @return
     */
    public static String runScript(String script) {
        String sRet = "";
        try {
            final Process m_process = Runtime.getRuntime().exec(script);
            final StringBuilder sbread = new StringBuilder();
            Thread tout = new Thread(new Runnable() {
                public void run() {
                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(m_process.getInputStream()),
                            8192);
                    String ls_1 = null;
                    try {
                        while ((ls_1 = bufferedReader.readLine()) != null) {
                            sbread.append(ls_1).append("\n");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            bufferedReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            tout.start();

            final StringBuilder sberr = new StringBuilder();
            Thread terr = new Thread(new Runnable() {
                public void run() {
                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(m_process.getErrorStream()),
                            8192);
                    String ls_1 = null;
                    try {
                        while ((ls_1 = bufferedReader.readLine()) != null) {
                            sberr.append(ls_1).append("\n");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            bufferedReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            terr.start();

            int retvalue = m_process.waitFor();
            while (tout.isAlive()) {
                Thread.sleep(50);
            }
            if (terr.isAlive())
                terr.interrupt();
            String stdout = sbread.toString();
            String stderr = sberr.toString();
            sRet = stdout + stderr;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return sRet;
    }

    /**
     * ?????????????????????????????? Root??????????????????????????????(??????ROOT??????)
     *
     * @return ???????????????/?????????Root??????
     */
    public static boolean getRootPermission(Context context) {
        String packageCodePath = context.getPackageCodePath();
        Process process = null;
        DataOutputStream os = null;
        try {
            String cmd = "chmod 777 " + packageCodePath;
            //?????????root??????
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * ????????????????????????????????????.
     *
     * @return
     */
    public static List<String[]> getProcessRunningInfo() {
        List<String[]> processList = null;
        try {
            String result = runCommandTopN1();
            processList = parseProcessRunningInfo(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return processList;
    }

    /**
     * ?????????top -n 1.
     *
     * @return
     */
    public static String runCommandTopN1() {
        String result = null;
        try {
            String[] args = {"/system/bin/top", "-n", "1"};
            result = runCommand(args, "/system/bin/");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * ?????????????????????.
     *
     * @param info User 39%, System 17%, IOW 3%, IRQ 0%
     *             PID    PR CPU% S   #THR     VSS     RSS    PCY    UID        Name
     *             31587  0  39%  S    14    542288K  42272K  fg   u0_a162  cn.amsoft.process
     *             313    1  17%  S    12    68620K   11328K  fg   system   /system/bin/surfaceflinger
     *             32076  1   2%  R     1    1304K    604K    bg   u0_a162  /system/bin/top
     * @return
     */
    public static List<String[]> parseProcessRunningInfo(String info) {
        List<String[]> processList = new ArrayList<String[]>();
        int Length_ProcStat = 10;
        String tempString = "";
        boolean bIsProcInfo = false;
        String[] rows = null;
        String[] columns = null;
        rows = info.split("[\n]+");
        // ????????????????????????????????????
        for (int i = 0; i < rows.length; i++) {
            tempString = rows[i];
            //AbLogUtil.d(AbAppUtil.class, tempString);
            if (tempString.indexOf("PID") == -1) {
                if (bIsProcInfo == true) {
                    tempString = tempString.trim();
                    columns = tempString.split("[ ]+");
                    if (columns.length == Length_ProcStat) {
                        //???/system/bin/?????????
                        if (columns[9].startsWith("/system/bin/")) {
                            continue;
                        }
                        //AbLogUtil.d(AbAppUtil.class, "#"+columns[9]+",PID:"+columns[0]);
                        processList.add(columns);
                    }
                }
            } else {
                bIsProcInfo = true;
            }
        }
        return processList;
    }

    /**
     * /**
     * <p>
     * ???????????????????????????.
     *
     * @param context
     * @return
     */
    public static long getAvailMemory(Context context) {
        //??????android????????????????????????  
        ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        MemoryInfo memoryInfo = new MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        //???????????????????????? ,?????????????????????????????????  
        return memoryInfo.availMem;
    }

    /**
     * ??????????????????.
     *
     * @param context
     * @return
     */
    public static long getTotalMemory(Context context) {
        //????????????????????????
        String file = "/proc/meminfo";
        String memInfo;
        String[] strs;
        long memory = 0;

        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader, 8192);
            //??????meminfo?????????????????????????????? 
            memInfo = bufferedReader.readLine();
            strs = memInfo.split("\\s+");
            for (String str : strs) {
                AbLogUtil.d(AbAppUtil.class, str + "\t");
            }
            //??????????????????????????????KB  
            memory = Integer.valueOf(strs[1]).intValue() * 1024;
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //Byte??????KB???MB
        return memory;
    }

    /**
     * ???????????????????????????
     *
     * @param context ?????????
     * @param intent  intent??????activity
     * @return boolean true???????????????false????????????
     */
    public static boolean isRuning(Context context, String className) {
        Intent intent = new Intent();
        intent.setClassName(context.getPackageName(), className);
        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * ?????????????????????????????????
     *
     * @param context
     * @param packageName
     * @return
     */
    public static boolean startActivityByPackageName(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            if (pm != null) {
                Intent intent = pm.getLaunchIntentForPackage(packageName);
                if (intent != null) {
                    intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    return true;
                }
            }
        } catch (Throwable e) {
        }
        return false;
    }


    /**
     * ????????????????????????uri??????activity
     *
     * @param context
     * @param uri
     * @param flags
     * @param title
     * @return
     */
    public static boolean startActivityByUriWithChooser(Context context, String uri, int flags, String title) {
        try {
            Intent intent = Intent.parseUri(uri, flags);
            if (intent == null) {
                return false;
            }

            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            Intent startIntent = Intent.createChooser(intent, title);
            startIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(startIntent);
            return true;
        } catch (Throwable e) {
        }
        return false;
    }

    /**
     * ???????????????
     */
    private static StringBuilder deviceidStr = null;

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public static String getDeviceId(Context context) {
        if (context == null) {
            return "";
        }
//        if (!AbStrUtil.isEmpty(deviceidStr)) {
//            return deviceidStr;
//        }
//        File file = new File(AbFileUtil.getCacheDownloadDir(context), "dv");
//        if (file.exists()) {
//            deviceidStr = AbFileUtil.okReadFile(file);
//            return deviceidStr;
//        }
        try {
            //IMEI???imei???
            TelephonyManager tm = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
            if (tm == null) {
                return "";
            }
            //????????????
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                return "";
            }
            deviceidStr = new StringBuilder();
            ArrayList<String> strings = new ArrayList<>();
            TelephonyManager telephonyManager = (TelephonyManager) context
                    .getSystemService(TELEPHONY_SERVICE);

            addString(strings, tm.getDeviceId());
            try {
                Class clazz = telephonyManager.getClass();
                Method getImei = clazz.getDeclaredMethod("getImei", int.class);//(int slotId)

                //??????IMEI 1????????????
                String imei1 = (String) getImei.invoke(telephonyManager, 0);
                addString(strings, imei1);

                String imei2 = (String) getImei.invoke(telephonyManager, 1);
                addString(strings, imei2);

            } catch (NoSuchMethodException e) {
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
            CTelephoneInfo cTelephoneInfo = CTelephoneInfo.getInstance();
            cTelephoneInfo.setCTelephoneInfo(context);
            addString(strings, cTelephoneInfo.getImeiSIM1());
            addString(strings, cTelephoneInfo.getImeiSIM2());

            for (int i = 0; i < strings.size(); i++) {
                if (!AbStrUtil.isEmpty(strings.get(i))) {
                    if (i < strings.size() - 1) {
                        deviceidStr.append(strings.get(i)).append("|");
                    } else {
                        deviceidStr.append(strings.get(i));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return deviceidStr.toString();
    }


    /**
     * ??????IMEI
     *
     * @param strings
     * @param id
     */
    private static void addString(ArrayList<String> strings, String id) {
        if (id != null && !strings.contains(id) && (id.length() == 14 || id.length() == 15)) {
            strings.add(id);
        }
    }

    /**
     * ????????????
     */
    public static String getMeta(Context context, String channel) {

        try {
            String msg = "";
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            msg = appInfo.metaData.getString(channel);
            return msg;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }


    /**
     * ???????????????activity?????????top
     *
     * @param context
     * @param className null????????? ????????????????????????
     * @return
     */
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public static synchronized boolean isTopActivity(Context context, String className, boolean isPack) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED || className == null) {
            return false;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//????????????
                UsageStatsManager m = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
                if (m != null) {
                    long now = System.currentTimeMillis();
                    //??????60????????????????????????
                    UsageEvents usageEvents = m.queryEvents(now - 30 * 60 * 1000, now);
                    String topActivity = "";
                    String packName = "";
                    if (usageEvents != null) {
                        UsageEvents.Event event = new UsageEvents.Event();
                        while (usageEvents.hasNextEvent()) {
                            UsageEvents.Event eventAux = new UsageEvents.Event();
                            usageEvents.getNextEvent(eventAux);
                            if (eventAux.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                                event = eventAux;
                            }
                        }
                        topActivity = event.getClassName();
                        packName = event.getPackageName();
                    }
                    if (topActivity == null && packName == null) {
                        return false;
                    }
                    if (isPack) {//??????????????????
                        AbLogUtil.d(AbAppUtil.class, packName + "==????????????==" + className + "===" + packName.equals(className));
                        return packName.equals(className);
                    } else {
                        AbLogUtil.d(AbAppUtil.class, topActivity + "==??????==" + className + "===" + className.equals(topActivity));
                        return topActivity.contains(className);
                    }
                }
            } else {
                List<ActivityManager.RunningTaskInfo> rTasks = getRunningTask(context, 1);
                for (ActivityManager.RunningTaskInfo task : rTasks) {
                    if (isPack) {
                        if (task.topActivity.getPackageName().equals(className)) {
                            return true;
                        }
                    } else {
                        if (task.topActivity.getClassName().contains(className)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * ???????????????activity?????????top
     *
     * @param context
     * @param className null????????? ????????????????????????
     * @return
     */
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public static synchronized boolean haveActivity(Context context, String className, boolean isPack) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED || className == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {//????????????
            UsageStatsManager m = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            if (m != null) {
                long now = System.currentTimeMillis();
                //??????60????????????????????????
                UsageEvents usageEvents = m.queryEvents(now - 30 * 60 * 1000, now);
                String topActivity = "";
                String packName = "";
                if (usageEvents != null) {
                    while (usageEvents.hasNextEvent()) {
                        UsageEvents.Event eventAux = new UsageEvents.Event();
                        usageEvents.getNextEvent(eventAux);
                        if (isPack) {//??????????????????
                            if (eventAux.getPackageName().equals(className)) {
                                System.out.println(packName + "==????????????==" + className + "===" + eventAux.getPackageName().equals(className));
                                return true;
                            }
                        } else {
                            if (eventAux.getClassName().contains(className)) {
                                System.out.println(topActivity + "==??????==" + className + "===" + eventAux.getClassName().equals(topActivity));
                                return true;
                            }
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> rTasks = getRunningTask(context, 20);
            for (ActivityManager.RunningTaskInfo task : rTasks) {
                if (task.topActivity.getClassName().equals(className)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * ???????????????activity
     *
     * @param context
     * @param className
     * @return
     */
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public static String[] getTopActivity(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {//????????????
            UsageStatsManager m = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            if (m != null) {
                long now = System.currentTimeMillis();
                //??????60????????????????????????
                UsageEvents usageEvents = m.queryEvents(now - 60 * 60 * 1000, now);
                String topActivity = "";
                String packName = "";
                if (usageEvents != null) {
                    UsageEvents.Event event = new UsageEvents.Event();
                    while (usageEvents.hasNextEvent()) {
                        UsageEvents.Event eventAux = new UsageEvents.Event();
                        usageEvents.getNextEvent(eventAux);
                        if (eventAux.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                            event = eventAux;
                        }
                    }
                    topActivity = event.getClassName();
                    packName = event.getPackageName();
                }
                return new String[]{packName, topActivity};
            }
        } else {
            List<ActivityManager.RunningTaskInfo> rTasks = getRunningTask(context, 1);
            ActivityManager.RunningTaskInfo task = rTasks.get(0);
            return new String[]{task.topActivity.getPackageName(), task.topActivity.getClassName()};
        }
        return null;
    }

    /**
     * ??????activity?????????
     *
     * @param context
     * @param num
     * @return
     */
    public static List<ActivityManager.RunningTaskInfo> getRunningTask(Context context, int num) {
        if (context != null) {
            ActivityManager am = (ActivityManager) context
                    .getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> rTasks = am.getRunningTasks(num);
            return rTasks;
        }
        return null;
    }

    /**
     * ??????????????????
     *
     * @param context
     * @return
     */
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    public static String getDeviceInfo(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
        StringBuilder sb = new StringBuilder();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return "";
        }
        sb.append("\\" + tm.getDeviceSoftwareVersion());
        sb.append("\\" + tm.getLine1Number());
        sb.append("\\" + tm.getNetworkCountryIso());
        sb.append("\\" + tm.getNetworkOperator());
        sb.append("\\" + tm.getNetworkOperatorName());
        sb.append("\\" + tm.getNetworkType());
        sb.append("\\" + tm.getPhoneType());
        sb.append("\\" + tm.getSimCountryIso());
        sb.append("\\" + tm.getSimOperator());
        sb.append("\\" + tm.getSimOperatorName());
        sb.append("\\" + tm.getSimSerialNumber());
        sb.append("\\" + tm.getSimState());
        sb.append("\\" + tm.getSubscriberId());
        sb.append("\\" + tm.getVoiceMailNumber());
        return sb.toString();
    }

    /**
     * ?????????SIM
     *
     * @param context
     * @return
     */
    public static boolean isSiMDo(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        String simSer = tm.getSimSerialNumber();
        if (simSer == null || simSer.equals("")) {
            return false;
        }
        return true;
    }

    /**
     * ??????????????????apk
     * @param context
     * @param packageName
     * @return
     */
    public static boolean isInstallApk(Context context, String packageName) {
        if (packageName == null || "".equals(packageName))
            return false;
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName,
                    PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
