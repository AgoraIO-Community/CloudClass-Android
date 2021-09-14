package io.agora.edu.core.internal.download.db;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import io.agora.edu.core.internal.download.config.InnerConstant;
import io.agora.edu.core.internal.download.utils.LogUtils;


public class DbOpenHelper extends SQLiteOpenHelper{
    public static final String TAG = "DbOpenHelper";

    public DbOpenHelper(Context context) {
        super(context, InnerConstant.Db.NAME_DB, null, getVersionCode(context));
    }

    private static int getVersionCode(Context context){
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        }catch (Exception e){
            LogUtils.e(TAG, "create db failed!");
            e.printStackTrace();
        }
        return 0;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        String info = "create table if not exists " + InnerConstant.Db.NAME_TABALE
                + "(" +
                InnerConstant.Db.id + " varchar(500)," +
                InnerConstant.Db.downloadUrl + " varchar(100)," +
                InnerConstant.Db.filePath + " varchar(100)," +
                InnerConstant.Db.size + " integer," +
                InnerConstant.Db.downloadLocation + " integer," +
                InnerConstant.Db.downloadStatus + " integer)";

        LogUtils.i(TAG, "onCreate() -> sql=" + info);
        db.execSQL(info);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //For download, in fact, there is no such business of upgrading the database.
        // So we directly delete and rebuild the table
        db.execSQL("drop table if exists " + InnerConstant.Db.NAME_TABALE);
        onCreate(db);
    }
}
