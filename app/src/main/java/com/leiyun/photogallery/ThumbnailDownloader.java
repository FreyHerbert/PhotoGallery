package com.leiyun.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by LeiYun on 2016/11/27 0027.
 */

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0; // 标识下载请求消息

    private Boolean mHasQuit = false;
    private Handler mRequestHandler; // 负责ThumbnailDownloader后台线程上管理下载请求消息队列，同时负责从消息队列里取出并处理下载请求消息
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>(); // 用来存储对Handler的引用
    private Handler mResponseHandler;
    private ThumbnailDownloaderListener<T> mThumbnailDownloaderListener;

    /**
     * 为了把下载任务和更新UI的任务分开，需要一个监听器接口
     * @param <T>
     */
    public interface ThumbnailDownloaderListener<T> {
        void onThumbnailDownloader(T target, Bitmap thumbnail);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloaderListener<T> listener) {
        mThumbnailDownloaderListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        mRequestHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL：" + mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    /**
     * 处理target的url将每一个获取到的target都加入到消息队列
     * @param target 需要解析的目标
     * @param url 解析的url
     */
    public void queueThumbnail(T target, String url) {
        Log.i(TAG, "Got a URL: " + url);

        if (url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
                .sendToTarget();
        }
    }

    /**
     * 清除队列中的所有请求
     */
    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
    }

    /**
     * 处理handler发送过来的消息需要做的事情
     * 也就是执行下载动作的地方
     * @param target
     */
    private void handleRequest(final T target) {
        try {
            final String url = mRequestMap.get(target);

            if (url == null) {
                return;
            }

            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory
                    .decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.i(TAG, "Bitmap created");

            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mRequestMap.get(target) != url ||
                            mHasQuit) {
                        return;
                    }

                    mRequestMap.remove(target);
                    mThumbnailDownloaderListener.onThumbnailDownloader(target, bitmap);
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Error downloading image", e);
            e.printStackTrace();
        }
    }
}
