package com.quickspeech.knowledge.service;

import com.quickspeech.common.exception.BusinessException;
import com.quickspeech.common.constant.ResponseCode;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class DocumentParserService {

    private final Tika tika = new Tika();

    /**
     * 解析上传的文件
     */
    public String parseFile(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            if (fileName == null) {
                throw new BusinessException(ResponseCode.FILE_PARSE_ERROR, "文件名为空");
            }

            String fileType = getFileType(fileName);
            return parseByType(file.getInputStream(), fileType);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResponseCode.FILE_PARSE_ERROR,
                    "文件解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析文件路径
     */
    public String parseFilePath(String filePath) {
        try {
            Path path = Path.of(filePath);
            String fileName = path.getFileName().toString();
            String fileType = getFileType(fileName);

            try (InputStream is = Files.newInputStream(path)) {
                return parseByType(is, fileType);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResponseCode.FILE_PARSE_ERROR,
                    "文件解析失败: " + e.getMessage());
        }
    }

    private String parseByType(InputStream inputStream, String fileType) throws IOException {
        switch (fileType.toLowerCase()) {
            case "pdf":
            case "doc":
            case "docx":
            case "xls":
            case "xlsx":
            case "ppt":
            case "pptx":
                return parseWithTika(inputStream);
            case "txt":
            case "md":
            case "markdown":
            case "csv":
            case "json":
            case "xml":
            case "html":
                return parseText(inputStream);
            default:
                try {
                    return parseWithTika(inputStream);
                } catch (Exception e) {
                    return parseText(inputStream);
                }
        }
    }

    private String parseWithTika(InputStream inputStream) throws IOException {
        try {
            return tika.parseToString(inputStream);
        } catch (TikaException e) {
            throw new IOException("Tika解析失败: " + e.getMessage(), e);
        }
    }

    private String parseText(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String getFileType(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot + 1);
        }
        return "txt";
    }
}
