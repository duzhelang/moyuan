package com.moyuan.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 本地开发便利：后端启动时托管拉起 rag-service（Python FastAPI）。
 * <p>
 * 只在 8010 尚未监听时启动，已在运行则直接复用；后端关闭时只终止自己拉起的进程。
 * 生产环境建议关闭 {@code moyuan.rag-service.auto-start}，交给 systemd / Docker 管理。
 */
@Slf4j
@Getter
@Setter
@Component
@Profile("!test")
@ConfigurationProperties(prefix = "moyuan.rag-service")
public class RagServiceLauncher implements ApplicationRunner, DisposableBean {

    /** 是否在启动后端时自动拉起 RAG 服务 */
    private boolean autoStart = true;

    /** Python 可执行文件 */
    private String python = "python";

    /** rag-service 工作目录，相对后端启动目录 */
    private String workDir = "../rag-service";

    /** 启动脚本 */
    private String script = "app.py";

    /** 子进程日志文件 */
    private String logFile = "logs/rag-service.log";

    @Value("${moyuan.health.rag-url:http://127.0.0.1:8010/rag/health}")
    private String healthUrl;

    /** 是否由本进程拉起；只回收自己创建的进程，不误杀外部启动的 RAG */
    private volatile boolean managed;

    private volatile Process process;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @Override
    public void run(ApplicationArguments args) {
        if (!autoStart) {
            log.info("RAG 服务托管启动已关闭（moyuan.rag-service.auto-start=false）");
            return;
        }
        Thread launcher = new Thread(this::startIfNeeded, "rag-service-launcher");
        launcher.setDaemon(true);
        launcher.start();
    }

    private void startIfNeeded() {
        if (healthy()) {
            log.info("RAG 服务已在 {} 运行，跳过托管启动", healthUrl);
            return;
        }

        Path dir = resolveWorkDir();
        if (dir == null) {
            log.warn("未找到 rag-service 目录（配置 work-dir={}），跳过自动启动", workDir);
            return;
        }

        Path logPath = Paths.get(logFile).toAbsolutePath().normalize();
        try {
            if (logPath.getParent() != null) {
                Files.createDirectories(logPath.getParent());
            }
            ProcessBuilder builder = new ProcessBuilder(python, script)
                    .directory(dir.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(logPath.toFile()));
            builder.environment().put("PYTHONIOENCODING", "utf-8");
            builder.environment().put("PYTHONUTF8", "1");

            process = builder.start();
            managed = true;
            log.info("已拉起 RAG 服务 pid={}，工作目录={}，日志={}", process.pid(), dir, logPath);
        } catch (IOException e) {
            log.error("RAG 服务启动失败：{}", e.getMessage());
            return;
        }

        for (int second = 1; second <= 60; second++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (healthy()) {
                log.info("RAG 服务已就绪（{}），启动耗时约 {} 秒", healthUrl, second);
                return;
            }
            if (!process.isAlive()) {
                int exitCode = process.exitValue();
                process = null;
                managed = false;
                if (healthy()) {
                    log.info("RAG 端口已有其他实例在服务，沿用现有进程");
                } else {
                    log.error("RAG 服务进程已退出（exit={}），请查看日志 {}", exitCode, logPath);
                }
                return;
            }
        }
        log.warn("RAG 服务 60 秒内未就绪，仍在后台启动，请查看日志 {}", logPath);
    }

    /** 配置目录优先，其次按常见启动位置兜底，便于 IDE 与仓库根目录两种方式运行 */
    private Path resolveWorkDir() {
        Path configured = Paths.get(workDir).toAbsolutePath().normalize();
        if (Files.isRegularFile(configured.resolve(script))) {
            return configured;
        }
        List<String> candidates = List.of("rag-service", "../rag-service", "../../rag-service");
        Path base = Paths.get(System.getProperty("user.dir"));
        for (String candidate : candidates) {
            Path path = base.resolve(candidate).normalize();
            if (Files.isRegularFile(path.resolve(script))) {
                return path;
            }
        }
        return null;
    }

    /** 健康探测只用于判断端口是否已有服务，不解析业务字段 */
    private boolean healthy() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(healthUrl))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void destroy() {
        Process current = process;
        if (!managed || current == null || !current.isAlive()) {
            return;
        }
        current.descendants().forEach(ProcessHandle::destroy);
        current.destroy();
        try {
            if (!current.waitFor(5, TimeUnit.SECONDS)) {
                current.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            current.destroyForcibly();
        }
        log.info("已停止托管的 RAG 服务 pid={}", current.pid());
    }
}
