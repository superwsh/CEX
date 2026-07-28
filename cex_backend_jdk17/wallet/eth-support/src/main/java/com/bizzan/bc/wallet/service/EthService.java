package com.bizzan.bc.wallet.service;

import com.bizzan.bc.wallet.config.CoinProperties;
import com.bizzan.bc.wallet.entity.Account;
import com.bizzan.bc.wallet.entity.Contract;
import com.bizzan.bc.wallet.entity.Payment;
import com.bizzan.bc.wallet.util.EthConvert;
import com.bizzan.bc.wallet.util.MessageResult;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.crypto.exception.CipherException;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;


@Service
public class EthService {
    private static final Logger log = LoggerFactory.getLogger(EthService.class);

    @Autowired private CoinProperties coin;
    @Autowired private Web3j web3j;
    @Autowired private PaymentHandler paymentHandler;
    @Autowired private ResourceLoader resourceLoader;
    @Autowired
    private AccountService accountService;
    @Autowired
    private Contract contract;

    /**
     * 从 classpath 加载 keystore 文件
     */
    @SneakyThrows
    public Credentials loadKeystoreFromResource(String walletPath, String password) {
        Resource resource = resourceLoader.getResource("classpath:" + walletPath);
//        try (InputStream is = resource.getInputStream()) {
//            return WalletUtils.loadCredentials(password, resource.getFile());
//        }
        return WalletUtils.loadCredentials(password, resource.getFile());
    }

    /**
     * 获取 ETH 余额（原生 ETH）
     */
    public BigDecimal getBalance(String address) throws IOException {
        EthGetBalance resp = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
        return Convert.fromWei(resp.getBalance().toString(), Convert.Unit.ETHER);
    }

    /**
     * 获取当前 gasPrice（支持 gasSpeedUp 配置）
     */
    public BigInteger getGasPrice() throws IOException {
        EthGasPrice resp = web3j.ethGasPrice().send();
        return new BigDecimal(resp.getGasPrice())
                .multiply(coin.getGasSpeedUp())
                .toBigInteger();
    }

    /**
     * 计算矿工费
     */
    public BigDecimal getMinerFee(BigInteger gasLimit) throws IOException {
        if (gasLimit == null || gasLimit.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("GasLimit必须大于0");
        }

        BigDecimal gasPrice = new BigDecimal(getGasPrice());
        BigDecimal fee = gasPrice.multiply(new BigDecimal(gasLimit));
        BigDecimal etherFee = Convert.fromWei(fee, Convert.Unit.ETHER);

        log.debug("矿工费计算 - GasLimit：{}，GasPrice：{}，费用：{} ETH",
                gasLimit, gasPrice, etherFee);
        return etherFee;
    }

    /**
     * 检查交易是否成功
     */
    public boolean isTransactionSuccess(String txid) throws IOException {
        if (StringUtils.isBlank(txid)) {
            log.warn("检查交易状态失败，交易ID为空");
            return false;
        }

        EthTransaction ethTransaction = web3j.ethGetTransactionByHash(txid).send();
        if (ethTransaction.getTransaction().isEmpty()) {
            log.warn("交易不存在，ID：{}", txid);
            return false;
        }

        var transaction = ethTransaction.getTransaction().get();
        // 检查是否已上链
        if ("0x0000000000000000000000000000000000000000000000000000000000000000"
                .equalsIgnoreCase(transaction.getBlockHash())) {
            log.warn("交易未上链，ID：{}", txid);
            return false;
        }

        // 检查交易回执
        EthGetTransactionReceipt receiptResponse = web3j.ethGetTransactionReceipt(txid).send();
        if (receiptResponse.getTransactionReceipt().isEmpty()) {
            log.warn("交易回执不存在，ID：{}", txid);
            return false;
        }

        String status = receiptResponse.getTransactionReceipt().get().getStatus();
        boolean success = "0x1".equalsIgnoreCase(status);
        log.debug("交易状态检查，ID：{}，状态：{}，是否成功：{}", txid, status, success);

        return success;
    }

    /**
     * ✅【核心改动】获取 ERC20 Token 余额 —— 替换 jsonrpc4j
     * @param contractAddress 合约地址（如 USDT）
     * @param address 查询地址
     * @return 余额（已转换为 ETH 单位，可根据需要调整）
     */
    public BigDecimal getTokenBalance(String contractAddress, String address) throws IOException {
        if (StringUtils.isBlank(address)) {
            log.warn("查询代币余额失败，地址为空");
            return BigDecimal.ZERO;
        }
        log.info(">>>>>>>>>>>1111>>>>>>>>>>>>address={}, contractAddress={}", address, contractAddress);
        if(StringUtils.isBlank(contractAddress)) contractAddress = contract.getAddress();
        // 构造 balanceOf(address) 调用
        Function function = new Function(
                "balanceOf",
                Collections.singletonList(new Address(address)),
                Collections.singletonList(new TypeReference<Uint256>() {})
        );
        String encodedData = FunctionEncoder.encode(function);

        // 使用 Web3j 原生 eth_call
        // 很多节点要求 eth_call 必须包含从哪个地址调用的，这里可以用目标地址或全零地址
        log.info(">>>>>>>>>>>>>>>>>>>>>>>address={}, contractAddress={}, encodedData={}", address, contractAddress, encodedData);
        Transaction transaction = Transaction.createEthCallTransaction(address, contractAddress, encodedData);
        EthCall ethCall = web3j.ethCall(transaction, DefaultBlockParameterName.LATEST).send();
        if (ethCall == null) {
            throw new IOException("RPC 返回为空");
        }
        if (ethCall.hasError()) {
            log.error("getTokenBalance failed: {}", ethCall.getError().getMessage());
            throw new IOException("Failed to query token balance: " + ethCall.getError().getMessage());
        }

        String value = ethCall.getValue();
        if ("0x".equalsIgnoreCase(value) || value.length() <= 2) {
            value = "0x0";
        }

        BigInteger balance = Numeric.toBigInt(value);
        // 注意：此处单位需与合约 decimals 匹配，示例按 18 位处理
        return EthConvert.fromWei(new BigDecimal(balance), EthConvert.Unit.ETHER);
    }

