<template>
  <div class="page-web page-bg">

    <Head />
    <div class="spot-account-section">
      <div class="container">
        <div class="spot-account-header">
          <h1>{{ $t('spotAccount') }}</h1>
          <p>{{ $t('spotAccountDesc') }}</p>
        </div>

        <div class="spot-account-content">
          <el-tabs v-model="activeTab" class="spot-account-tabs">
            <el-tab-pane :label="$t('spotAccount')" name="spot">
              <div class="spot-account-info">

                <div class="asset-list">
                  <div class="asset-list-header">
                    <h3>{{ $t('assetList') }}</h3>
                    <el-button type="primary" size="small" @click="loadAssets">
                      {{ $t('refresh') }}
                    </el-button>
                  </div>

                  <el-table :data="assetList" style="width: 100%" v-loading="loading"
                    :cell-style="{ textAlign: 'left' }">
                    <el-table-column prop="currency" :label="$t('currency')" align="left">
                      <template slot-scope="scope">
                        <span>{{ scope.row.currency }}</span>
                      </template>
                    </el-table-column>
                    <el-table-column prop="balance" :label="$t('balance')" align="left">
                      <template slot-scope="scope">
                        <div class="amount-info" style="text-align: left;">
                          <div class="amount">{{ formatAmount(scope.row.balance) }}</div>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column :label="$t('actions')" align="left">
                      <template slot-scope="scope">
                        <el-button type="text" size="small" @click="recharge(scope.row)">
                          {{ $t('recharge') }}
                        </el-button>
                        <el-button type="text" size="small" @click="showWithdrawDialog(scope.row)">
                          {{ $t('withdraw') }}
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </div>

    <!-- 充值弹窗 -->
    <el-dialog :title="$t('recharge')" :visible.sync="rechargeDialogVisible" width="600px" :close-on-click-modal="false"
      :before-close="handleRechargeClose" :append-to-body="true" :modal="true">
      <div class="recharge-form">
        <div class="form-item">
          <label>{{ $t('currency') }}:</label>
          <span class="currency-display">{{ currentRecharge.currency }}</span>
        </div>

        <div class="form-item">
          <label>{{ $t('networkAddresses') }}:</label>
          <div class="network-address-list">
            <div v-for="network in getRechargeNetworks()" :key="network.value" class="network-item"
              :class="{ 'active': currentRecharge.selectedNetwork === network.value, 'disabled-network': network.disabled }"
              @click="network.disabled ? null : selectRechargeNetwork(network.value)">
              <div class="network-info">
                <div class="network-name" :class="{ 'disabled-text': network.disabled }">{{ network.label }}</div>
                <div class="network-address" :class="{ 'disabled-text': network.disabled }">{{ network.address }}</div>
              </div>
              <el-button type="text" size="small" @click.stop="network.disabled ? null : copyAddress(network.address)"
                class="copy-btn" :disabled="network.disabled">
                {{ $t('copy') }}
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <div slot="footer" class="dialog-footer">
        <el-button @click="rechargeDialogVisible = false">
          {{ $t('close') }}
        </el-button>
      </div>
    </el-dialog>

    <!-- 提现弹窗 -->
    <el-dialog :title="$t('withdraw')" :visible.sync="withdrawDialogVisible" width="500px" :close-on-click-modal="false"
      :before-close="handleClose" :append-to-body="true" :modal="true">
      <div class="withdraw-form">
        <div class="transfer-to-wallet-description">
          {{ $t('transferToCollectWalletAmountDescription') }}
        </div>
        <div class="transfer-to-wallet">
          <el-tooltip :content="$t('transferToCollectWalletTip')" placement="top" popper-class="narrow-tooltip">
            <el-button type="primary" size="small" @click="transferToCollectWallet">
              {{ $t('transferToCollectWallet') }}
            </el-button>
          </el-tooltip>
          <div style="display: flex;align-items: center; justify-content: space-between;">
            <div style="margin-right: 10px;">{{ $t('transferToCollectWalletAmount') }}</div>
            <div>
              <el-input v-model="transferToCollectWalletAmount" type="number" style="display: inline-block;"></el-input>
            </div>
          </div>
        </div>

        <div class="form-item">
          <label>{{ $t('currency') }}:</label>
          <span class="currency-display">{{ currentWithdraw.currency }}</span>
        </div>
        <div class="form-item">
          <label>{{ $t('network') }}:</label>
          <el-select v-model="currentWithdraw.network" placeholder="选择网络" style="width: 100%"
            @change="updateNetworkDescription">
            <el-option v-for="network in getAvailableNetworks()" :key="network.value" :label="network.label"
              :value="network.value" :disabled="network.disabled" :class="{ 'disabled-network': network.disabled }">
            </el-option>
          </el-select>
        </div>
        <div v-if="currentWithdraw.description" class="network-description">
          <el-alert :title="currentWithdraw.description" type="warning" :closable="false" show-icon>
          </el-alert>
        </div>

        <div class="form-item">
          <label>{{ $t('withdrawAddress') }}:</label>
          <el-input v-model="currentWithdraw.address" :placeholder="$t('enterWithdrawAddress')" type="text"
            style="width: 100%">
          </el-input>
        </div>

        <div class="form-item">
          <label>{{ $t('withdrawAmount') }}:</label>
          <el-input v-model="currentWithdraw.amount" :placeholder="$t('enterWithdrawAmount')" type="number"
            style="width: 100%">
            <template slot="append">
              <el-button @click="setMaxAmount">MAX</el-button>
            </template>
          </el-input>
          <div class="balance-info">
            {{ $t('available') }}: {{ formatAmount(getCurrentBalance()) }}
          </div>
        </div>

        <div class="form-item">
          <label>{{ $t('fee') }}:</label>
          <span class="fee-display">
            <!-- 手续费固定0.0001 -->
            {{ formatFee() }}
            {{ currentWithdraw.currency }}
          </span>
        </div>

        <!-- 合约信息显示 -->
        <div v-if="getCurrentNetworkInfo()" class="contract-info">
          <div class="info-content">
            <div v-if="getCurrentNetworkInfo().info" class="info-item">
              <span class="info-label">信息:</span>
              <span class="info-value contract-address">{{ getCurrentNetworkInfo().info }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="actual-amount">预计到账金额：{{ getExpectedAmount() }}</div>

      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="confirmWithdraw" :disabled="!canWithdraw()" :loading="withdrawing">
          {{ withdrawing ? $t('withdrawing') : $t('confirmWithdraw') }}
        </el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import Head from '@/components/Head.vue'

import { createWallet, transferToCollectWallet, getMinerFee } from '@/api/api/wallet'
import { withdraw } from '@/api/api/user'
import { getUserInfo } from '@/api/api/user'
import { DEFAULT_MINER_FEE, COIN_DETAIL } from '@/constant/coins.config'
import {
  createWalletList,
  createWalletBalanceList,
  getNetworkNameByIndex,
  getDefaultWalletAddress,
  getDefaultAvailableNetworks,
  getDefaultAssetList
} from './spot.account.service'


export default {
  name: 'SpotAccount',
  components: {
    Head,
  },
  data() {
    return {
      activeTab: 'spot',
      loading: false,
      withdrawDialogVisible: false,
      withdrawing: false,
      currentWithdraw: {
        currency: '', // uppercase
        network: '',
        address: '',
        amount: '',
        description: ''
      },
      transferToCollectWalletAmount: 0,
      // 每个代币对应一个钱包地址
      evmWalletAddress: getDefaultWalletAddress(),
      availableNetworks: getDefaultAvailableNetworks(),
      rechargeDialogVisible: false,
      currentRecharge: {
        currency: '', // uppercase
        selectedNetwork: ''
      },
      assetList: getDefaultAssetList(),
      minerFee: 0,
      assetsInterval: null,
      memberId: ''
    }
  },
  mounted() {
    this.isLogin = this.$store.state.isLogin
    if (!this.isLogin) {
      this.$message.error('请先登录')
      return
    }
    getUserInfo().then(res => {
      this.account = res.data.username;
      this.memberId = res.data.id;
      this.loadWalletAddress().then(() => {
        this.loadAssets()
        this.pollingAssets()
      })
    }).catch((e) => {
      console.log(e)
      this.$message.error('获取用户信息失败,请检查网络')
    })
  },
  beforeDestroy() {
    clearInterval(this.assetsInterval)
  },
  methods: {
    pollingAssets() {
      this.assetsInterval = setInterval(() => {
        this.loadAssets()
      }, 10000)
    },
    loadWalletAddress() {
      return Promise.allSettled(createWalletList(this.memberId)).then((resList) => {
        resList.forEach((res, index) => {
          if (res.status === 'fulfilled') {
            this.evmWalletAddress[COIN_DETAIL[getNetworkNameByIndex(index)].symbol] = res.value.data;
          } else {
            // 如果多个toast在同一个微任务执行，两个toast会重叠
            setTimeout(() => {
              this.$message.error(`创建${getNetworkNameByIndex(index)}钱包失败，请检查网络`)
            }, 0)
          }
        })
      })
    },
    loadAssets() {
      if (!this.isLogin) {
        this.$message.error('请先登录')
        return
      }
      this.loading = true
      // 加载数据
      Promise.allSettled(createWalletBalanceList()).then((resList) => {
        resList.forEach((res, index) => {
          if (res.status === 'fulfilled') {
            this.assetList[index].balance = res.value.data.balance;
          } else {
            setTimeout(() => {
              this.$message.error(`获取${getNetworkNameByIndex(index)}余额失败，请检查网络`)
            }, 0)
          }
        })
      }).finally(() => {
        this.loading = false
      })
    },

    formatAmount(amount) {
      return parseFloat(amount).toFixed(8)
    },

    formatUSDT(amount) {
      return parseFloat(amount).toFixed(2)
    },

    recharge(asset) {
      if (!this.isLogin) {
        this.$message.error('请先登录')
        return
      }
      const currency = asset.currency.toUpperCase();
      // 创建钱包，如果已创建钱包则返回钱包地址
      createWallet(currency, this.memberId).then(res => {
        this.evmWalletAddress[currency] = res.data;
        this.currentRecharge.currency = currency;
        this.currentRecharge.selectedNetwork = ''
        this.rechargeDialogVisible = true
      })
    },

    showWithdrawDialog(asset) {
      if (!this.isLogin) {
        this.$message.error('请先登录')
        return
      }
      const currency = asset.currency.toUpperCase();
      this.currentWithdraw.currency = currency;
      this.currentWithdraw.network = ''
      this.currentWithdraw.address = ''
      this.currentWithdraw.amount = ''
      this.currentWithdraw.description = ''
      this.transferToCollectWalletAmount = 0;
      this.withdrawDialogVisible = true

      this.getMinerFee()

      // 默认选中第一个可用的网络
      this.$nextTick(() => {
        const networks = this.getAvailableNetworks()
        if (networks.length > 0) {
          const firstAvailableNetwork = networks.find(network => !network.disabled)
          if (firstAvailableNetwork) {
            this.currentWithdraw.network = firstAvailableNetwork.value
            this.updateNetworkDescription()
          }
        }
      })
    },
    getMinerFee() {
      const currency = this.currentWithdraw.currency;
      getMinerFee(currency).then(fee => {
        this.minerFee = parseFloat(fee).toFixed(8) || parseFloat(DEFAULT_MINER_FEE) // 默认0.0002
      }).catch(() => {
        this.minerFee = parseFloat(DEFAULT_MINER_FEE)
      })
    },

    getAvailableNetworks() {
      // 只支持Localhost:8545本地测试网，其他网络灰化不可点击
      return this.availableNetworks[this.currentWithdraw.currency] || []
    },

    updateNetworkDescription() {
      if (this.currentWithdraw.network) {
        const networks = this.getAvailableNetworks()
        const selectedNetwork = networks.find(network => network.value === this.currentWithdraw.network)
        if (selectedNetwork) {
          this.currentWithdraw.description = selectedNetwork.description
        }
      } else {
        this.currentWithdraw.description = ''
      }
    },

    getCurrentNetworkInfo() {
      if (this.currentWithdraw.network) {
        const networks = this.getAvailableNetworks()
        return networks.find(network => network.value === this.currentWithdraw.network)
      }
      return null
    },

    copyContractAddress(address) {
      navigator.clipboard.writeText(address).then(() => {
        this.$message.success('合约地址已复制到剪贴板')
      }).catch(() => {
        // 降级方案
        const textArea = document.createElement('textarea')
        textArea.value = address
        document.body.appendChild(textArea)
        textArea.select()
        document.execCommand('copy')
        document.body.removeChild(textArea)
        this.$message.success('合约地址已复制到剪贴板')
      })
    },

    getCurrentBalance() {
      const asset = this.assetList.find(a => a.currency === this.currentWithdraw.currency)
      return asset ? asset.balance : '0'
    },

    setMaxAmount() {
      this.currentWithdraw.amount = this.getCurrentBalance();
    },

    formatFee() {
      return this.minerFee;
    },

    getExpectedAmount() {
      const minerFee = this.formatFee();
      return parseFloat(this.currentWithdraw.amount - minerFee) > 0 ? parseFloat(this.currentWithdraw.amount - minerFee) : 0;
    },

    canWithdraw() {
      const minerFee = this.formatFee();
      return this.currentWithdraw.network &&
        this.currentWithdraw.address &&
        this.currentWithdraw.amount &&
        parseFloat(this.currentWithdraw.amount - minerFee) > 0 &&
        parseFloat(this.currentWithdraw.amount) <= parseFloat(this.getCurrentBalance())
    },

    confirmWithdraw() {
      if (!this.canWithdraw()) return

      // 显示地址确认警告
      this.$confirm(
        `请确认提现地址：\n\n${this.currentWithdraw.address}\n\n地址确认无误后，点击"确认提现"继续操作。`,
        '提现地址确认',
        {
          confirmButtonText: '确认提现',
          cancelButtonText: '重新输入',
          type: 'warning',
          dangerouslyUseHTMLString: false,
          customClass: 'withdraw-confirm-dialog'
        }
      ).then(() => {
        // 用户确认地址后，执行提现
        this.executeWithdraw()
      }).catch(() => {
        // 用户取消，不做任何操作
        console.log('用户取消提现')
      }).finally(() => {
        this.withdrawing = false
      })
    },

    executeWithdraw() {

      const currency = this.currentWithdraw.currency;
      if (this.currentWithdraw.currency - this.minerFee < 0) {
        this.$message.error('提现数量不能小于手续费')
        return
      }
      withdraw({
        address: this.currentWithdraw.address,
        amount: this.currentWithdraw.amount,
      }, currency).then((res) => {
        if (res.code === 500) {
          this.$message.error('交易失败，请稍后重试')
          this.withdrawing = false
        } else {
          this.$message.success(this.$t('withdrawSuccess'))
          this.withdrawDialogVisible = false
          this.withdrawing = false
          this.loadAssets()
        }
      })
    },

    handleClose(done) {
      this.withdrawDialogVisible = false
      done()
    },

    getRechargeNetworks() {
      // 根据币种返回可用的充值网络和地址，只支持Localhost:8545本地测试网
      const currency = this.currentRecharge.currency;
      const evmNetworks = this.availableNetworks[currency] || []
      evmNetworks.forEach(network => {
        network.address = this.evmWalletAddress[currency] ?? ''
      })
      return evmNetworks
    },

    selectRechargeNetwork(networkValue) {
      this.currentRecharge.selectedNetwork = networkValue
    },

    copyAddress(address) {
      navigator.clipboard.writeText(address).then(() => {
        this.$message.success('地址已复制到剪贴板')
      }).catch(() => {
        // 降级方案
        const textArea = document.createElement('textarea')
        textArea.value = address
        document.body.appendChild(textArea)
        textArea.select()
        document.execCommand('copy')
        document.body.removeChild(textArea)
        this.$message.success('地址已复制到剪贴板')
      })
    },

    handleRechargeClose(done) {
      this.rechargeDialogVisible = false
      done()
    },
    transferToCollectWallet() {
      if (this.transferToCollectWalletAmount <= 0) {
        this.$message.error('划转数量不能小于0')
        return
      }
      const currency = this.currentWithdraw.currency;
      transferToCollectWallet(currency, this.transferToCollectWalletAmount).then(res => {
        if (res.code === 500) {
          this.$message.error(res.message)
        } else {
          this.$message.success('转移到归集钱包成功')
        }
      })
    }
  }
}
</script>

<style scoped>
.spot-account-section {
  padding: 40px 0;
  min-height: calc(100vh - 200px);
}

.spot-account-header {
  text-align: center;
  margin-bottom: 40px;
}

.spot-account-header h1 {
  font-size: 32px;
  font-weight: bold;
  color: #333;
  margin-bottom: 10px;
}

.spot-account-header p {
  font-size: 16px;
  color: #666;
}

.spot-account-content {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 30px;
}

.spot-account-tabs {
  margin-bottom: 30px;
}

.account-summary {
  display: flex;
  gap: 20px;
  margin-bottom: 40px;
}

.summary-card {
  flex: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 24px;
  border-radius: 8px;
  text-align: center;
}

.summary-title {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 10px;
}

.summary-value {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 5px;
}

.summary-currency {
  font-size: 12px;
  opacity: 0.8;
}

.asset-list {
  margin-top: 30px;
}

.asset-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.asset-list-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.currency-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.currency-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
}

