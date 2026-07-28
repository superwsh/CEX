const { ethers } = require("hardhat");
const { queryUSDTBalance } = require("./queryUSDTBalance");
async function main() {
    console.log("=== 账户余额查询脚本 ===\n");

    // 获取网络信息
    const network = await ethers.provider.getNetwork();
    console.log(`当前网络: ${network.name} (Chain ID: ${network.chainId})`);

    // 获取当前区块信息
    const blockNumber = await ethers.provider.getBlockNumber();
    console.log(`当前区块高度: ${blockNumber}\n`);

    // 查询指定地址余额的函数
    async function querySpecificAddress(address) {
        try {
            console.log(`\n=== 查询指定地址: ${address} ===`);

            const balance = await ethers.provider.getBalance(address);
            const balanceInEth = ethers.formatEther(balance);
            const transactionCount = await ethers.provider.getTransactionCount(address);
            const balanceInUsdt = await queryUSDTBalance(address);
            console.log(`地址: ${address}`);
            console.log(`ETH余额: ${balanceInEth} ETH`);
            console.log(`USDT余额: ${balanceInUsdt} USDT`);
            console.log(`交易数量: ${transactionCount}`);

            // 检查是否为合约地址
            const code = await ethers.provider.getCode(address);
            if (code !== "0x") {
                console.log(`类型: 合约地址`);
            } else {
                console.log(`类型: 外部账户`);
            }

        } catch (error) {
            console.error(`查询地址 ${address} 时出错:`, error.message);
        }
    }

    // 查询地址
    const commonAddresses = [
        "0x017053f7d06f152dd3ad998f4ac8ba3ceb663e26",
    ];

    for (const address of commonAddresses) {
        await querySpecificAddress(address);
    }

    // 获取网络统计信息
    console.log("\n=== 网络统计信息 ===");
    try {
        const gasPrice = await ethers.provider.getFeeData();
        console.log(`当前Gas价格: ${ethers.formatUnits(gasPrice.gasPrice, "gwei")} Gwei`);

        if (gasPrice.maxFeePerGas) {
            console.log(`最大Gas费用: ${ethers.formatUnits(gasPrice.maxFeePerGas, "gwei")} Gwei`);
        }
        if (gasPrice.maxPriorityFeePerGas) {
            console.log(`最大优先费用: ${ethers.formatUnits(gasPrice.maxPriorityFeePerGas, "gwei")} Gwei`);
        }
    } catch (error) {
        console.log("无法获取Gas价格信息");
    }

    // 获取网络连接信息
    try {
        const networkInfo = await ethers.provider.getNetwork();
        console.log(`网络名称: ${networkInfo.name}`);
        console.log(`链ID: ${networkInfo.chainId}`);
    } catch (error) {
        console.log("无法获取网络信息");
    }
}

// 错误处理
main()
    .then(() => process.exit(0))
    .catch((error) => {
        console.error("脚本执行失败:", error);
        process.exit(1);
    });

// 导出函数供其他脚本使用
module.exports = {
    queryBalance: async function (address) {
        try {
            const balance = await ethers.provider.getBalance(address);
            return ethers.formatEther(balance);
        } catch (error) {
            throw new Error(`查询地址 ${address} 余额失败: ${error.message}`);
        }
    },

    queryAllBalances: async function () {
        const accounts = await ethers.getSigners();
        const balances = [];

        for (const account of accounts) {
            const balance = await ethers.provider.getBalance(account.address);
            balances.push({
                address: account.address,
                balance: ethers.formatEther(balance),
                privateKey: account.privateKey
            });
        }

        return balances;
    }
};
