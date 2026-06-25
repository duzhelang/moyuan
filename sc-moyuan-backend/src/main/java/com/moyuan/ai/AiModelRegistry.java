package com.moyuan.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyuan.entity.AiModel;
import com.moyuan.entity.AiModuleConfig;
import com.moyuan.exception.BusinessException;
import com.moyuan.common.ResultCode;
import com.moyuan.mapper.AiModelMapper;
import com.moyuan.mapper.AiModuleConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AiModelRegistry {

    private final AiModelMapper aiModelMapper;
    private final AiModuleConfigMapper aiModuleConfigMapper;
    private final List<AiModelAdapter> adapters;
    private final Map<String, AiModel> modelCache = new ConcurrentHashMap<>();
    private final Map<String, AiModuleConfig> moduleConfigCache = new ConcurrentHashMap<>();

    public AiModelRegistry(AiModelMapper aiModelMapper, AiModuleConfigMapper aiModuleConfigMapper, List<AiModelAdapter> adapters) {
        this.aiModelMapper = aiModelMapper;
        this.aiModuleConfigMapper = aiModuleConfigMapper;
        this.adapters = adapters;
    }

    @PostConstruct
    public void init() {
        refreshCache();
    }

    public void refreshCache() {
        modelCache.clear();
        List<AiModel> models = aiModelMapper.selectList(null);
        for (AiModel model : models) {
            if (model.getIsEnabled() == 1) {
                modelCache.put(model.getName(), model);
            }
        }
        log.info("AI模型缓存已刷新，共加载 {} 个模型", modelCache.size());

        moduleConfigCache.clear();
        List<AiModuleConfig> configs = aiModuleConfigMapper.selectList(null);
        for (AiModuleConfig config : configs) {
            moduleConfigCache.put(config.getModuleCode(), config);
        }
        log.info("AI模块配置缓存已刷新，共加载 {} 个模块", moduleConfigCache.size());
    }

    public AiModel getModel(String name) {
        AiModel model = modelCache.get(name);
        if (model == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的AI模型: " + name);
        }
        return model;
    }

    public AiModel getDefaultModel() {
        return modelCache.values().stream()
                .filter(m -> m.getIsDefault() == 1)
                .findFirst()
                .orElse(modelCache.values().stream().findFirst()
                        .orElseThrow(() -> new BusinessException(ResultCode.ERROR, "没有可用的AI模型")));
    }

    public List<AiModel> getAllEnabledModels() {
        return List.copyOf(modelCache.values());
    }

    public AiModelAdapter getAdapter(String provider) {
        return adapters.stream()
                .filter(adapter -> adapter.supports(provider))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.ERROR, "不支持的AI提供商: " + provider));
    }

    public AiModuleConfig getModuleConfig(String moduleCode) {
        return moduleConfigCache.get(moduleCode);
    }

    public AiModel getModelByModuleCode(String moduleCode) {
        AiModuleConfig config = moduleConfigCache.get(moduleCode);
        if (config == null || config.getModelId() == null) {
            return getDefaultModel();
        }
        return modelCache.values().stream()
                .filter(m -> m.getId().equals(config.getModelId()))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("模块 {} 配置的模型ID {} 不存在或已禁用，使用默认模型", moduleCode, config.getModelId());
                    return getDefaultModel();
                });
    }

    public AiModel getModelForModule(String moduleCode) {
        if (moduleCode == null || moduleCode.isEmpty()) {
            return getDefaultModel();
        }
        return getModelByModuleCode(moduleCode);
    }

    public String chat(String message, String modelName, String systemPrompt) {
        AiModel model = modelName != null ? getModel(modelName) : getDefaultModel();
        AiModelAdapter adapter = getAdapter(model.getProvider());
        return adapter.chat(message, model, systemPrompt);
    }

    public String chatByModule(String message, String moduleCode, String systemPrompt) {
        AiModel model = getModelByModuleCode(moduleCode);
        AiModuleConfig config = getModuleConfig(moduleCode);
        String effectivePrompt = systemPrompt;
        if (config != null && config.getPromptTemplate() != null && !config.getPromptTemplate().isEmpty()) {
            effectivePrompt = config.getPromptTemplate();
        }
        AiModelAdapter adapter = getAdapter(model.getProvider());
        return adapter.chat(message, model, effectivePrompt);
    }

    public String visionByModule(String prompt, String base64Image, String moduleCode, String visionModelName, String systemPrompt) {
        AiModel model = getModelByModuleCode(moduleCode);
        AiModuleConfig config = getModuleConfig(moduleCode);
        String effectivePrompt = systemPrompt;
        if (config != null && config.getPromptTemplate() != null && !config.getPromptTemplate().isEmpty()) {
            effectivePrompt = config.getPromptTemplate();
        }
        AiModelAdapter adapter = getAdapter(model.getProvider());

        if (visionModelName != null && !visionModelName.isEmpty()) {
            AiModel visionModel = new AiModel();
            visionModel.setId(model.getId());
            visionModel.setName(model.getName());
            visionModel.setDisplayName(model.getDisplayName());
            visionModel.setProvider(model.getProvider());
            visionModel.setModelType(model.getModelType());
            visionModel.setApiUrl(model.getApiUrl());
            visionModel.setApiKey(model.getApiKey());
            visionModel.setModelId(visionModelName);
            visionModel.setVisionModelId(visionModelName);
            visionModel.setMaxTokens(model.getMaxTokens());
            visionModel.setExtraConfig(model.getExtraConfig());
            visionModel.setIsEnabled(model.getIsEnabled());
            visionModel.setIsDefault(model.getIsDefault());
            visionModel.setSortOrder(model.getSortOrder());
            visionModel.setCreateTime(model.getCreateTime());
            visionModel.setUpdateTime(model.getUpdateTime());
            return adapter.vision(prompt, base64Image, visionModel, effectivePrompt);
        }

        return adapter.vision(prompt, base64Image, model, effectivePrompt);
    }

    public String vision(String prompt, String base64Image, String modelName, String visionModelName, String systemPrompt) {
        AiModel model = modelName != null ? getModel(modelName) : getDefaultModel();
        AiModelAdapter adapter = getAdapter(model.getProvider());

        if (visionModelName != null && !visionModelName.isEmpty()) {
            AiModel visionModel = new AiModel();
            visionModel.setId(model.getId());
            visionModel.setName(model.getName());
            visionModel.setDisplayName(model.getDisplayName());
            visionModel.setProvider(model.getProvider());
            visionModel.setModelType(model.getModelType());
            visionModel.setApiUrl(model.getApiUrl());
            visionModel.setApiKey(model.getApiKey());
            visionModel.setModelId(visionModelName);
            visionModel.setVisionModelId(visionModelName);
            visionModel.setMaxTokens(model.getMaxTokens());
            visionModel.setExtraConfig(model.getExtraConfig());
            visionModel.setIsEnabled(model.getIsEnabled());
            visionModel.setIsDefault(model.getIsDefault());
            visionModel.setSortOrder(model.getSortOrder());
            visionModel.setCreateTime(model.getCreateTime());
            visionModel.setUpdateTime(model.getUpdateTime());
            return adapter.vision(prompt, base64Image, visionModel, systemPrompt);
        }

        return adapter.vision(prompt, base64Image, model, systemPrompt);
    }
}