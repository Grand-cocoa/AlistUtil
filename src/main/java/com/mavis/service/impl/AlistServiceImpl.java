package com.mavis.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mavis.entity.AlistConfig;
import com.mavis.service.AlistService;
import com.mavis.util.OkHttpUtils;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Kane
 * @since 2024/10/9 10:50
 */
@Service
@EnableConfigurationProperties(AlistConfig.class)
public class AlistServiceImpl implements AlistService {

    private final AlistConfig alistConfig;

    private String token = null;

    public AlistServiceImpl(@Qualifier("97166c07-9e60-405b-adc7-897c142b4119") AlistConfig alistConfig) {
        this.alistConfig = alistConfig;
    }

    /**
     * 获取AlistToken
     *
     * @return AlistToken
     */
    @Override
    public synchronized String getToken() {
        if (token != null){
            return token;
        }
        String url = alistConfig.getAlistBaseUrl() + "/api/auth/login";
        HashMap<String, String> dataBody = new HashMap<>();
        dataBody.put("username", alistConfig.getAlistUsername());
        dataBody.put("password", alistConfig.getAlistPassword());
        // 发送请求获取token
        String resultBody = OkHttpUtils.doPost(url, dataBody, null);
        // 处理返回数据获取token并返回
        JSONObject json_data = JSON.parseObject(JSON.parseObject(resultBody).get("data").toString());
        return json_data.get("token").toString();
    }

    /**
     * 列出目录文件
     *
     * @param path 路径
     *             eg:/files/test
     */
    @Override
    public String getAlistFileList(String path) {
        String url = alistConfig.getAlistBaseUrl() + "/api/fs/list";
        HashMap<String, String> headerData = new HashMap<>();
        HashMap<String, String> bodyData = new HashMap<>();
        bodyData.put("path", path);
        headerData.put("Authorization", getToken());
        return OkHttpUtils.doPostWithHeaders(url, bodyData, headerData, () -> {
            token = null;
            headerData.put("Authorization", getToken());
            return OkHttpUtils.doPostWithHeaders(url, bodyData, headerData, null);
        });
    }

    /**
     * 获取文件信息
     *
     * @param path     路径 eg: /files/test/xxx.txt
     * @param password 文件密码，可以传空字符串
     * @return
     */
    @Override
    public String getAlistFileInfo(String path, String password) {
        String url = alistConfig.getAlistBaseUrl() + "/api/fs/get";
        HashMap<String, String> headerData = new HashMap<>();
        HashMap<String, String> bodyData = new HashMap<>();
        bodyData.put("path", path);
        bodyData.put("password", password);
        headerData.put("Authorization", getToken());
        return OkHttpUtils.doPostWithHeaders(url, bodyData, headerData, () -> {
            token = null;
            headerData.put("Authorization", getToken());
            return OkHttpUtils.doPostWithHeaders(url, bodyData, headerData, null);
        });
    }

    /**
     * 递归-遍历文件夹下所有文件下载地址
     *
     * @param path     目录 eg:/files/test
     * @param password 文件密码，可以传空字符串
     * @return {path : rawUrl}
     */
    @Override
    public ArrayList<HashMap<String, String>> getAlistAllFilesInfo(String path, String password) {
        // 获取path目录下信息
        String result = getAlistFileList(path);
        // 获取所有的信息
        JSONObject json_data = JSON.parseObject(JSON.parseObject(result).get("data").toString());
        // 将content转为json数组
        JSONArray json_array = json_data.getJSONArray("content");

        ArrayList<HashMap<String, String>> filesInfoMaps = new ArrayList<>();
        processDirectory(filesInfoMaps, path, json_array, password);
        return filesInfoMaps;

    }