    /**
     * 发起 ETH 转账（同步）
     */
    public MessageResult transferEth(Credentials credentials, String to, BigDecimal amount) {
        return paymentHandler.transferEth(Payment.builder()
                .credentials(credentials)
                .to(to)
                .amount(amount)
                .unit("ETH")
                .build());
    }

    /**
     * 创建新钱包（保留原有逻辑，优化异常处理）
     */
    public String createNewWallet(String account, String password) throws Exception {
        log.info("生成新ETH钱包，账户：{}", account);

        // 参数校验
        if (StringUtils.isBlank(account)) {     // || StringUtils.isBlank(password)
            throw new IllegalArgumentException("账户名和密码不能为空");
        }
        if (coin.getKeystorePath() == null) {
            throw new IllegalStateException("Keystore路径未配置");
        }

        File keystoreDir = new File(coin.getKeystorePath());
        if (!keystoreDir.exists() && !keystoreDir.mkdirs()) {
            throw new IOException("创建Keystore目录失败：" + coin.getKeystorePath());
        }

        // 生成钱包文件
        String fileName = WalletUtils.generateNewWalletFile(password, keystoreDir, true);
        Credentials credentials = WalletUtils.loadCredentials(password,
                new File(keystoreDir, fileName));
        String address = credentials.getAddress();

        // 保存账户信息
        accountService.saveOne(account, fileName, address);
        log.info("生成钱包成功，账户：{}，地址：{}", account, address);

        return address;
    }

    /**
     * 同步地址余额
     */
    public void syncAddressBalance(String address) throws IOException {
        if (StringUtils.isBlank(address)) {
            log.warn("同步余额失败，地址为空");
            return;
        }

        BigDecimal balance = getBalance(address);
        accountService.updateBalance(address, balance);
//        Metrics.gauge("eth.wallet.balance", Collections.singletonMap("address", address), balance);
        log.info("同步余额成功，地址：{}，余额：{} ETH", address, balance);
    }

    public MessageResult rechargeToMemberWallet(String address, BigDecimal amount) {
        Account account = accountService.findByAddress(address);
        if (account == null) {
            MessageResult messageResult = new MessageResult(500, "没有找到账户");
            log.info(messageResult.toString());
            return messageResult;
        }
        BigDecimal memberWalletBalance = account.getMemberWalletBalance();
        try {
            accountService.updateMemberWalletBalance(account.getAddress(), memberWalletBalance.add(amount));
        } catch (Exception e) {
            e.printStackTrace();
        }
        MessageResult result = new MessageResult(0, "success");
        return result;
    }

