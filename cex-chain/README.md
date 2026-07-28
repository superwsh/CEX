# CEX 代币合约

目录包含了 C2E-CEX 使用的 ERC20 代币合约。

## 部署
- **启动本地链**
`npx hardhat node`
- **部署USDT代币**
`npm run deploy`

- 测试
`npx hardhat test`

## 合约列表

### 1. USDT.sol - Tether USD
- **名称**: Tether USD
- **符号**: USDT
- **精度**: 6 位小数
- **初始供应量**: 1,000,000,000 USDT

#### 功能特性
- ✅ 标准 ERC20 功能
- ✅ 铸币功能（仅所有者）
- ✅ 销毁功能
- ✅ 暂停/恢复功能
- ✅ 黑名单功能
- ✅ 重入攻击保护
- ✅ 所有者权限管理

#### 主要函数
```solidity
// 铸币
function mint(address to, uint256 amount) external onlyOwner

// 销毁
function burn(uint256 amount) external
function burnFrom(address from, uint256 amount) external onlyOwner

// 黑名单管理
function addToBlacklist(address account) external onlyOwner
function removeFromBlacklist(address account) external onlyOwner
function isBlacklisted(address account) external view returns (bool)

// 暂停/恢复
function pause() external onlyOwner
function unpause() external onlyOwner
```

## 部署说明

### 1. 安装依赖
```bash
npm install @openzeppelin/contracts
```

### 2. 编译合约
```bash
npx hardhat compile
```

### 3. 运行测试
```bash
npx hardhat test test/TokenTest.js
```

### 4. 部署合约
```bash
npx hardhat run scripts/deploy-tokens.js --network <network-name>
```

## 安全特性

### USDT 合约
- 使用 OpenZeppelin 的安全库
- 支持暂停功能防止紧急情况
- 黑名单功能防止恶意地址
- 重入攻击保护
- 所有者权限控制

## 使用示例

### 部署合约

### 铸币操作
```javascript

// 铸币 1000 USDT 给指定地址
await usdt.mint(userAddress, ethers.parseUnits("1000", 6));
```

### 黑名单操作
```javascript
// 添加地址到黑名单
await usdt.addToBlacklist(maliciousAddress);

// 从黑名单移除地址
await usdt.removeFromBlacklist(maliciousAddress);

// 检查地址是否在黑名单中
const isBlacklisted = await usdt.isBlacklisted(address);
```

## 注意事项

1. **权限管理**: 只有合约所有者可以执行铸币、销毁、暂停等管理操作
2. **黑名单功能**: 仅 USDT 合约支持黑名单功能
3. **精度设置**: WBTC 使用 8 位小数，USDT 使用 6 位小数
4. **安全考虑**: 合约已通过测试，但在主网部署前建议进行安全审计

## 测试覆盖

合约包含完整的测试套件，覆盖以下功能：
- ✅ 基本 ERC20 功能
- ✅ 铸币和销毁功能
- ✅ 转账功能
- ✅ 暂停/恢复功能
- ✅ 黑名单功能（USDT）
- ✅ 权限控制
- ✅ 错误处理

运行测试命令：
```bash
npx hardhat test
```
