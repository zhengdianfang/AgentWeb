
package com.just.agentweb;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.List;

import static android.provider.MediaStore.EXTRA_OUTPUT;
import static com.just.agentweb.FileUpLoadChooserImpl.REQUEST_CODE;

/**
 * <p>
 * Created by cenxiaozhong on 2017.8.19
 * </p>
 */
public final class ActionActivity extends Activity {

    public static final String KEY_ACTION = "KEY_ACTION";
    public static final String KEY_URI = "KEY_URI";
    public static final String KEY_FROM_INTENTION = "KEY_FROM_INTENTION";
    public static final String KEY_FILE_CHOOSER_INTENT = "KEY_FILE_CHOOSER_INTENT";
    private static RationaleListener mRationaleListener;
    private static PermissionListener mPermissionListener;
    private static FileDataListener mFileDataListener;
    private static final String TAG = ActionActivity.class.getSimpleName();
    private Action mAction;


    static void start(Activity activity, Action action) {

        Intent mIntent = new Intent(activity, ActionActivity.class);
        mIntent.putExtra(KEY_ACTION, action);
//        mIntent.setExtrasClassLoader(Action.class.getClassLoader());
        activity.startActivity(mIntent);

    }

    static void setFileDataListener(FileDataListener fileDataListener) {
        mFileDataListener = fileDataListener;
    }

    static void setPermissionListener(PermissionListener permissionListener) {
        mPermissionListener = permissionListener;
    }

    private void cancelAction() {
        mFileDataListener = null;
        mPermissionListener = null;
        mRationaleListener = null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            LogUtils.i(TAG, "savedInstanceState:" + savedInstanceState);
            return;
        }
        Intent intent = getIntent();
        mAction = intent.getParcelableExtra(KEY_ACTION);
        if (mAction == null) {
            cancelAction();
            this.finish();
            return;
        }
        if (mAction.getAction() == Action.ACTION_PERMISSION) {
            permission(mAction);
        } else if (mAction.getAction() == Action.ACTION_CAMERA) {
            realOpenCamera();
        } else if (mAction.getAction() == Action.ACTION_RECORD_VIDEO) {
            realOpenVideo();
        } else {
            fetchFile(mAction);
        }

    }

    private void fetchFile(Action action) {

        if (mFileDataListener == null)
            finish();

        openRealFileChooser();
    }

    private void openRealFileChooser() {

        try {
            if (mFileDataListener == null) {
                finish();
                return;
            }

            Intent mIntent = getIntent().getParcelableExtra(KEY_FILE_CHOOSER_INTENT);
            if (mIntent == null) {
                cancelAction();
                return;
            }
            this.startActivityForResult(mIntent, REQUEST_CODE);
        } catch (Throwable throwable) {
            LogUtils.i(TAG, "找不到文件选择器");
            fileDataActionOver(-1, null);
            if (LogUtils.isDebug()) {
                throwable.printStackTrace();
            }
        }

    }

    private void fileDataActionOver(int resultCode, Intent data) {
        if (mFileDataListener != null) {
            mFileDataListener.onFileDataResult(REQUEST_CODE, resultCode, data);
            mFileDataListener = null;
        }
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {


        LogUtils.i(TAG, "mFileDataListener:" + mFileDataListener);
        if (requestCode == REQUEST_CODE) {
            fileDataActionOver(resultCode, mUri != null ? new Intent().putExtra(KEY_URI, mUri) : data);
        }
    }

    private void permission(Action action) {


        List<String> permissions = action.getPermissions();
        LogUtils.i(TAG, "permission:" + action.getPermissions());
        if (AgentWebUtils.isEmptyCollection(permissions)) {
            mPermissionListener = null;
            mRationaleListener = null;
            finish();
            return;
        }

        if (mRationaleListener != null) {
            boolean rationale = false;
            for (String permission : permissions) {
                rationale = shouldShowRequestPermissionRationale(permission);
                if (rationale) break;
            }
            mRationaleListener.onRationaleResult(rationale, new Bundle());
            mRationaleListener = null;
            finish();
            return;
        }

        if (mPermissionListener != null)
            requestPermissions(permissions.toArray(new String[]{}), 1);

        LogUtils.i(TAG, "request permission send");
    }

    private Uri mUri;

    private void realOpenCamera() {

        try {
            if (mFileDataListener == null)
                finish();
            File mFile = AgentWebUtils.createImageFile(this);
            if (mFile == null) {
                mFileDataListener.onFileDataResult(REQUEST_CODE, Activity.RESULT_CANCELED, null);
                mFileDataListener = null;
                finish();
            }
            Intent intent = AgentWebUtils.getIntentCaptureCompat(this, mFile);
            LogUtils.i(TAG, "listener:" + mFileDataListener + "  file:" + mFile.getAbsolutePath());
            // 指定开启系统相机的Action
            mUri = intent.getParcelableExtra(EXTRA_OUTPUT);
            this.startActivityForResult(intent, REQUEST_CODE);
        } catch (Throwable ignore) {
            LogUtils.i(TAG, "找不到系统相机");
            if (mFileDataListener != null) {
                mFileDataListener.onFileDataResult(REQUEST_CODE, Activity.RESULT_CANCELED, null);
            }
            mFileDataListener = null;
            if (LogUtils.isDebug())
                ignore.printStackTrace();
        }


    }

    private void realOpenVideo() {

        try {
            if (mFileDataListener == null)
                finish();
            File mFile = AgentWebUtils.createVideoFile(this);
            if (mFile == null) {
                mFileDataListener.onFileDataResult(REQUEST_CODE, Activity.RESULT_CANCELED, null);
                mFileDataListener = null;
                finish();
            }
            Intent intent = AgentWebUtils.getIntentVideoRecordCompat(this, mFile);
            LogUtils.i(TAG, "listener:" + mFileDataListener + "  file:" + mFile.getAbsolutePath());
            mUri = intent.getParcelableExtra(EXTRA_OUTPUT);
            this.startActivityForResult(intent, REQUEST_CODE);
        } catch (Throwable ignore) {
            LogUtils.i(TAG, "找不到系统相机");
            if (mFileDataListener != null) {
                mFileDataListener.onFileDataResult(REQUEST_CODE, Activity.RESULT_CANCELED, null);
            }
            mFileDataListener = null;
            if (LogUtils.isDebug())
                ignore.printStackTrace();
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        LogUtils.i(TAG, "onRequestPermissionsResult");
        if (mPermissionListener != null) {
            Bundle mBundle = new Bundle();
            mBundle.putInt(KEY_FROM_INTENTION, mAction.getFromIntention());
            mPermissionListener.onRequestPermissionsResult(permissions, grantResults, mBundle);
        }
        mPermissionListener = null;
        finish();
    }

    interface RationaleListener {
        void onRationaleResult(boolean showRationale, Bundle extras);
    }

    interface PermissionListener {
        void onRequestPermissionsResult(@NonNull String[] permissions, @NonNull int[] grantResults, Bundle extras);
    }

    interface FileDataListener {
        void onFileDataResult(int requestCode, int resultCode, Intent data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
