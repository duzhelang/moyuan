import type { AiModuleConfig } from '@/api/modules/ai'
import type { AiContext } from '@/configs/ai-chat/pageProfiles'

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

/**
 * 全站 AI 小窗（诗词助理）的提示词拼装
 * 复用后台「AI 模块配置」中维护的 promptTemplate，并按页面上下文替换变量
 *
 * @param userMessage 用户问题
 * @param ctx 页面上下文（诗人 / 诗词）
 * @param config 模块配置，为空时使用内置兜底模板
 * @param isFirst 是否为该页首次提问（影响回答长度）
 */
export function buildPoetryPrompt(
  userMessage: string,
  ctx: AiContext | null,
  config: AiModuleConfig | null,
  isFirst = false
): string {
  const poetName = ctx?.poetName || (ctx?.type === 'poet' ? ctx?.name : '') || ctx?.author || '该诗人'
  const poemTitle = ctx && ctx.type === 'poem' && ctx.name ? ctx.name : '该诗词'
  const poemContent = ctx?.content || ''

  let systemConstraint = ''
  if (config?.promptTemplate) {
    const maxLength = isFirst ? (config.firstResponseLength || 100) : (config.maxLength || 200)
    const styleHint = isFirst
      ? `这是首次提问，请用2-3句话简要概括即可，不超过${config.firstResponseLength || 100}字`
      : `根据问题复杂度适当展开，但不超过${maxLength}字`

    systemConstraint = config.promptTemplate
      .replace(/\{poetName\}/g, poetName)
      .replace(/\{poemTitle\}/g, poemTitle)
      .replace(/\{poemContent\}/g, poemContent)
      .replace(/\{maxLength\}/g, String(maxLength))
      .replace(/\{styleHint\}/g, styleHint)

    if (ctx?.type === 'poem' && poemTitle !== '该诗词' && !systemConstraint.includes(poemTitle)) {
      systemConstraint += `\n\n当前诗词：《${poemTitle}》${ctx.author ? ' - ' + ctx.author : ''}\n诗词内容：${poemContent}`
    }
  } else {
    const maxLength = isFirst ? 100 : 200
    systemConstraint = `你是一个古典诗词文化助手，精通古今中外诗歌知识。
回答要求：
1. 语言简洁精炼，避免冗余
2. 使用通俗易懂的中文
3. 重点突出，条理清晰
4. 不要使用markdown格式，直接输出纯文本
5. ${isFirst ? '这是首次提问，请用2-3句话简要概括即可' : '根据问题复杂度适当展开，但不超过' + maxLength + '字'}`
    if (ctx?.type === 'poem' && poemTitle !== '该诗词') {
      systemConstraint += `\n\n当前诗词：《${poemTitle}》${ctx.author ? ' - ' + ctx.author : ''}\n诗词内容：${poemContent}`
    } else if (ctx?.type === 'poet' && poetName !== '该诗人') {
      systemConstraint += `\n\n当前诗人：${poetName}`
    }
  }

  return `【系统设定】${systemConstraint}\n\n【用户问题】${userMessage}`
}

/**
 * 全站 AI 小窗（网页用法助理）的提示词拼装
 * 基础规则来自 system_chat 模块配置（后台可维护），其后追加检索到的【系统资料】
 */
export function buildSystemPrompt(
  userMessage: string,
  config: AiModuleConfig | null,
  knowledgeContext: string
): string {
  const maxLength = config?.maxLength || 300
  const template = config?.promptTemplate
    ? config.promptTemplate.replace(/\{maxLength\}/g, String(maxLength))
    : `你是「墨渊」的系统用法小助理，只依据【系统资料】回答本系统相关问题（页面在哪、玩法怎么玩、功能怎么用、规则是什么）。
规则：
1) 只依据【系统资料】作答，资料中没有的内容直接回答「我暂时没有这方面信息」，禁止编造页面、入口或功能；
2) 不要回答诗词内容、诗人介绍、诗词赏析类问题，遇到这类问题提示用户切换「诗词助理」；
3) 回答简洁，先给结论再给步骤，不超过${maxLength}字；
4) 不使用 markdown 格式，直接输出纯文本；
5) 涉及可体验的功能时，可在回答末尾另起一行输出 [[路由]] 标记（路由必须取自【系统资料】中给出的关联入口），前端会渲染成可点击入口。`

  const material = knowledgeContext
    ? `\n\n【系统资料】\n${knowledgeContext}`
    : '\n\n【系统资料】\n（本次没有检索到相关资料，请回答「我暂时没有这方面信息」）'

  return `【系统设定】${template}${material}\n\n【用户问题】${userMessage}`
}
