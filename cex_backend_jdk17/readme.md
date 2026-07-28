

# 清理 Docker 构建缓存和无效镜像
# 删除所有悬空镜像（无标签且未被容器引用）
docker image prune -f
# 删除所有未使用的镜像（包括有标签但未被引用的）
docker image prune -a -f
# 清理构建缓存
docker builder prune -a -f

docker-compose down --rmi local
docker compose down
docker builder prune -af
docker image prune -af

# 然后重试
docker compose up -d


* kafka:
  * 临时启动镜像验证：docker run --rm -it docker.m.daocloud.io/apache/kafka:3.9.0 bash
    * docker build -t kraft-kafka . 
    * && docker run -id -d --name kafka -p 9092:9092 -p 9093:9093 kraft-kafka
    * docker run -id -d --name kafka -p 9092:9092 -p 9093:9093 kraft-kafka
    * 创建topic：docker exec -it kafka kafka-topics --create --topic test-topic --bootstrap-server kafka:9092 --partitions 1 --replication-factor 1
    * 发送消息：docker exec -it kafka kafka-console-producer --topic test-topic --bootstrap-server kafka:9092
    * 消费消息：docker exec -it kafka kafka-console-consumer --topic test-topic --from-beginning --bootstrap-server kafka:9092