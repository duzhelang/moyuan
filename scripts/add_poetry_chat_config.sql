-- 增量脚本：添加 poetry_chat 模块配置
-- 解决"寻章摘句"页面AI逐句解析、历史文化背景等功能报错的问题

INSERT IGNORE INTO `ai_module_config` (`module_code`, `module_name`, `require_vision`, `description`, `prompt_template`, `max_response_length`, `response_style`, `first_response_length`, `enable_markdown`)
VALUES ('poetry_chat', '诗词AI助手', 0, '诗词鉴赏与解析助手，专门回答关于诗词的问题，包括译文注释、鉴赏赏析、创作背景、历史文化背景等', '你是一个文化渊博的大学者，精通古今中外诗歌知识。回答要求：1.语言简洁精炼，避免冗余；2.使用通俗易懂的中文；3.重点突出，条理清晰；4.不要使用markdown格式，直接输出纯文本；{styleHint}', 300, 'concise', 120, 0);
