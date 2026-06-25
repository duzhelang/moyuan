package com.moyuan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyuan.ai.AiModelRegistry;
import com.moyuan.entity.AiModel;
import com.moyuan.entity.AiModuleConfig;
import com.moyuan.exception.BusinessException;
import com.moyuan.common.ResultCode;
import com.moyuan.mapper.AiModelMapper;
import com.moyuan.mapper.AiModuleConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelConfigService {

    private final AiModelMapper aiModelMapper;
    private final AiModuleConfigMapper aiModuleConfigMapper;
    private final AiModelRegistry aiModelRegistry;

    public List<AiModel> getAllModels() {
        return aiModelMapper.selectList(
                new LambdaQueryWrapper<AiModel>().orderByAsc(AiModel::getSortOrder)
        );
    }

    public List<AiModel> getEnabledModels() {
        return aiModelMapper.selectList(
                new LambdaQueryWrapper<AiModel>()
                        .eq(AiModel::getIsEnabled, 1)
                        .orderByAsc(AiModel::getSortOrder)
        );
    }

    public AiModel getModelById(Long id) {
        AiModel model = aiModelMapper.selectById(id);
        if (model == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "AI模型不存在");
        }
        return model;
    }

    @Transactional
    public AiModel createModel(AiModel model) {
        if (Integer.valueOf(1).equals(model.getIsDefault())) {
            clearDefaultModel();
        }
        if (model.getIsEnabled() == null) {
            model.setIsEnabled(1);
        }
        if (model.getIsDefault() == null) {
            model.setIsDefault(0);
        }
        aiModelMapper.insert(model);
        aiModelRegistry.refreshCache();
        return model;
    }

    @Transactional
    public AiModel updateModel(AiModel model) {
        getModelById(model.getId());
        if (Integer.valueOf(1).equals(model.getIsDefault())) {
            clearDefaultModel();
        }
        model.setUpdateTime(null);
        aiModelMapper.updateById(model);
        aiModelRegistry.refreshCache();
        return model;
    }

    @Transactional
    public void deleteModel(Long id) {
        aiModelMapper.deleteById(id);
        aiModelRegistry.refreshCache();
    }

    @Transactional
    public void toggleModel(Long id) {
        AiModel model = getModelById(id);
        model.setIsEnabled(model.getIsEnabled() == 1 ? 0 : 1);
        aiModelMapper.updateById(model);
        aiModelRegistry.refreshCache();
    }

    public String testConnection(Long id) {
        AiModel model = getModelById(id);
        return aiModelRegistry.chat("你好，请回复ok确认连接正常", model.getName(), null);
    }

    @Transactional
    public void setDefaultModel(Long id) {
        clearDefaultModel();
        AiModel model = getModelById(id);
        model.setIsDefault(1);
        aiModelMapper.updateById(model);
        aiModelRegistry.refreshCache();
    }

    private void clearDefaultModel() {
        AiModel defaultModel = aiModelMapper.selectOne(
                new LambdaQueryWrapper<AiModel>().eq(AiModel::getIsDefault, 1)
        );
        if (defaultModel != null) {
            defaultModel.setIsDefault(0);
            aiModelMapper.updateById(defaultModel);
        }
    }

    public List<AiModuleConfig> getAllModuleConfigs() {
        return aiModuleConfigMapper.selectList(null);
    }

    public AiModuleConfig getModuleConfigByCode(String moduleCode) {
        AiModuleConfig config = aiModuleConfigMapper.selectOne(
                new LambdaQueryWrapper<AiModuleConfig>().eq(AiModuleConfig::getModuleCode, moduleCode)
        );
        if (config == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "模块配置不存在");
        }
        return config;
    }

    @Transactional
    public AiModuleConfig updateModuleConfig(AiModuleConfig config) {
        AiModuleConfig existing = aiModuleConfigMapper.selectOne(
                new LambdaQueryWrapper<AiModuleConfig>().eq(AiModuleConfig::getModuleCode, config.getModuleCode())
        );
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "模块配置不存在");
        }
        if (config.getModelId() != null) existing.setModelId(config.getModelId());
        if (config.getModuleName() != null) existing.setModuleName(config.getModuleName());
        if (config.getDescription() != null) existing.setDescription(config.getDescription());
        if (config.getPromptTemplate() != null) existing.setPromptTemplate(config.getPromptTemplate());
        if (config.getMaxLength() != null) existing.setMaxLength(config.getMaxLength());
        if (config.getResponseStyle() != null) existing.setResponseStyle(config.getResponseStyle());
        if (config.getFirstResponseLength() != null) existing.setFirstResponseLength(config.getFirstResponseLength());
        if (config.getEnableMarkdown() != null) existing.setEnableMarkdown(config.getEnableMarkdown());
        existing.setUpdateTime(null);
        aiModuleConfigMapper.updateById(existing);
        aiModelRegistry.refreshCache();
        return existing;
    }

    public List<AiModel> getModelsForModule(String moduleCode) {
        AiModuleConfig config = aiModuleConfigMapper.selectOne(
                new LambdaQueryWrapper<AiModuleConfig>().eq(AiModuleConfig::getModuleCode, moduleCode)
        );
        if (config == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "模块配置不存在");
        }
        LambdaQueryWrapper<AiModel> wrapper = new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getIsEnabled, 1);
        if (Integer.valueOf(1).equals(config.getRequireVision())) {
            wrapper.and(w -> w
                    .eq(AiModel::getModelType, "vision")
                    .or()
                    .eq(AiModel::getModelType, "both")
            );
        }
        wrapper.orderByAsc(AiModel::getSortOrder);
        return aiModelMapper.selectList(wrapper);
    }
}