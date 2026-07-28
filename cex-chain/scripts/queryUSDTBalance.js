const config = require("../config.json");
async function queryUSDTBalance(address) {
    const usdt = await ethers.getContractAt("USDT", config.USDT.address);
    const balance = await usdt.balanceOf(address);
    return ethers.formatUnits(balance, 6);
}

module.exports = {
    queryUSDTBalance
}