    public File getResourceAsFile(String filePath) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + filePath);

        if (!resource.exists()) {
            throw new IOException("资源不存在: " + filePath);
        }

        // 检查资源是否在文件系统中（非JAR包）
        try {
            return resource.getFile(); // 如果资源在文件系统中，直接返回
        } catch (IOException e) {
            // 如果资源在JAR包中，提取到临时文件
            return extractFromJarToTempFile(resource, filePath);
        }
    }

    private File extractFromJarToTempFile(Resource resource, String originalPath) throws IOException {
        // 从路径中提取文件名
        String fileName = originalPath.substring(originalPath.lastIndexOf('/') + 1);

        // 创建临时文件
        File tempFile = File.createTempFile("jar-extract-", "-" + fileName);
        tempFile.deleteOnExit(); // JVM退出时删除

        // 将资源内容复制到临时文件
        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        return tempFile;
    }

    public MessageResult withdraw(File walletFile, String password, String toAddress, BigDecimal amount, boolean sync, String withdrawId) {
        Credentials credentials;
        try {
            credentials = WalletUtils.loadCredentials(password, walletFile);
        } catch (IOException e) {
            e.printStackTrace();
            return new MessageResult(500, "钱包文件不存在");
        } catch (CipherException e) {
            e.printStackTrace();
            return new MessageResult(500, "解密失败，密码不正确");
        }
        return paymentHandler.transferEth(credentials, toAddress, amount);

    }

    public MessageResult transferFromWithdrawWallet(String toAddress, BigDecimal amount, boolean sync, String withdrawId) {
        String fileName = coin.getKeystorePath() + "/" + coin.getWithdrawWallet();
        // 将URL转换为File对象
        try {

            // 将URL转换为File对象
            File withdrawFile = getResourceAsFile(fileName);

            MessageResult result = withdraw(withdrawFile, coin.getWithdrawWalletPassword(), toAddress, amount, sync, withdrawId);
            log.info("withdraw result:{}", result.toString());

            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return new MessageResult(500, "读取本地提现ks失败");
        }
    }

    public MessageResult withdrawToken(File walletFile, String password, String toAddress, BigDecimal amount, boolean sync, String withdrawId) {
        Credentials credentials;
        try {
            credentials = WalletUtils.loadCredentials(password, walletFile);
        } catch (IOException e) {
            e.printStackTrace();
            return new MessageResult(500, "钱包文件不存在");
        } catch (CipherException e) {
            e.printStackTrace();
            return new MessageResult(500, "解密失败，密码不正确");
        }

        return paymentHandler.transferToken(credentials, toAddress, amount);

    }

    public MessageResult withdrawFromMemberWallet(String address, BigDecimal amount) {
        Account account = accountService.findByAddress(address);
        if (account == null) {
            MessageResult messageResult = new MessageResult(500, "没有找到账户");
            log.info(messageResult.toString());
            return messageResult;
        }
        BigDecimal memberWalletBalance = account.getMemberWalletBalance();
        try {
            accountService.updateMemberWalletBalance(account.getAddress(), memberWalletBalance.subtract(amount));
        } catch (Exception e) {
            e.printStackTrace();
        }
        MessageResult result = new MessageResult(0, "success");
        return result;
    }

    public MessageResult transferFromWallet(String address, BigDecimal amount, BigDecimal fee, BigDecimal minAmount) {
        log.info("transferFromWallet 方法");
        List<Account> accounts = accountService.findByBalance(minAmount);
        if (accounts == null || accounts.size() == 0) {
            MessageResult messageResult = new MessageResult(500, "没有满足条件的转账账户(大于0.1)!");
            log.info(messageResult.toString());
            return messageResult;
        }
        BigDecimal transferredAmount = BigDecimal.ZERO;
        for (Account account : accounts) {
            BigDecimal realAmount = account.getBalance().subtract(fee);
            if (realAmount.compareTo(amount.subtract(transferredAmount)) > 0) {
                realAmount = amount.subtract(transferredAmount);
            }
            log.info("begin accumulate from " + account);
            MessageResult result = transfer(coin.getKeystorePath() + "/" + account.getWalletFile(), "", address, realAmount, true, "");
            if (result.getCode() == 0 && result.getData() != null) {
                log.info("transfer address={},amount={},txid={}", account.getAddress(), realAmount, result.getData());
                transferredAmount = transferredAmount.add(realAmount);
                try {
                    syncAddressBalance(account.getAddress());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (transferredAmount.compareTo(amount) >= 0) {
                break;
            }
        }
        MessageResult result = new MessageResult(0, "success");
        result.setData(transferredAmount);
        return result;
    }


    public MessageResult transfer(String walletFile, String password, String toAddress, BigDecimal amount, boolean sync, String withdrawId) {
        Credentials credentials;
        try {
            credentials = WalletUtils.loadCredentials(password, walletFile);
        } catch (IOException e) {
            e.printStackTrace();
            return new MessageResult(500, "钱包文件不存在");
        } catch (CipherException e) {
            e.printStackTrace();
            return new MessageResult(500, "解密失败，密码不正确");
        }
        if (sync) {
            return paymentHandler.transferEth(credentials, toAddress, amount);
        } else {
            paymentHandler.transferEthAsync(credentials, toAddress, amount, withdrawId);
            return new MessageResult(0, "提交成功");
        }
    }


    public MessageResult transferToken(String fromAddress, String toAddress, BigDecimal amount, boolean sync) {


        Account account = accountService.findByAddress(fromAddress);
        Credentials credentials;
        try {
            credentials = WalletUtils.loadCredentials("", coin.getKeystorePath() + "/" + account.getWalletFile());
        } catch (IOException e) {
            e.printStackTrace();
            return new MessageResult(500, "私钥文件不存在");
        } catch (CipherException e) {
            e.printStackTrace();
            return new MessageResult(500, "解密失败，密码不正确");
        }

        return paymentHandler.transferToken(credentials, toAddress, amount);
    }

    public MessageResult transferTokenFromWithdrawWallet(String toAddress, BigDecimal amount, boolean sync, String withdrawId) {
        String fileName = coin.getKeystorePath() + "/" + coin.getWithdrawWallet();
        try {

            // 将URL转换为File对象
            File withdrawFile = getResourceAsFile(fileName);
            MessageResult result = withdrawToken(withdrawFile, coin.getWithdrawWalletPassword(), toAddress, amount, sync, withdrawId);
            log.info("withdraw result:{}", result.toString());
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return new MessageResult(500, "读取本地提现ks失败");
        }
    }

}
