---
title: React 组件基础
createTime: 2026/02/09 15:51:41
permalink: /article/j3yf2tp3/
---

# React 组件基础（重点函数式）

---

## 1. 组件是什么

在 React 里，组件（Component）是 UI 的最小可复用单元。你可以把页面看成组件树：

- 页面组件负责整体结构
- 业务组件负责某个功能块
- 通用组件负责复用能力（弹窗、表格、按钮、加载态等）

组件化的意义是：拆分复杂度、提高复用率、降低维护成本。

### Java 工程师可先这样理解

- React 组件 ≈ Java 里的“对象 + `render()` 方法”：输入变了就重新渲染。
- JSX ≈ 模板引擎（JSP/Thymeleaf）的内联写法，但和逻辑代码写在一起。
- `props` ≈ 方法参数/构造参数：由外部传入，组件内部只读。
- `state` ≈ 对象内部字段：但必须通过 `setState`（如 `setCount`）更新，才能驱动 UI 刷新。
- `useEffect` ≈ 生命周期回调 + 资源清理（可类比 `@PostConstruct` + `@PreDestroy`）。

---

## 2. 为什么函数式组件是主流

React 早期常见类组件（Class Component），现在主流是函数式组件（Function Component）+ Hooks。

函数式组件的优势：

1. 代码更简洁，没有 `this` 绑定问题。
2. 逻辑复用更自然（可抽成自定义 Hook）。
3. 副作用与状态管理更清晰（`useEffect`、`useState` 等）。

一个最小函数式组件：

在 React 17+（自动 JSX Runtime）中，通常不再需要手动 `import React`。

```tsx
export function Hello() {
  return <h1>你好，React</h1>;
}
```

---

## 3. JSX：组件返回的“界面描述”

JSX 可以先把它理解成：**“在 JavaScript 里写 HTML 外观的 UI 模板语法”**。

Java 类比：

- JSP/Thymeleaf 是“模板文件 + 表达式”。
- JSX 是“模板语法直接写在组件函数里”。

### 3.1 JSX 到底是什么（不是字符串）

这段：

```tsx
const view = <h1>Hello</h1>;
```

不是在创建 HTML 字符串，而是在创建“元素描述对象”。
编译后会变成类似：

```tsx
const view = React.createElement("h1", null, "Hello");
```

所以 JSX 本质是语法糖，帮助你更直观地描述 UI 结构。

### 3.2 `{}` 里能放什么

`{}` 里放的是**表达式**，不是语句。

✅ 可以放：变量、三元表达式、函数调用结果、数组 `map` 结果。  
❌ 不能直接放：`if`、`for`、`switch` 这些语句。

```tsx
const name = "小明";
const isVip = true;

return (
  <div>
    <p>{name}</p>
    <p>{isVip ? "VIP 用户" : "普通用户"}</p>
    <p>{new Date().toLocaleDateString()}</p>
  </div>
);
```

如果你想写 `if`，通常在 `return` 上面先算好变量，再放进 JSX。

### 3.3 属性写法为什么和 HTML 不一样

JSX 更接近 JavaScript 对象命名规则：

- `class` -> `className`
- `for` -> `htmlFor`
- 事件用驼峰：`onclick` -> `onClick`
- `style` 用对象：`style={{ color: "red", fontSize: 14 }}`

```tsx
return (
  <label htmlFor="kw" className="search-label" style={{ color: "#333" }}>
    关键词
  </label>
);
```

### 3.4 为什么必须“单一根节点”

组件的 `return` 需要返回一个整体。
如果你想并列返回多个元素，用一个根包起来，或用 Fragment：

```tsx
return (
  <>
    <h3>标题</h3>
    <p>内容</p>
  </>
);
```

Java 类比：方法返回值只能是一个对象；Fragment 相当于“逻辑容器”，不会多渲染一层真实 DOM。

### 3.5 一个完整、常见的 JSX 示例

