package com.moyuan.service;

import org.springframework.web.multipart.MultipartFile;

public interface AiService {

    String chat(String message, String model);

    String writePoemFromImage(MultipartFile image, String model);

    String analyzePoem(String poem, String model);
}