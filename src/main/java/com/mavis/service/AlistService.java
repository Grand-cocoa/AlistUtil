package com.mavis.service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Kane
 * @since 2024/10/9 10:49
 */
public interface AlistService {
    /**
     * 获取AlistToken
     *
     * @return AlistToken
     */
    String getToken();
    /**
     * 列出目录文件
     *
     * @param path 路径
     *             eg:/files/test
     */
    String getAlistFileList(String path);
    /**
     * 获取文件信息
     *
     * @param path     路径 eg: /files/test/xxx.txt
     * @param password 文件密码，可以传空字符串
     */
    String getAlistFileInfo(String path, String password);
    /**
     * 递归-遍历文件夹下所有文件下载地址
     *
     * @param path     目录 eg:/files/test
     * @param password 文件密码，可以传空字符串
     * @return {path : rawUrl}
     */
    ArrayList<HashMap<String, String>> getAlistAllFilesInfo(String path, String password);
    /**
     * 上传文件
     *
     * @param originalPath 目标文件
     * @param targetPath   目的路径
     * @param FileName     目的文件名
     * @return 文件路径/结果
     */
    String uploadFile(String originalPath, String FileName, String targetPath) throws IOException;

    /**
     * 上传文件
     * @param outputStream 文件流
     * @param FileName     目标文件名
     * @param targetPath   目标路径
     * @return 文件路径/结果
     */
    String uploadFile(byte[] outputStream, String FileName, String targetPath) throws IOException;
}