```tsx
const name = "小明";
const tasks = ["写周报", "提测", "代码评审"];

return (
  <section>
    <h3 className="title">欢迎你，{name}</h3>
    <ul>
      {tasks.map((task) => (
        <li key={task}>{task}</li>
      ))}
    </ul>
  </section>
);
```

### 3.6 JSX 常见报错速记

1. **忘记闭合标签**：`<input>` 在 JSX 中通常要写成 `<input />`。  
2. **返回多个平级元素未包裹**：要加 `<div>` 或 `<>...</>`。  
3. **在 JSX 里直接写 `if`**：改用三元表达式，或提前在 `return` 外处理。

---

## 4. Props：组件的输入

`props` 是父组件传给子组件的输入。你可以把子组件当成“根据输入返回 UI 的函数”。

Java 类比：

- `props` ≈ 方法参数 / 构造参数
- 子组件 ≈ 被调用方
- 父组件重新传参 ≈ 再次调用方法并传入新参数

### 4.1 最小示例：只读输入

```tsx
type UserCardProps = {
  name: string;
  age?: number;
};

function UserCard({ name, age = 18 }: UserCardProps) {
  return <div>{name} - {age}</div>;
}
```

这个例子里：

- `name` 是必填参数
- `age` 是可选参数
- `age = 18` 是默认值

### 4.2 为什么 `props` 必须只读

React 的核心是单向数据流：**父 -> 子**。
如果子组件直接改 `props`，会导致“数据到底由谁维护”变得混乱。

所以正确方式是：

1. 父组件维护状态
2. 通过 `props` 传给子组件
3. 子组件只渲染，不直接改

### 4.3 常见实战模式：数据下行 + 事件上行

```tsx
import { useState } from "react";

type CounterPanelProps = {
  count: number;
  onIncrement: () => void;
};

function CounterPanel({ count, onIncrement }: CounterPanelProps) {
  return <button onClick={onIncrement}>+1（当前：{count}）</button>;
}

export function CounterPage() {
  const [count, setCount] = useState(0);

  return (
    <CounterPanel
      count={count}
      onIncrement={() => setCount((prev) => prev + 1)}
    />
  );
}
```

这里的思路：

- `count` 从父组件传给子组件（数据下行）
- 子组件触发 `onIncrement` 通知父组件（事件上行）
- 父组件更新状态后再把新值传下去

Java 类比：父组件像 Service，子组件像调用链下游；数据向下传，回调向上抛。

### 4.4 TypeScript 下 props 的常见写法

- 必填字段：`name: string`
- 可选字段：`age?: number`
- 默认值：`function A({ age = 18 }: Props) {}`
- 回调签名：`onChange: (value: string) => void`
- 插槽内容：`children: ReactNode`

### 4.5 两个高频误区

1. **直接修改 props（错误）**

```tsx
function Bad({ count }: { count: number }) {
  // count += 1; // ❌ 不要这样做
  return <span>{count}</span>;
}
```

2. **把 props 无脑拷贝到 state**

`const [x, setX] = useState(props.x)` 只在初始化用一次，后续父组件更新 `props.x` 时，`x` 不会自动同步。

只有在你明确要“本地编辑副本”时，才考虑这样做。

### 4.6 一句话记忆

`props` 解决的是“组件输入”；`state` 解决的是“组件内部可变状态”。

---

## 5. State：组件的内部状态（`useState`）

`state` 用于描述“会变化并触发视图更新”的数据。

```tsx
import { useState } from "react";

function Counter() {
  const [count, setCount] = useState(0);

  return (
    <button onClick={() => setCount((prev) => prev + 1)}>
      点击次数：{count}
    </button>
  );
}
```

要点：

- 调用 `setXxx` 会触发重新渲染。
- 更新依赖旧值时，用函数式更新 `setState(prev => ...)`。
- 不要直接改对象/数组本身，要创建新引用再更新。

Java 类比：把 `state` 想成类字段没问题，但要记住 React 不是靠你“改字段”刷新页面，而是靠 `setXxx` 触发渲染流程。

