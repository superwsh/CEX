package com.bizzan.bc.wallet.service;

import com.bizzan.bc.wallet.config.EtherscanProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class EtherscanApi {

    private final Logger logger = LoggerFactory.getLogger(EtherscanApi.class);
    private final WebClient webClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public EtherscanApi(EtherscanProperties props, ObjectMapper objectMapper,
                        WebClient.Builder webClientBuilder) {
        this.apiKey = props.getApiKey();
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
                .baseUrl("https://api.etherscan.io/api/v2")
                .build();
    }

    public void sendRawTransaction(String hexValue) {
        var payload = Map.of(
                "jsonrpc", "2.0",
                "method", "eth_sendRawTransaction",
                "params", List.of(hexValue),
                "id", 1
        );

        webClient.post()
                .uri(uri -> uri.queryParam("apikey", apiKey).build())
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(resp -> logger.info("Etherscan broadcast: {}", resp))
                .onErrorResume(e -> {
                    logger.error("Etherscan broadcast failed", e);
                    return Mono.empty();
                })
                .subscribe();
    }

    /**
     * 检查事件日志
     */
    public boolean checkEventLog(Long blockHeight, String address, String topic0, String txid) {
        if (blockHeight == null || address == null || topic0 == null || txid == null) {
            logger.warn("检查事件日志参数为空");
            return false;
        }

        try {
            Map<String, String> params = Map.of(
                    "module", "logs",
                    "action", "getLogs",
                    "fromBlock", blockHeight.toString(),
                    "toBlock", blockHeight.toString(),
                    "address", address,
                    "topic0", topic0,
                    "apikey", apiKey
            );

            String response = this.webClient
                    .post()
                    .bodyValue(params)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            logger.info("getLogs result = {}", response);
            JsonNode resultNode = objectMapper.readTree(response);

            // 状态码0表示失败
            if (resultNode.get("status").asInt() == 0) {
                return false;
            }

            // 遍历交易列表匹配txid
            // 1. 获取result数组节点
            JsonNode resultArray = resultNode.get("result");
            if (resultArray == null || resultArray.isEmpty()) {
                return false;
            }

            // 2. 遍历数组，匹配txid（找到后立即返回true）
            for (JsonNode node : resultArray) {
                String txHash = node.get("transactionHash").asText();
                if (txHash != null && txHash.equalsIgnoreCase(txid)) {
                    logger.info("找到匹配的交易日志，txid：{}", txid);
                    return true;
                }
            }
        } catch (WebClientResponseException e) {
            logger.error("Etherscan API请求失败，状态码：{}，响应体：{}", e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (JsonProcessingException e) {
            logger.error("解析Etherscan响应失败", e);
        } catch (Exception e) {
            logger.error("检查事件日志失败", e);
        }
        return false;
    }

}
