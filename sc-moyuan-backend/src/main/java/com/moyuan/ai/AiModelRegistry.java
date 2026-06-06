package com.moyuan.ai;

import com.moyuan.entity.AiModel;
import com.moyuan.exception.BusinessException;
import com.moyuan.common.ResultCode;
import com.moyuan.mapper.AiModelMapper;
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
    private final List<AiModelAdapter> adapters;
    private final Map<String, AiModel> modelCache = new ConcurrentHashMap<>();

    public AiModelRegistry(AiModelMapper aiModelMapper, List<AiModelAdapter> adapters) {
        this.aiModelMapper = aiModelMapper;
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

    public String chat(String message, String modelName, String systemPrompt) {
        AiModel model = modelName != null ? getModel(modelName) : getDefaultModel();
        AiModelAdapter adapter = getAdapter(model.getProvider());
        return adapter.chat(message, model, systemPrompt);
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