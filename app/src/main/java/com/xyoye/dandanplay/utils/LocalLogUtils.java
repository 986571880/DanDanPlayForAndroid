package com.xyoye.dandanplay.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by xyoye on 2019/11/6.
 */

public class LocalLogUtils {
    private ExecutorService singleExecutor;
    private File mLogFile;
    private SimpleDateFormat dateFormat;
    private boolean isEnableLog;

    private LocalLogUtils() {
        isEnableLog = AppConfig.getInstance().isOnlinePlayLogEnable();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault());
        singleExecutor = Executors.newSingleThreadExecutor();
        mLogFile = new File(Constants.DefaultConfig.logPath);
        File folder = mLogFile.getParentFile();
        if (!folder.exists() || !folder.isDirectory()){
            folder.mkdirs();
        }
        if (mLogFile.exists()) {
            mLogFile.delete();
            try {
                mLogFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class Holder {
        static LocalLogUtils instance = new LocalLogUtils();
    }

    public static LocalLogUtils getInstance() {
        return Holder.instance;
    }

    public void write(String log) {
        if (!isEnableLog) return;

        singleExecutor.execute(() -> {
            try {
                String time = dateFormat.format(System.currentTimeMillis());
                String realLog = time + "   "+log;

                FileWriter fileWriter = new FileWriter(mLogFile, true);
                fileWriter.write(realLog);
                fileWriter.write("\n");
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
