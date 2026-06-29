package com.moyuan.service;

import org.springframework.web.multipart.MultipartFile;

public interface AiService {

    String chat(String message, String model);

    String chat(String message, String model, String moduleCode);

    String writePoemFromImage(MultipartFile image, String model);

    String writePoemFromImage(MultipartFile image, String model, String visionModel);

    String writePoemFromImage(MultipartFile image, String model, String visionModel, String moduleCode);

    String analyzePoem(String poem, String model);

    String analyzePoem(String poem, String model, String moduleCode);

    String matchCouplet(String upperCouplet, String model);

    String matchCouplet(String upperCouplet, String model, String moduleCode);

    String ocrImage(MultipartFile image, String model, String visionModel, String moduleCode);
}
