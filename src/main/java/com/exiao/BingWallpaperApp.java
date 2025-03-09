package com.exiao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BingWallpaperApp {
    private static final Logger logger = LoggerFactory.getLogger(BingWallpaperApp.class);
    private static final String BING_API_URL = "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=zh-CN";
    private static final String BING_BASE_URL = "https://www.bing.com";
    private static final String IMAGES_DIR = "docs/.vuepress/public/images";
    private static final String WALLPAPER_DATA_FILE = "docs/.vuepress/public/data/bing-wallpaper.json";

    public static void main(String[] args) {
        try {
            logger.info("开始执行Bing壁纸更新任务");
            
            // 确保目录存在
            createDirectoryIfNotExists(IMAGES_DIR);
            createDirectoryIfNotExists(Paths.get(WALLPAPER_DATA_FILE).getParent().toString());
            
            // 获取Bing壁纸信息
            String bingData = fetchBingData();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(bingData);
            
            // 解析壁纸数据
            JsonNode imageNode = rootNode.path("images").get(0);
            String imageUrl = BING_BASE_URL + imageNode.path("url").asText();
            String copyright = imageNode.path("copyright").asText();
            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            
            // 下载壁纸
            String fileName = "bingWallpaper.jpg";
            String localPath = IMAGES_DIR + "/" + fileName;
            downloadImage(imageUrl, localPath);
            
            // 更新数据文件
            updateWallpaperData(date, copyright, fileName);
            
            logger.info("Bing壁纸更新任务完成");
        } catch (Exception e) {
            logger.error("执行Bing壁纸更新任务时发生错误", e);
            System.exit(1);
        }
    }
    
    private static String fetchBingData() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BING_API_URL);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }
    
    private static void downloadImage(String imageUrl, String localPath) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(imageUrl);
            try (CloseableHttpResponse response = httpClient.execute(request);
                 InputStream inputStream = response.getEntity().getContent();
                 FileOutputStream outputStream = new FileOutputStream(localPath)) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                logger.info("壁纸已下载到: {}", localPath);
            }
        }
    }
    
    private static void updateWallpaperData(String date, String copyright, String fileName) throws IOException {
        File dataFile = new File(WALLPAPER_DATA_FILE);
        ObjectMapper mapper = new ObjectMapper();
        
        JsonNode rootNode;
        if (dataFile.exists()) {
            rootNode = mapper.readTree(dataFile);
        } else {
            rootNode = mapper.createObjectNode();
        }
        
        ((com.fasterxml.jackson.databind.node.ObjectNode) rootNode).put("lastUpdate", date);
        ((com.fasterxml.jackson.databind.node.ObjectNode) rootNode).put("copyright", copyright);
        ((com.fasterxml.jackson.databind.node.ObjectNode) rootNode).put("image", "images/bing/" + fileName);
        
        mapper.writerWithDefaultPrettyPrinter().writeValue(dataFile, rootNode);
        logger.info("壁纸数据已更新");
    }
    
    private static void createDirectoryIfNotExists(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            logger.info("创建目录: {}", dirPath);
        }
    }
}