package com.leiyun.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by LeiYun on 2016/11/23 0023.
 */

public class FlickrFetchr {
    private static final String TAG = "FlickrFetchr";
    private static final String API_KEY = "025d91011c70c32f8588e9e30e32d05b";


    /**
     * 指定URL获取原始数据并返回一个字节流数组
     * @param urlSpec 指定一个URL
     * @return byte[]
     * @throws IOException
     */
    public byte[] getUrlBytes(String urlSpec) throws IOException{
        URL url = new URL(urlSpec); // 根据String创建一个URL
        // openConnection返回的是URLConnection对象，所以要强制转换
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() +
                                        "; with " +
                                        urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        }finally {
            connection.disconnect();
        }
    }

    /**
     * 将getUrlBytes(String)方法返回的结果转换为 String 。
     * @param urlSpec 指定一个URL
     * @return
     * @throws IOException
     */
    public String getUrlString(String urlSpec) throws IOException{
        return new String(getUrlBytes(urlSpec));
    }

    /**
     * 构建URL并获取内容的方法
     */
    public List<GalleryItem> fetchItems() {
        List<GalleryItem> items = new ArrayList<>();

        try {
            String url = Uri.parse("https://api.flickr.com/services/rest/")
                    .buildUpon()
                    // appendQueryParameter(String,String) 可自动转义查询字符串
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("extras", "url_s") //这个参数值告诉Flickr：如有小尺寸图片，也一并返回其URL
                    .build().toString();

            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON " + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(items, jsonBody);
        } catch (IOException e) {
            Log.e(TAG, "Failed to fetch items ", e);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse JSON", e);
        }

        return items;
    }

    /**
     * 取出每张图片的信息，生成一个个 GalleryItem 对象
     * 再将它们添加到 List 中
     * @param items GalleryItem对象的List
     * @param jsonBody json数据
     */
    private void parseItems(List<GalleryItem> items, JSONObject jsonBody) throws JSONException {
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");

        for (int i = 0; i < photoJsonArray.length(); i++) {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);

            GalleryItem item = new GalleryItem();
            item.setId(
                    photoJsonObject.getString("id"));
            item.setCaption(
                    photoJsonObject.getString("title"));

            if (!photoJsonObject.has("url_s")) {
                continue;
            }

            item.setUrl(photoJsonObject.getString("url_s"));
            items.add(item);
        }
    }
}
