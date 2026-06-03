package com.moyuan.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * 图片水印工具类
 */
@Slf4j
@Component
public class WatermarkUtil {

    /** 左上角 */
    public static final int TOP_LEFT = 0;
    /** 右上角 */
    public static final int TOP_RIGHT = 1;
    /** 左下角 */
    public static final int BOTTOM_LEFT = 2;
    /** 右下角 */
    public static final int BOTTOM_RIGHT = 3;
    /** 居中 */
    public static final int CENTER = 4;

    @Value("${file.watermark.text:AI生成}")
    private String watermarkText;

    @Value("${file.watermark.logo-path:./uploads/watermark/logo.png}")
    private String logoPath;

    @Value("${file.watermark.opacity:0.5}")
    private float watermarkOpacity;

    @Value("${file.watermark.position:3}")
    private int watermarkPosition;

    /**
     * 添加文字水印
     *
     * @param image    原始图片
     * @param text     水印文字
     * @param color    水印颜色
     * @param font     水印字体
     * @param position 水印位置
     * @return 添加水印后的图片
     */
    public BufferedImage addTextWatermark(BufferedImage image, String text, Color color, Font font, int position) {
        if (image == null || text == null || text.isEmpty()) {
            return image;
        }

        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage watermarkedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = watermarkedImage.createGraphics();

        try {
            g2d.drawImage(image, 0, 0, null);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            g2d.setColor(color);
            g2d.setFont(font);

            FontRenderContext frc = g2d.getFontRenderContext();
            Rectangle2D bounds = font.getStringBounds(text, frc);
            int textWidth = (int) bounds.getWidth();
            int textHeight = (int) bounds.getHeight();

            int padding = 20;
            int x = padding;
            int y = textHeight + padding;

            switch (position) {
                case TOP_LEFT:
                    x = padding;
                    y = textHeight + padding;
                    break;
                case TOP_RIGHT:
                    x = width - textWidth - padding;
                    y = textHeight + padding;
                    break;
                case BOTTOM_LEFT:
                    x = padding;
                    y = height - textHeight;
                    break;
                case BOTTOM_RIGHT:
                    x = width - textWidth - padding;
                    y = height - textHeight;
                    break;
                case CENTER:
                    x = (width - textWidth) / 2;
                    y = (height + textHeight) / 2;
                    break;
            }

            g2d.drawString(text, x, y);
        } finally {
            g2d.dispose();
        }

        return watermarkedImage;
    }

    /**
     * 添加图片水印
     *
     * @param image     原始图片
     * @param watermark 水印图片
     * @param position  水印位置
     * @param opacity   水印透明度（0.0-1.0）
     * @return 添加水印后的图片
     */
    public BufferedImage addImageWatermark(BufferedImage image, BufferedImage watermark, int position, float opacity) {
        if (image == null || watermark == null) {
            return image;
        }

        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage watermarkedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = watermarkedImage.createGraphics();

        try {
            g2d.drawImage(image, 0, 0, null);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
            g2d.setComposite(alphaChannel);

            int watermarkWidth = watermark.getWidth();
            int watermarkHeight = watermark.getHeight();

            int padding = 20;
            int x = padding;
            int y = padding;

            switch (position) {
                case TOP_LEFT:
                    x = padding;
                    y = padding;
                    break;
                case TOP_RIGHT:
                    x = width - watermarkWidth - padding;
                    y = padding;
                    break;
                case BOTTOM_LEFT:
                    x = padding;
                    y = height - watermarkHeight - padding;
                    break;
                case BOTTOM_RIGHT:
                    x = width - watermarkWidth - padding;
                    y = height - watermarkHeight - padding;
                    break;
                case CENTER:
                    x = (width - watermarkWidth) / 2;
                    y = (height - watermarkHeight) / 2;
                    break;
            }

            g2d.drawImage(watermark, x, y, null);
        } finally {
            g2d.dispose();
        }

        return watermarkedImage;
    }

    /**
     * 添加AI生成水印（使用配置的默认文字和透明度）
     *
     * @param image 原始图片
     * @return 添加水印后的图片
     */
    public BufferedImage addAiWatermark(BufferedImage image) {
        if (image == null) {
            return image;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int fontSize = Math.max(24, Math.min(width, height) / 15);

        Font font = new Font("Microsoft YaHei", Font.BOLD, fontSize);
        Color color = new Color(255, 255, 255, (int) (watermarkOpacity * 255));

        return addTextWatermark(image, watermarkText, color, font, watermarkPosition);
    }

    /**
     * 从文件加载水印图片
     *
     * @param filePath 水印图片路径
     * @return 水印图片，加载失败返回null
     */
    public BufferedImage loadWatermarkImage(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.warn("水印图片文件不存在：{}", filePath);
                return null;
            }
            return ImageIO.read(file);
        } catch (IOException e) {
            log.error("加载水印图片失败：{}", filePath, e);
            return null;
        }
    }

    /**
     * 添加logo水印（使用配置的logo路径）
     *
     * @param image 原始图片
     * @return 添加水印后的图片
     */
    public BufferedImage addLogoWatermark(BufferedImage image) {
        if (image == null) {
            return image;
        }

        BufferedImage logo = loadWatermarkImage(logoPath);
        if (logo == null) {
            log.warn("logo水印图片加载失败，跳过水印添加");
            return image;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int logoMaxSize = Math.min(width, height) / 5;

        double ratio = Math.min((double) logoMaxSize / logo.getWidth(), (double) logoMaxSize / logo.getHeight());
        int targetWidth = (int) (logo.getWidth() * ratio);
        int targetHeight = (int) (logo.getHeight() * ratio);

        BufferedImage scaledLogo = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledLogo.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(logo, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return addImageWatermark(image, scaledLogo, watermarkPosition, watermarkOpacity);
    }

    /**
     * 获取配置的水印位置
     *
     * @return 水印位置常量
     */
    public int getConfiguredPosition() {
        return watermarkPosition;
    }

    /**
     * 获取配置的水印透明度
     *
     * @return 水印透明度
     */
    public float getConfiguredOpacity() {
        return watermarkOpacity;
    }

    /**
     * 获取配置的水印文字
     *
     * @return 水印文字
     */
    public String getConfiguredText() {
        return watermarkText;
    }
}