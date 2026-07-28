import { walletService as request } from '../request'
import { COIN_DETAIL } from '@/constant/coins.config'

const createRpc = (coinSymbol, path) => {
    const requestName = COIN_DETAIL[coinSymbol].restfulApi;
    const baseUrl = COIN_DETAIL[coinSymbol].baseUrl;
    if (!requestName) {
        throw new Error(`Unsupported coin: ${coinSymbol}`)
    }
    return `${baseUrl}/${requestName}/rpc/${path}`
}

/**
 * 获取钱包余额
 * @returns 
 */
export function walletBalance(coinSymbol, account) {
    return request({
        url: createRpc(coinSymbol, `balance/${account}`),
        method: 'GET',
    })
}

/**
 * 创建用户钱包/获取钱包地址
 * 如果已创建钱包则返回钱包地址
 * @param {*} coinSymbol 
 * @param {*} account 
 * @returns 
 */
export function createWallet(coinSymbol, account) {
    if(!coinSymbol || !account) {
        return Promise.reject('coin and account are required')
    }
    return request({
        url: createRpc(coinSymbol, `address/${account}`),
        method: 'GET',
    })
}

/**
 * 转账到归集地址
 */
export function transferToCollectWallet(coinSymbol, amount) {
    const transferAddress = COIN_DETAIL[coinSymbol].transferAddress;
    return request({
        url: createRpc(coinSymbol, `transfer`),
        method: 'GET',
        params: {
            // 使用固定的归集地址
            address: transferAddress,
            amount: amount
        }
    })
}

/**
 * 获取提现手续费
 * @param {*} coinSymbol 
 * @returns 
 */
export function getMinerFee(coinSymbol) {
    const minTxFee = COIN_DETAIL[coinSymbol].minTxFee;
    return Promise.resolve(minTxFee)
}

// export function testApi() {
//     return request({
//         url: '/uc/withdraw/support/coin/info',
//         method: 'POST',
//         baseURL: 'http://127.0.0.1:6001'
//     })
//     return request({
//         url: '/uc/withdraw/apply',
//         method: 'POST',
//         baseURL: 'http://127.0.0.1:6001',
//         data: {
//             unit: 'ETH',
//             address: '0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266',
//             amount: 0.1,
//             fee: 0.005,
//             remark: 'test',
//             jyPassword: '12345678'
//         }
//     })
// }