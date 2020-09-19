package com.vodafone.idtmlib.lib.dagger;

import android.content.Context;

import com.vodafone.idtmlib.BuildConfig;
import com.vodafone.idtmlib.lib.utils.Printer;

import java.io.File;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class PrinterModule {
    private boolean enable;

    public PrinterModule(boolean enable) {
        this.enable = enable;
    }


    @Singleton
    @Provides
    Thread.UncaughtExceptionHandler provideDefaultCrashHandler() {
        return Thread.getDefaultUncaughtExceptionHandler();
    }

    @Singleton
    @Provides
    @Named("printer_file_path")
    String providePrinterFilePath(Context context) {
        File externalCacheDir = context.getExternalCacheDir();
        if (externalCacheDir == null) {
            return null;
        }
        StringBuilder filename = new StringBuilder(externalCacheDir.getAbsolutePath());
        filename.append("/idtmLogs.txt");
        return filename.toString();
    }

    @Singleton
    @Provides
    Printer.TagVerbosityLevel provideTagVerbosityLevel() {
        return Printer.TagVerbosityLevel.NONE;
    }

    @Singleton
    @Provides
    Printer providePrinter(Context context, Thread.UncaughtExceptionHandler defaultCrashHandler,
                           @Named("printer_file_path") String filepath,
                           Printer.TagVerbosityLevel tagVerbosityLevel) {
        if (enable) {
            return new Printer(context, defaultCrashHandler, filepath, BuildConfig.APP_CODENAME,
                    tagVerbosityLevel);
        } else {
            return new Printer(context, defaultCrashHandler, null, BuildConfig.APP_CODENAME,
                    tagVerbosityLevel) {

                @Override
                protected synchronized void msg(int type, Object... objs) {
                    // do nothing
                }
            };
        }
    }
}
