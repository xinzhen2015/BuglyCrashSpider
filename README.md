# Bugly Crash Spider

> 针对 Bugly 平台的 Crash 信息爬虫工具，运行时会分页扫描 Bugly 上的奔溃列表，遇到新出现的 Crash，就调用报警处理。

- 爬虫使用 Kotlin 实现
- 模拟登录使用 Puppeteer 实现

- [x] Crash 信息爬取
- [x] 新发生 Crash 报警
    - 扫描频率可自行配置(可通过 `crontab` 实现等)
    - 默认实现了发送到企业微信群机器人, 可自行扩展发送到其他 IM (Slack, 钉钉等)

- [x] 扫到的 Crash 信息会存的数据库, 以便下次扫描时过滤出新发生的 Crash. 默认实现了 MySql 存储,可自行扩展
- [x] 使用 `Puppeteer` 模拟登录获取 token 和 cookie

## 爬虫使用方法

### 使用 jar

#### 使用默认实现 

https://github.com/stefanJi/BuglyCrashSpider/releases

#### 自己打包:

```
./gradle clean build
```

### 配置

> JSON 格式

|key|value type|note|
|:---|:---|:---|
|buGlyHost|`string`|Bugly的host地址 现在是`https://bugly.qq.com/v2`|
|auth.token|`string`|Bugly 请求响应 header 里的 X-token 对应的值。不用自己填 value，只用留下这个 key，value 由 Puppeteer 注入|
|auth.cookie|`string`|Bugly 请求响应 header 里的 Set-cookie中 `bugly_session` 对应的值。不用自己填 value，只用留下这个 key，value 由 Puppeteer 注入| 
|query.searchType|`string`|筛选的类型|
|query.exceptionTypeList|`string`|异常的类型|
|query.pid|`string`|默认为`1`|
|query.platformId|`string`| Android 为 `1`|
|query.sortOrder|`string`|排序方式 `asc` `desc`|
|query.status|`string`|筛选处理状态 0: 未处理|
|query.rows|`int`|每次请求的数据量 最大100|
|query.sortField|`string`|排序的key `uploadTime`: 按上报时间|
|query.appId|`string`|Bugly 中应用的 App ID|

以下为可选:

> 如果你使用现在的默认实现，则需要都填写

|key|value type|note|
|:---|:---|:---|
|qyWeChatBot.webHook|`string`|企业微信群机器人 hook 地址|
|mysql.host|`string`|MySQL 数据库 Host|
|mysql.user|`string`||
|mysql.pass|`string`||


<details>
<summary>配置文件模板</summary>

```json
{
  "buGlyHost": "https://bugly.qq.com/v2/issueList",
  "auth": {
    "token": "",
    "cookie": ""
  },
  "query": {
    "searchType": "errorType",
    "exceptionTypeList": "Crash,Native",
    "pid": "1",
    "platformId": "1",
    "sortOrder": "desc",
    "status": "0",
    "rows": 20,
    "sortField": "uploadTime",
    "appId": "Bugly上分配给App的id"
  },
  "qyWeChatBot": {
    "webHook": "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=<群机器人的Key>"
  },
    "mysql": {
    "host": "localhost:3306/test?useSSL=true",
    "user": "root",
    "pass": "123456"
  }
}
```
</details>

### 扩展

- `IHandler` 报警处理, 现已提供 `QYWeChatHandler` 发送消息到企业微信群机器人
- `IDao` 数据持久化, 现已提供 `MySqlDaoImpl` 持久化到 MySQL
- `IRequester` 模拟请求 Bugly 接口, 现已提供 `OkHttpRequester` 通过 OkHttp 模拟请求
- `Filter` 过滤策略, 现已提供 `ExistsFilter`: 过滤类似的异常堆栈; `NumberFilter`: 过滤发生次数

```kotlin
class App {
    companion object {
        @JvmStatic
        fun main(vararg args: String) {
            ConfigMgr.setConfigFilePath(parseCLIArg(*args))
            val config = ConfigMgr.config()
            val handler: IHandler = QYWeChatHandler(config.qyWeChatBot, config)
            val dao: IDao = MySqlDaoImpl(config)
            val requester: IRequester = OkHttpRequester()
            val filters: Array<Filter> = arrayOf(ExistsFilter())
            try {
                BuGlyCrashSpider(config, handler, requester, dao， filters).start()
            } catch (e: Exception) {
                handler.handleException(e)
            }
        }
     }
}
```

## Puppeteer 模拟登录

```
cd login_by_puppeteer/
yarn install
cd ..
node login_by_puppeteer/index.js config.json
```

```
登录
模拟登录
https://bugly.qq.com/v2/workbench/apps
登录成功
{ accept: 'application/json;charset=utf-8',
  referer: 'https://bugly.qq.com/v2/workbench/apps',
  'x-csrf-token': 'phBXYDH3-wqfnydJdYyYjkhr',
  'x-token': '251744223',
  'user-agent':
   'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3844.0 Safari/537.36',
  'sec-fetch-mode': 'cors',
  'content-type': 'application/json;charset=utf-8' }
获取 token 和 cookie
bugly_session: eyJpdiI6IlIzc291NzhVRGxMRVF6S2dtcUgxckE9PSIsInZhbHVlIjoiN2ljTEdMY3c5TExCSnZQaVFEOXBaMDJBRjZucVNHaGxJNGU0aEF0VVRsem10RFU2MW5cL2ZQZFpkMkhyU3I4cTNsd2JUSE03SVV5RXc2ME03MDBwY3VnPT0iLCJtYWMiOiI4NzU3YTBlMmNkYmE2ODAxNjU5N2Y3OTkyYjFhN2YwYjY0NTNlZjQ2YzEwMGUzNGU3YmQ5ODdiODgwY2NkMDMxIn0%3D
x-token: 251744223
开始写入新的 token cookie
写入完成
```

## 直接执行

```
./script/crontab_task.sh
```