.amount-info {
  text-align: right;
}

.amount {
  font-weight: bold;
  color: #333;
}

.usdt-value {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

@media (max-width: 768px) {
  .account-summary {
    flex-direction: column;
  }

  .spot-account-content {
    padding: 20px;
  }
}

.form-item {
  margin-bottom: 10px;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #333;
}

.currency-display {
  font-weight: bold;
  color: #409EFF;
  font-size: 16px;
}

.fee-display {
  color: #E6A23C;
  font-weight: 500;
}

.balance-info {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.dialog-footer {
  text-align: right;
}

.dialog-footer .el-button {
  margin-left: 10px;
}

/* 禁用的网络选项样式 */
.disabled-network {
  color: #c0c4cc !important;
  cursor: not-allowed !important;
  opacity: 0.6;
}

.disabled-network:hover {
  color: #c0c4cc !important;
  background-color: transparent !important;
  border-color: #e4e7ed !important;
}

.disabled-text {
  color: #c0c4cc !important;
}

.disabled-network .copy-btn {
  color: #c0c4cc !important;
  cursor: not-allowed !important;
}

.disabled-network .copy-btn:hover {
  color: #c0c4cc !important;
}

/* 网络描述样式 */
.network-description {
  margin: 15px 0;
}

.network-description .el-alert {
  margin: 0;
}

/* 合约信息样式 */
.contract-info {
  margin-top: 20px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e9ecef;
}

.contract-info .el-divider {
  margin: 0 0 15px 0;
}

.contract-info .el-divider__text {
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.info-content {
  padding: 0;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  padding: 8px 0;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-label {
  font-weight: 500;
  color: #666;
  min-width: 80px;
  margin-right: 10px;
}

.info-value {
  color: #333;
  font-family: 'Courier New', monospace;
  word-break: break-all;
}

.contract-address {
  background-color: #fff;
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #ddd;
  margin-right: 10px;
  font-size: 12px;
}

.copy-contract-btn {
  color: #409eff;
  padding: 2px 8px;
  font-size: 12px;
}

.copy-contract-btn:hover {
  color: #66b1ff;
}

/* 提现确认弹窗样式 */
:global(.withdraw-confirm-dialog) {
  max-width: 500px;
}

:global(.withdraw-confirm-dialog .el-message-box__content) {
  padding: 20px;
  font-size: 14px;
  line-height: 1.6;
}

:global(.withdraw-confirm-dialog .el-message-box__message) {
  white-space: pre-line;
  word-break: break-all;
  font-family: 'Courier New', monospace;
}

:global(.withdraw-confirm-dialog .el-message-box__btns) {
  padding: 15px 20px 20px;
}

/* 充值弹窗样式 */
.recharge-form {
  padding: 20px 0;
}

.network-address-list {
  max-height: 300px;
  overflow-y: auto;
}

.network-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.3s;
}

.network-item:hover {
  border-color: #409eff;
  background-color: #f5f7fa;
}

.network-item.active {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.network-info {
  flex: 1;
}

.network-name {
  font-weight: 500;
  color: #333;
  margin-bottom: 5px;
}

.network-address {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #666;
  word-break: break-all;
}

.copy-btn {
  margin-left: 10px;
  color: #409eff;
}

.copy-btn:hover {
  color: #66b1ff;
}

/* 自定义tooltip样式 */
:global(.narrow-tooltip) {
  max-width: 200px !important;
}

:global(.narrow-tooltip .el-tooltip__popper) {
  max-width: 200px !important;
  word-wrap: break-word;
  white-space: normal;
}

.actual-amount {
  margin-top: 10px;
  font-size: 14px;
  color: #999;
}

.transfer-to-wallet {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 10px 0;
  border: 1px solid #e4e7ed;
  padding: 10px;
}

.transfer-to-wallet-description {
  font-size: 12px;
  color: #999;
}
</style>