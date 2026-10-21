/**
 * 「诗宝阁」AI 对话页 —— 能力（Skill）常量
 *
 * 能力 = 一段任务模板（prompt 片段）+ 期望输出形态。
 * 点击能力 chip 时，把任务模板与用户输入拼成最终 user 消息（角色 system prompt 保持独立）。
 * 其中 acrostic / tone 的模板与解析逻辑直接复用首页现有实现，避免重复造轮子。
 */

/** 结果渲染形态：普通文本 / 平仄严格 JSON / 藏头诗 */
export type SkillRenderKind = 'text' | 'tone' | 'acrostic'

export interface AiSkill {
  /** 能力唯一标识 */
  id: string
  /** 能力名称 */
  name: string
  /** 一句话说明 */
  desc: string
  /** 能力图标（emoji） */
  icon: string
  /** 适用角色 id；空数组表示全部角色可用 */
  roleIds: string[]
  /** 输入框占位提示 */
  placeholder: string
  /** 任务模板：把用户输入拼成最终指令 */
  buildPrompt: (input: string) => string
  /** 结果渲染形态 */
  render: SkillRenderKind
  /** 发送前校验：返回错误文案表示不通过，返回 null 表示通过 */
  validate?: (input: string) => string | null
}

function acrosticHeads(input: string): string[] {
  return Array.from(input.trim()).slice(0, 8)
}

