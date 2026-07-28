## 免责声明

1. 项目所涉及代码均来自开源社区以及社区二次开发
2. 本项目为社区教学演示类项目，请谨慎使用，且不构成投资建议。

## 前端启动说明
安装npm包依赖

 `npm install` 或 `yarn install`


## 启动命令
启动 前端目录(cex-front) 运行

`npm run serve`

### 配置说明

#### 服务器API配置

开发环境配置: .env.development

生产环境配置: .env.production

**后台部署后配置以下三个环境变量，分别对应后台不同微服务的api**

VUE_APP_UC_API_URL = ''

VUE_APP_EXCHANGE_API_URL = ''

VUE_APP_MARKET_API_URL= ''

**微服务对应端口：**

UC：6001

EXCHANGE：6003

MARKET：6004

**例：**

VUE_APP_UC_API_URL = 'http://{服务器IP地址}:6001'

VUE_APP_EXCHANGE_API_URL = 'http://{服务器IP地址}:6003'

VUE_APP_MARKET_API_URL = 'http://{服务器IP地址}:6004'



测试钱包账号：13800138001
测试钱包密码：12345678