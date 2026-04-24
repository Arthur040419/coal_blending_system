-- 示例：Ollama OpenAI 兼容接口（默认 status=0，避免未启动服务时误调用超时）
-- 使用前：1) 启动 Ollama 并拉取模型；2) 将 status 改为 1；3) 按需填写 api_key
INSERT INTO `model_config` (`model_name`, `model_type`, `api_url`, `api_key`, `temperature`, `top_p`, `status`, `remark`)
VALUES
  ('qwen2.5', 'LLM', 'http://127.0.0.1:11434/v1/chat/completions', '', 0.50, 0.90, 0, '示例：启用前请确认本机 11434 可访问');
