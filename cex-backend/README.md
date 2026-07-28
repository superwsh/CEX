## 免责声明

1. 项目所涉及代码和中间件均来自开源社区以及社区二次开发
2. 本项目为社区教学演示类项目，请谨慎使用，且不构成投资建议。

## 后端启动说明
1. 后端使用docker-compose在本地启动所有的模块以及中间件
2. 启动的中间件包括 redis,mysql,mongodb,zookeeper,kafka
3. 启动的服务包括
   - cloud：注册中心服务  
   - uc：用户中心服务  
   - exchange-api：交易入口服务  
   - exchange：撮合引擎服务  
   - market：行情服务
4. 因为工程较大，完整本地启动时间大概在5~10分钟左右，请耐心等待，后台如果一直卡在下载镜像阶段，一般是网络问题，终止重试几次
5. 因为不同系统的文件系统权限不同，如果遇到log4j文件无法写入，请修改各个logback-spring.xml下的日志路径

## 启动命令

1. 启动 后端目录(cex-backend) 运行
   docker compose up -d
2. 停止 后端目录(cex-backend) 运行
   docker-compose down -v

## 补充说明
1. 启动后docker会在本地创建一个mysql，并初始化一些基础数据，但是这些数据只在工程第一次运行时初始化，如果需要重置这些数据，可以删掉sql/ 下面的data文件夹并重启镜像