---

## 6. 事件处理：用户交互入口

React 事件名使用驼峰，如 `onClick`、`onChange`。

```tsx
import { useState } from "react";

function SearchBox() {
  const [keyword, setKeyword] = useState("");

  const handleSubmit = () => {
    console.log("搜索词", keyword);
  };

  return (
    <>
      <input value={keyword} onChange={(e) => setKeyword(e.target.value)} />
      <button onClick={handleSubmit}>搜索</button>
    </>
  );
}
```

要点：

- 事件处理函数是“传函数”，不是“立即执行结果”。
- 表单类组件通常采用受控模式（`value + onChange`）。

---

## 7. 渲染逻辑：条件渲染与列表渲染

### 7.1 条件渲染

常见写法：

- 三元表达式：`condition ? A : B`
- 与运算：`condition && A`

### 7.2 列表渲染

```tsx
{list.map((item) => (
  <li key={item.id}>{item.name}</li>
))}
```

`key` 是列表 diff 的关键标识，要求：

- 稳定
- 唯一
- 尽量使用业务 id，不建议用数组索引（尤其在可重排列表中）

---

## 8. 副作用与“生命周期思维”（`useEffect`）

函数式组件没有类组件生命周期函数，但可以用 `useEffect` 表达“渲染后执行副作用”的行为。

先记住一句话：**依赖变更时，React 会先清理旧 effect，再执行新 effect。**

```tsx
import { useEffect, useState } from "react";

async function fetchUsers(keyword: string): Promise<string[]> {
  const resp = await fetch(`/api/users?keyword=${encodeURIComponent(keyword)}`);
  return resp.json();
}

function UserSearch() {
  const [keyword, setKeyword] = useState("");
  const [users, setUsers] = useState<string[]>([]);

  useEffect(() => {
    if (!keyword.trim()) {
      setUsers([]);
      return;
    }

    // 防抖：输入停止 300ms 后再请求
    const timer = setTimeout(async () => {
      const result = await fetchUsers(keyword);
      setUsers(result);
    }, 300);

    // 清理：下次 keyword 变化前/组件卸载前，取消上一次等待中的任务
    return () => clearTimeout(timer);
  }, [keyword]);

  return (
    <>
      <input value={keyword} onChange={(e) => setKeyword(e.target.value)} />
      <ul>
        {users.map((name) => (
          <li key={name}>{name}</li>
        ))}
      </ul>
    </>
  );
}
```

### 8.1 逐帧执行图（以 `[keyword]` 为例）

| 帧 | 触发事件 | 渲染（Render） | effect（Setup） | cleanup（清理） |
|---|---|---|---|---|
| F1 | 首次挂载 | `keyword=""`，显示空列表 | 不发请求（直接返回） | 无 |
| F2 | 输入 `ja` | 用 `keyword="ja"` 重新渲染 | 创建 `timer#1`（300ms 后请求） | 无 |
| F3 | 很快又输入 `java` | 用 `keyword="java"` 再渲染 | 创建 `timer#2`（300ms 后请求） | **先清理 `timer#1`** |
| F4 | 停止输入并等待 | UI 保持当前输入 | `timer#2` 触发，请求并更新列表 | 无 |
| F5 | 组件卸载 | 组件从页面移除 | 无 | 清理仍在等待的 timer（若存在） |

你会看到一个固定顺序：

1. 状态变化触发渲染
2. 渲染完成后，检查依赖是否变化
3. 若变化：先跑上一次 cleanup，再跑这一次 setup

Java 类比：像“监听器重注册”流程——先 `remove old listener`，再 `add new listener`，避免重复订阅和资源泄漏。

### 8.2 为什么一定要 cleanup

如果不 cleanup，定时器会越积越多：

- 输入 `ja` 时开一个 timer
- 输入 `jav` 又开一个 timer
- 输入 `java` 再开一个 timer

