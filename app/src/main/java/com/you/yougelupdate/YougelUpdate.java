package com.you.yougelupdate;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static android.os.Environment.MEDIA_MOUNTED;

/**
 * Created by Administrator on 2016-11-08.
 */
public class YougelUpdate {
    private static final String TAG = YougelUpdate.class.getName();
    private static final String VERSIONCODE = "versionCode";
    private static final String VERSIONNAME = "versionName";
    private static final String DOWNLOADURL = "downloadUrl";
    private static final String DESCRIPTION = "description";
    private static final String EXTERNAL_STORAGE_PERMISSION = "android.permission.WRITE_EXTERNAL_STORAGE";
    private static final int NOTIFI_DOWNLOAD_ID = 0x123;
    private Context context;
    private Activity activity;
    private String downloadUrl;
    private NotificationManager manager;
    private Notification.Builder builder;
    private String lastName;
    private String description;
    int progress = 0;

    public YougelUpdate(Context context) {
        this.context = context;
        this.activity = (Activity) context;
    }

    //获取应用当前版本号
    public int getVersionCode() {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

    //检查应用版本
    public void checkVersion(final String url) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL requestUrl = new URL(url);
                    HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(10 * 1000);
                    connection.setReadTimeout(15 * 1000);

                    if (connection.getResponseCode() == 200) {
                        InputStream is = connection.getInputStream();
                        String response = read(is);
                        JSONObject jObject = new JSONObject(response);
                        int lastVersion = jObject.getInt(VERSIONCODE);
                        lastName = jObject.getString(VERSIONNAME);
                        downloadUrl = jObject.getString(DOWNLOADURL);
                        description = jObject.getString(DESCRIPTION);

                        if (lastVersion > getVersionCode()) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showDialogForUpdate();
                                }
                            });
                        } else {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    myToast("当前已经是最新版本");
                                }
                            });
                        }

                    }

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //弹出dialog提示版本更新
    public void showDialogForUpdate() {
        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("更新模块" + lastName);
        builder.setMessage(description);
        builder.setPositiveButton("继续", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new DownloadTask().execute(downloadUrl);
            }
        });
        builder.setNegativeButton("下次再说", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    //异步任务下载
    class DownloadTask extends AsyncTask<String, Integer, File> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            builder = new Notification.Builder(context);
            builder.setContentTitle("开始下载文件");
            builder.setSmallIcon(R.mipmap.icon_update);
            //设置进度条，false表示为确定的
            builder.setProgress(100, 0, false);
            manager.notify(NOTIFI_DOWNLOAD_ID, builder.build());
        }

        @Override
        protected File doInBackground(String... params) {
            InputStream in = null;
            FileOutputStream out = null;

            String appName = context.getString(context.getApplicationInfo().labelRes);
            int icon = context.getApplicationInfo().icon;
            try {
                URL requrl = new URL(params[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) requrl.openConnection();

                urlConnection.setRequestMethod("GET");
                urlConnection.setDoOutput(false);
                urlConnection.setConnectTimeout(10 * 1000);
                urlConnection.setReadTimeout(10 * 1000);
                urlConnection.setRequestProperty("Connection", "Keep-Alive");
                urlConnection.setRequestProperty("Charset", "UTF-8");
                urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");

                urlConnection.connect();
                long bytetotal = urlConnection.getContentLength();
                long bytesum = 0;
                int byteread = 0;
                in = urlConnection.getInputStream();
                File dir = getCacheDirectory(context);
                String apkName = downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1, downloadUrl.length());
                File apkFile = new File(dir, apkName);
                out = new FileOutputStream(apkFile);
                byte[] buffer = new byte[1024 * 10000];

                while ((byteread = in.read(buffer)) > -1) {
                    bytesum += byteread;
                    out.write(buffer, 0, byteread);
                    progress = (int) (((float) bytesum / (float) bytetotal) * 100);
                    publishProgress(progress);
                }
                return apkFile;
            } catch (Exception e) {
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ignored) {

                    }
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ignored) {

                    }
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            builder.setProgress(100, values[0], false);
            builder.setContentTitle("正在下载文件");
            builder.setContentText("当前进度是：" + values[0] + "%");

            Notification notify = builder.build();
            //不让用户删除通知信息
            notify.flags = Notification.FLAG_NO_CLEAR;
            manager.notify(NOTIFI_DOWNLOAD_ID, notify);
        }

        @Override
        protected void onPostExecute(File result) {
            super.onPostExecute(result);

            if (progress == 100) {
                manager.cancel(NOTIFI_DOWNLOAD_ID);
            }
            if (!result.exists()) {
                myToast("下载失败");
            } else {
                installAPk(result);
                deleteApk(result);
            }
        }
    }

    //安装apk
    private void installAPk(File apkFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        //如果没有设置SDCard写权限，或者没有sdcard,apk文件保存在内存中，需要授予权限才能安装
        try {
            String[] command = {"chmod", "777", apkFile.toString()};
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.start();
        } catch (IOException ignored) {
        }
        intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    //删除安装包
    public void deleteApk(final File apkFile) {
        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (apkFile.exists()) {
            builder.setTitle("温馨提示");
            long size = apkFile.length();
            int unit = 0;
            if (size > 0 && size < 1024) {
                builder.setMessage("安装失败，安装包大小是 " + size + " B,是否删除");
            } else if (size >= 1024 && size < 1024 * 1024) {
                unit = 1024;
                builder.setMessage("安装失败，安装包大小是 " + getSize(size, unit) + " KB,是否删除");
            } else if (size >= 1024 * 1024 && size < 1024 * 1024 * 1024) {
                unit = 1024 * 1024;
                builder.setMessage("安装失败，安装包大小是 " + getSize(size, unit) + " MB,是否删除");
            } else {
                unit = 1024 * 1024 * 1024;
                builder.setMessage("安装失败，安装包大小是 " + getSize(size, unit) + " GB,是否删除");
            }
            builder.setPositiveButton("删除", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    apkFile.delete();
                }
            });
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
    }

    public static File getCacheDirectory(Context context) {
        File appCacheDir = null;
        if (MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) && hasExternalStoragePermission(context)) {
            appCacheDir = getExternalCacheDir(context);
        }
        if (appCacheDir == null) {
            appCacheDir = context.getCacheDir();
        }
        if (appCacheDir == null) {
        }
        return appCacheDir;
    }

    private static File getExternalCacheDir(Context context) {
        File dataDir = new File(new File(Environment.getExternalStorageDirectory(), "Android"), "data");
        File appCacheDir = new File(new File(dataDir, context.getPackageName()), "cache");
        if (!appCacheDir.exists()) {
            if (!appCacheDir.mkdirs()) {
                return null;
            }
            try {
                new File(appCacheDir, ".nomedia").createNewFile();
            } catch (IOException e) {
            }
        }
        return appCacheDir;
    }

    private static boolean hasExternalStoragePermission(Context context) {
        int perm = context.checkCallingOrSelfPermission(EXTERNAL_STORAGE_PERMISSION);
        return perm == PackageManager.PERMISSION_GRANTED;
    }

    //将流转换为字符串的方法
    public static String read(InputStream is) {
        BufferedReader dr = null;
        try {
            dr = new BufferedReader(new InputStreamReader(is, "gbk"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        StringBuffer stringBuffer = new StringBuffer();
        String line = "";
        try {
            while ((line = dr.readLine()) != null) {
                stringBuffer.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuffer.toString();
    }

    //吐司
    public void myToast(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    //计算文件大小
    public double getSize(long total, int unit) {
        double dTotal = total;
        double dUnit = unit;
        BigDecimal decimal = new BigDecimal(dTotal / dUnit);
        return decimal.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
}