    //处理文件夹
    private void processDirectory(ArrayList<HashMap<String, String>> filesInfoMaps, String path, JSONArray json_array, String password) {
        //判空处理
        if (json_array == null || json_array.isEmpty()) {
            return;
        }
        for (int i = 0; i < json_array.size(); i++) {
            HashMap<String, String> filesInfoMap = new HashMap<>();
            // 获取当前json对象
            JSONObject json_object = json_array.getJSONObject(i);
            // 获取当前json对象的name
            String name = json_object.getString("name");
            // 判断是否为文件夹
            String is_dir = json_object.getString("is_dir");
            if (!"true".equals(is_dir)) {
                String filename = safePathJoin(path, name);
                String alistFileInfo = getAlistFileInfo(filename, password);
                JSONObject json_file_data = JSON.parseObject(JSON.parseObject(alistFileInfo).get("data").toString());
                if (json_file_data.containsKey("content")) {
                    JSONArray json_file_content = json_file_data.getJSONArray("content");
                    for (int j = 0; j < json_file_content.size(); j++) {
                        // 获取当前json对象
                        JSONObject json_file_object = json_file_content.getJSONObject(j);
                        // 获取当前json对象的raw_url
                        String rawUrl = json_file_object.getString("raw_url");
                        filesInfoMap.put("filePath", filename);
                        filesInfoMap.put("rawUrl", rawUrl);
                        filesInfoMaps.add(filesInfoMap);
                    }
                } else {
                    String rawUrl = json_file_data.getString("raw_url");
                    filesInfoMap.put("filePath", filename);
                    filesInfoMap.put("rawUrl", rawUrl);
                    filesInfoMaps.add(filesInfoMap);
                }
            } else {
                // 处理文件夹
                String directoryPath = safePathJoin(path, name);
                String directoryResult = getAlistFileList(directoryPath);
                JSONObject directoryJsonData = JSON.parseObject(JSON.parseObject(directoryResult).get("data").toString());
                JSONArray directoryJsonArray = directoryJsonData.getJSONArray("content");
                processDirectory(filesInfoMaps, directoryPath, directoryJsonArray, password);
            }
        }
    }

    private String safePathJoin(String base, String name) {
        // 防止路径穿越
        return base.endsWith("/") ? base + name : base + "/" + name;
    }


    /**
     * 上传文件
     *
     * @param originalPath 目标文件
     * @param targetPath   目的路径
     * @param FileName     目的文件名
     * @return 文件路径/结果
     * @throws IOException
     */
    @Override
    public String uploadFile(String originalPath, String FileName, String targetPath) throws IOException {
        String url = alistConfig.getAlistBaseUrl() + "/api/fs/put";
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("text/plain");
        byte[] bytes = Files.readAllBytes(Paths.get(originalPath));
        RequestBody body = RequestBody.create(mediaType, bytes);
        Request request = new Request.Builder()
                .url(url)
                .method("PUT", body)
                .addHeader("Authorization", getToken())
                .addHeader("File-Path", targetPath + "/" + FileName)
                .addHeader("As-Task", "true")
                .addHeader("Content-Length", "")
                .build();
        Response response = client.newCall(request).execute();
        JSONObject parse = JSON.parseObject(response.body().string());
        if (parse.get("code").toString().equals("200")) {
            return alistConfig.getAlistBaseUrl() + "/d" + targetPath + "/" + FileName;
        } else {
            return parse.get("message").toString();
        }
    }

    /**
     * 上传文件
     *
     * @param outputStream 文件流
     * @param FileName     目标文件名
     * @param targetPath   目标路径
     * @return 文件路径/结果
     */
    @Override
    public String uploadFile(byte[] outputStream, String FileName, String targetPath) throws IOException {
        String url = alistConfig.getAlistBaseUrl() + "/api/fs/put";
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create(mediaType, outputStream);
        Request request = new Request.Builder()
                .url(url)
                .method("PUT", body)
                .addHeader("Authorization", getToken())
                .addHeader("File-Path", targetPath + "/" + FileName)
                .addHeader("As-Task", "true")
                .addHeader("Content-Length", "")
                .build();
        Response response = client.newCall(request).execute();
        JSONObject parse = JSON.parseObject(response.body().string());
        if (parse.get("code").toString().equals("200")) {
            return alistConfig.getAlistBaseUrl() + "/d" + targetPath + "/" + FileName;
        } else {
            return parse.get("message").toString();
        }
    }

}