export const SKILLS: AiSkill[] = [
  {
    id: 'compose',
    name: '命题作诗',
    desc: '依主题创作合律诗词',
    icon: '📜',
    roleIds: ['creator'],
    placeholder: '输入主题，如：秋日登高（可附体裁与韵部）',
    render: 'text',
    buildPrompt: (input) =>
      `请以「${input}」为主题创作一首诗。若未指定体裁与韵部，默认作七言绝句、押平声韵。要求：\n`
      + '1. 合平仄、一韵到底；\n'
      + '2. 先给诗作正文，再附一句简要创作思路；\n'
      + '3. 只使用真实存在的典故与意象，不得杜撰出处。'
  },
  {
    id: 'gather',
    name: '集句成诗',
    desc: '化用经典名句成篇',
    icon: '🧵',
    roleIds: ['creator'],
    placeholder: '输入主题或所需意境，如：离别之思',
    render: 'text',
    buildPrompt: (input) =>
      `请围绕「${input}」集句成诗：优先化用经典名句，逐句标注出处（格式：诗句 —— 作者《篇名》）。`
      + '要求：出处必须真实存在，不得杜撰；全篇意境连贯、语气统一。'
  },
  {
    id: 'acrostic',
    name: '藏头诗',
    desc: '输入藏头字，AI 作藏头诗',
    icon: '🖋️',
    roleIds: ['creator'],
    placeholder: '输入 2-8 个藏头字，如：墨渊',
    render: 'acrostic',
    validate: (input) => {
      const heads = acrosticHeads(input)
      if (!heads.length) return '请输入藏头字'
      if (heads.length < 2) return '藏头字至少 2 个'
      return null
    },
    buildPrompt: (input) => {
      const heads = acrosticHeads(input)
      return `请创作一首藏头诗，藏头字依次为「${heads.join('')}」。\n`
        + '要求：\n'
        + `1. 共 ${heads.length} 句，每句第一个字依次为上述藏头字，顺序不可变；\n`
        + '2. 体裁为七言，每句 7 个字；\n'
        + '3. 押平声韵且一韵到底，内容连贯、意境统一、语言典雅；\n'
        + `4. 直接输出诗句，每句一行，共 ${heads.length} 行，不要标题、不要解释、不要任何多余符号。`
    }
  },
  {
    id: 'polish',
    name: '润色入律',
    desc: '按格律改写并说明改动',
    icon: '✨',
    roleIds: ['creator', 'judge'],
    placeholder: '粘贴待改写的诗句',
    render: 'text',
    buildPrompt: (input) =>
      '请将下面的诗句按近体诗格律改写为合律版本，逐处说明改动理由。'
      + `若未指定体裁，请按原句字数推断（五言/七言）：\n${input}`
  },
  {
    id: 'tone',
    name: '平仄校验',
    desc: '校验诗句平仄与押韵',
    icon: '🧭',
    roleIds: ['judge'],
    placeholder: '粘贴一首绝句/律诗（每句一行）',
    render: 'tone',
    buildPrompt: (input) =>
      '你是精通《平水韵》与近体诗格律的专家。请校验下面这首诗的平仄格律与押韵情况。\n'
      + `待校验诗作：\n${input}\n\n`
      + '要求：只输出如下严格 JSON，不要任何解释文字、不要 markdown 代码块标记：\n'
      + '{"valid":true或false,"format":"体裁，如 七言绝句","rhymeGroup":"押韵所属平水韵韵部，如 下平八庚；不押韵填 无",'
      + '"lines":[{"text":"该句原文（含标点）","tone":"逐字平仄，仅含 平/仄 两种字符并忽略标点，如 平仄平平仄仄平","rhyme":true或false,"note":"该句简评，若不合律请指出问题"}],'
      + '"summary":"总体格律评价，1-2 句"}'
  },
  {
    id: 'rhyme',
    name: '韵脚查询',
    desc: '查询汉字所属韵部与同韵字',
    icon: '🎵',
    roleIds: ['judge'],
    placeholder: '输入汉字或韵部，如：风 / 东',
    render: 'text',
    buildPrompt: (input) =>
      `请查询「${input}」在《平水韵》中的归属：\n`
      + '1. 若输入为汉字，说明它所属的平水韵韵部（如 上平一东）与声调；\n'
      + '2. 若输入为韵部名称，列出该韵部的常用字；\n'
      + '3. 可附带 2-3 句以该韵部押韵的经典诗句作为示例；\n'
      + '4. 韵部归属必须准确，不确定处如实说明，不得杜撰。'
  },
  {
    id: 'couplet',
    name: '对对联',
    desc: '上联出题，AI 对下联',
    icon: '🏮',
    roleIds: ['creator'],
    placeholder: '输入上联，如：春风得意马蹄疾',
    render: 'text',
    buildPrompt: (input) =>
      `请为下面的上联对出下联：\n${input}\n\n`
      + '要求：\n'
      + '1. 下联与上联字数相等、词性相对、平仄相谐；\n'
      + '2. 意境连贯、语气统一，避免生硬拼凑；\n'
      + '3. 先给出下联正文，再用一句话说明对仗思路。'
  },
  {
    id: 'imagery',
    name: '意象典故',
    desc: '解读意象与典故流变',
    icon: '🎴',
    roleIds: ['critic'],
    placeholder: '输入意象，如：明月、孤舟',
    render: 'text',
    buildPrompt: (input) =>
      `请围绕意象「${input}」列举 3-5 句经典名句，并解读其情感内涵与意象流变。`
      + '名句请标注作者与篇名，务必真实，不得杜撰。'
  },
  {
    id: 'comment',
    name: '诗话点评',
    desc: '以诗话口吻分点点评',
    icon: '📖',
    roleIds: ['critic'],
    placeholder: '粘贴一首待点评的诗',
    render: 'text',
    buildPrompt: (input) =>
      '请以宋人诗话口吻点评下面这首诗，分点评述、鞭辟入里，末尾附一句总评：\n' + input
  },
  {
    id: 'feihua',
    name: '飞花令',
    desc: '设令字对句，逐句判定',
    icon: '🌸',
    roleIds: ['friend'],
    placeholder: '输入一个汉字作为令字，如：花',
    render: 'text',
    buildPrompt: (input) =>
      `请以「${input}」为令字，列出含该字的真实诗词名句。`
      + '每次给出 1-2 句，格式为「诗句 —— 作者《篇名》」，严禁杜撰。'
  },
  {
    id: 'chain',
    name: '诗词接龙',
    desc: '据句末字接下一句',
    icon: '🔗',
    roleIds: ['friend'],
    placeholder: '输入上一句诗，或直接输入首字',
    render: 'text',
    buildPrompt: (input) =>
      `请以「${input}」为首字，接出下一句真实存在的诗词名句，`
      + '格式为「诗句 —— 作者《篇名》」，严禁杜撰。'
  },
  {
    id: 'salon',
    name: '群贤论诗',
    desc: '众诗人依次发言，主持收尾',
    icon: '🏮',
    roleIds: ['salon'],
    placeholder: '输入论诗话题，如：何为诗中「气」',
    render: 'text',
    buildPrompt: (input) =>
      `请就话题「${input}」主持一场论诗雅集：分别以李白、杜甫、苏轼等诗人第一人称发表见解，`
      + '各具性情与笔法，最后由你作简短总结。'
  }
]

export function getSkillById(id: string): AiSkill | undefined {
  return SKILLS.find((s) => s.id === id)
}

/** 按角色筛选可用能力（roleIds 为空表示全角色可用） */
export function getSkillsByRole(roleId: string): AiSkill[] {
  return SKILLS.filter((s) => s.roleIds.length === 0 || s.roleIds.includes(roleId))
}
