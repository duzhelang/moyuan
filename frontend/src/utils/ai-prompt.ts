import type { AiModuleConfig } from '@/api/modules/ai'

interface PromptOptions {
  poetName: string
  isFirst?: boolean
  config?: AiModuleConfig | null
}

const DEFAULT_PROMPT_TEMPLATE = `你是一个古典诗词文化助手，正在为用户介绍诗人{poetName}。
回答要求：
1. 语言简洁精炼，避免冗余
2. 使用通俗易懂的中文
3. 重点突出，条理清晰
4. 不要使用markdown格式，直接输出纯文本
{styleHint}`

export function buildPoetPrompt(userMessage: string, options: PromptOptions): string {
  const { poetName, isFirst = false, config } = options

  let systemConstraint = ''

  if (config?.promptTemplate) {
    const maxLength = isFirst
      ? (config.firstResponseLength || 80)
      : (config.maxLength || 150)

    const styleHint = isFirst
      ? `这是首次提问，请用2-3句话简要概括即可，不超过${config.firstResponseLength || 80}字`
      : `根据问题复杂度适当展开，但不超过${maxLength}字`

    systemConstraint = config.promptTemplate
      .replace(/\{poetName\}/g, poetName)
      .replace(/\{maxLength\}/g, String(maxLength))
      .replace(/\{styleHint\}/g, styleHint)
  } else {
    const styleHint = isFirst
      ? '5. 这是首次提问，请用2-3句话简要概括即可'
      : '5. 根据问题复杂度适当展开，但不超过150字'

    systemConstraint = DEFAULT_PROMPT_TEMPLATE
      .replace(/\{poetName\}/g, poetName)
      .replace(/\{styleHint\}/g, styleHint)
  }

  return `【系统设定】${systemConstraint}\n\n【用户问题】${userMessage}`
}
