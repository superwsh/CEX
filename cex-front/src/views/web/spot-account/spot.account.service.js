import { COIN_DETAIL } from '@/constant/coins.config'
import { createWallet } from '@/api/api/wallet'
import { assetWallet } from '@/api/api/user'

const filterSupportedCoin = (coinKey) => {
    return COIN_DETAIL[coinKey].supported
}

const getNetworkKeysList = () => {
    return Object.keys(COIN_DETAIL).filter(filterSupportedCoin)
}

/**
 * 每种代币创建一个钱包
 */
const createWalletList = (memberId) => {
    return getNetworkKeysList().map(key => {
        return createWallet(COIN_DETAIL[key].symbol, memberId)
    })
}

/**
 * 获取用户钱包余额
 * 每种代币对应一个钱包
 */
const createWalletBalanceList = () => {
    return getNetworkKeysList().map(key => {
        return assetWallet({ symbol: COIN_DETAIL[key].symbol })
    })
}
const getNetworkNameByIndex = (index) => {
    return getNetworkKeysList()[index]
}

// vue data初始化
const getDefaultWalletAddress = () => getNetworkKeysList().reduce((acc, key) => {
    acc[key] = ''
    return acc
}, {})

const getDefaultAvailableNetworks = () => getNetworkKeysList().reduce((acc, key) => {
    acc[key] = COIN_DETAIL[key].networks
    return acc
}, {})

const getDefaultAssetList = () => getNetworkKeysList().map(key => ({
    currency: COIN_DETAIL[key].symbol,
    balance: '0',
    usdtValue: 0
}))


export {
    getNetworkKeysList,
    createWalletList,
    createWalletBalanceList,
    getNetworkNameByIndex,
    getDefaultWalletAddress,
    getDefaultAvailableNetworks,
    getDefaultAssetList
}