最终会出现多次重复请求、旧结果覆盖新结果、资源浪费。

### 8.3 依赖数组语义（速记）

- `[]`：仅首次挂载执行一次
- `[dep]`：挂载后执行 + `dep` 变化时再执行
- 不写依赖：每次渲染都执行（通常不推荐）

Java 类比：`useEffect` 不是单纯“生命周期函数”，更像“依赖驱动的回调 + 自动清理器”。

---

## 9. `useRef`：保存“可变容器”

`useRef` 常用于两类场景：

1. 获取 DOM 或第三方实例引用
2. 保存不需要触发渲染的可变值

```tsx
import { useRef } from "react";

const inputRef = useRef<HTMLInputElement | null>(null);

function focusInput() {
  inputRef.current?.focus();
}
```

注意：修改 `ref.current` 不会触发重渲染。

---

## 10. `useMemo` 与 `useCallback`：控制不必要计算和渲染

- `useMemo`：缓存“值”
- `useCallback`：缓存“函数”

```tsx
import { useCallback, useMemo } from "react";

const total = useMemo(() => heavyCompute(list), [list]);
const onSelect = useCallback((id: string) => doSomething(id), [doSomething]);
```

原则：

- 先保证正确，再做优化。
- 不要为“看起来高级”而滥用。

---

## 11. 组件复用与组合

React 推荐“组合优于继承”。常见方式：

### 11.1 `children` 插槽

```tsx
import type { ReactNode } from "react";

type PanelProps = { title: string; children: ReactNode };

function Panel({ title, children }: PanelProps) {
  return (
    <section>
      <h3>{title}</h3>
      <div>{children}</div>
    </section>
  );
}
```

### 11.2 业务逻辑抽成自定义 Hook

当多个组件共享同一套状态逻辑时，用 `useXxx` 提炼。

---

## 12. 受控组件与表单思维

受控组件：输入值由 React state 驱动。

```tsx
const [name, setName] = useState("");
<input value={name} onChange={(e) => setName(e.target.value)} />
```

好处：

- 数据流可控
- 便于校验、联动、提交

---

## 13. 数据流：单向数据流是 React 的核心原则

父组件通过 props 向下传递数据，子组件通过回调把事件向上传递。

典型模式：

1. 父组件保存状态
2. 子组件渲染 props
3. 子组件触发事件回调
4. 父组件更新状态并重新下发 props

这个“自上而下”的流向，让组件行为更可预测。

Java 类比：父组件像上层 Service/Controller，子组件像被调用方；数据向下传，事件通过回调向上抛，避免跨层随意改状态。

---

## 14. Java 与 React 对照速查表

| React 概念 | Java 工程师可类比 | 关键差异 |
|---|---|---|
| 组件（Component） | 类 + `render()` | React 由状态变化自动触发渲染 |
| `props` | 方法参数 / 构造参数 | `props` 在组件内部应只读 |
| `state` | 类的成员变量 | 需通过 `setXxx` 更新才能刷新 UI |
| `useEffect` | 生命周期回调 + 监听器 | 依赖变化会重复执行，并支持清理 |
| `useRef` | 成员变量引用 | 改 `ref.current` 不触发重渲染 |
| `useMemo` | 缓存计算结果 | 仅性能优化，不改变业务语义 |

---

## 15. 函数式组件常见误区

1. `useEffect` 依赖漏写，造成闭包旧值问题。
2. 把所有数据都放全局状态，导致复杂度上升。
3. 直接修改 state 对象/数组，导致视图不更新。
4. 列表渲染使用不稳定 key，导致渲染异常。
5. 为了优化而过度使用 `useMemo/useCallback`。

---

## 16. 一句话总结

函数式组件的核心是：

- 用 props 描述输入
- 用 state 管理变化
- 用 effect 处理副作用
- 用组合与 Hook 复用逻辑
- 用单向数据流保持可预测

把这五点真正理解透，你就掌握了 React 组件基础的主干。
