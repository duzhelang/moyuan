package com.moyuan.controller;

import com.moyuan.common.R;
import com.moyuan.entity.AiModel;
import com.moyuan.entity.AiModuleConfig;
import com.moyuan.service.AiModelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "AI模型管理接口")
@RestController
@RequestMapping("/api/admin/ai-models")
@RequiredArgsConstructor
public class AiModelConfigController {

    private final AiModelConfigService aiModelConfigService;

    @Operation(summary = "获取所有模型")
    @GetMapping
    public R<List<AiModel>> getAllModels() {
        return R.success(aiModelConfigService.getAllModels());
    }

    @Operation(summary = "获取启用的模型")
    @GetMapping("/enabled")
    public R<List<AiModel>> getEnabledModels() {
        return R.success(aiModelConfigService.getEnabledModels());
    }

    @Operation(summary = "获取所有模块配置")
    @GetMapping("/modules")
    public R<List<AiModuleConfig>> getAllModuleConfigs() {
        return R.success(aiModelConfigService.getAllModuleConfigs());
    }

    @Operation(summary = "获取模块可用模型列表")
    @GetMapping("/modules/{moduleCode}/models")
    public R<List<AiModel>> getModelsForModule(@PathVariable String moduleCode) {
        return R.success(aiModelConfigService.getModelsForModule(moduleCode));
    }

    @Operation(summary = "获取模型详情")
    @GetMapping("/{id}")
    public R<AiModel> getModelById(@PathVariable Long id) {
        return R.success(aiModelConfigService.getModelById(id));
    }

    @Operation(summary = "创建模型")
    @PostMapping
    public R<AiModel> createModel(@RequestBody AiModel model) {
        return R.success(aiModelConfigService.createModel(model));
    }

    @Operation(summary = "更新模型")
    @PutMapping("/{id}")
    public R<AiModel> updateModel(@PathVariable Long id, @RequestBody AiModel model) {
        model.setId(id);
        return R.success(aiModelConfigService.updateModel(model));
    }

    @Operation(summary = "删除模型")
    @DeleteMapping("/{id}")
    public R<Void> deleteModel(@PathVariable Long id) {
        aiModelConfigService.deleteModel(id);
        return R.success(null);
    }

    @Operation(summary = "启用/禁用模型")
    @PostMapping("/{id}/toggle")
    public R<Void> toggleModel(@PathVariable Long id) {
        aiModelConfigService.toggleModel(id);
        return R.success(null);
    }

    @Operation(summary = "设置默认模型")
    @PostMapping("/{id}/set-default")
    public R<Void> setDefaultModel(@PathVariable Long id) {
        aiModelConfigService.setDefaultModel(id);
        return R.success(null);
    }

    @Operation(summary = "获取提供商列表")
    @GetMapping("/providers")
    public R<List<Map<String, String>>> getProviders() {
        List<Map<String, String>> providers = new ArrayList<>();
        String[][] data = {
                {"zhipu", "智谱AI"},
                {"deepseek", "DeepSeek"},
                {"kimi", "Kimi (月之暗面)"},
                {"nvidia", "NVIDIA NIM"},
                {"qwen", "千问"},
                {"mimo", "小米MiMo"},
                {"openrouter", "OpenRouter"}
        };
        for (String[] item : data) {
            Map<String, String> provider = new HashMap<>();
            provider.put("value", item[0]);
            provider.put("label", item[1]);
            providers.add(provider);
        }
        return R.success(providers);
    }

    @Operation(summary = "测试模型连接")
    @PostMapping("/{id}/test")
    public R<String> testConnection(@PathVariable Long id) {
        try {
            String result = aiModelConfigService.testConnection(id);
            return R.success(result);
        } catch (Exception e) {
            return R.error("连接测试失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新模块配置")
    @PutMapping("/modules/{moduleCode}")
    public R<AiModuleConfig> updateModuleConfig(@PathVariable String moduleCode, @RequestBody AiModuleConfig config) {
        config.setModuleCode(moduleCode);
        return R.success(aiModelConfigService.updateModuleConfig(config));
    }
}