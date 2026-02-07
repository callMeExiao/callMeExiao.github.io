# AGENT.md

## 沟通约定
- 总是用简体中文回复。
- 不确定需求时先发问澄清，避免猜测。
- 关键假设必须显式说明。
- 输出先给结论，再给细节。
- 代码改动必须标注影响范围（文件/模块）。
- 涉及风险操作（删除/重置/覆盖）先确认。
- 发现异常或不一致立即停下并告知。
- 仅在必要时使用英文术语，并附中文说明。
- 当需要库/API 文档、代码生成、安装或配置步骤时，即使我未明确要求，也必须使用 Context7 MCP（MCP 文档与生成工具）。

## 项目概览
- 本项目是基于 VuePress 2 与 vuepress-theme-plume 的静态站点。
- 文档源内容位于 `docs/`，站点配置位于 `docs/.vuepress/config.ts`。

## 环境要求
- Node.js `^18.20.0` 或 `>=20.0.0`。

## 常用命令
- `npm i` 安装依赖
- `npm run docs:dev` 启动本地开发服务
- `npm run docs:dev-clean` 清理缓存后启动开发服务
- `npm run docs:build` 生成生产构建
- `npm run docs:preview` 预览生产构建
- `npm run vp-update` 更新 VuePress 与主题

## 构建产物
- 输出目录：`docs/.vuepress/dist`

## 修改建议
- 内容更新优先在 `docs/` 目录内完成。
- 若改动 `docs/.vuepress/config.ts` 或依赖版本，请说明影响范围与验证方式。

## 测试/验证
- 项目暂无自动化测试；建议至少执行 `npm run docs:build` 或本地 `npm run docs:dev` 进行人工验证。
