const TRANSFER_ADDRESS = '0x260db3aa6b460505e25702ea0129af01a6306b59'

const EXCHANGE_CONFIG = {
    'eth_usdt': {
        symbol: 'ETH/USDT',
        coin: 'ETH',
        base: 'USDT',
        route: 'eth_usdt',
    },
    'btc_usdt': {
        symbol: 'BTC/USDT',
        coin: 'BTC',
        base: 'USDT',
        route: 'btc_usdt',
    },
}

const COIN_DETAIL = {
    ETH: {
        symbol: 'ETH',
        restfulApi: 'eth',
        supported: true,
        transferAddress: TRANSFER_ADDRESS, // 归集地址
        minTxFee: 0.002,
        unit: 'ETH',
        baseUrl: process.env.VUE_APP_ETH_WALLET_API_URL,
        networks: [
            { value: 'localhost:8545', label: 'Localhost:8545 (本地EVM测试网)', disabled: false, description: '', info: '****' },
            { value: 'ETH', label: 'Ethereum (ETH)', disabled: true, description: '此网络暂不支持，请选择其他网络', info: '****' },
            { value: 'BSC', label: 'Binance Smart Chain (BSC)', disabled: true, description: '此网络暂不支持，请选择其他网络', info: '****' }
        ]
    },
    BTC: {
        symbol: 'BTC',
        restfulApi: 'btc',
        unit: 'BTC',
        supported: false,
        transferAddress: TRANSFER_ADDRESS,
        // baseUrl:process.env.VUE_APP_WALLET_API_URL,
        networks: [
            { value: 'localhost:8545', label: 'Localhost:8545 (本地EVM测试网)', disabled: false, description: '', info: 'WBTC合约地址：****' },
            { value: 'ETH', label: 'Ethereum (ERC20)', disabled: true, description: '此网络暂不支持，请选择其他网络', info: 'WBTC合约地址：0x2260FAC5E5542a773Aa44fBCfeDf7C193bc2C599' },
            { value: 'BSC', label: 'Binance Smart Chain (BEP20)', disabled: true, description: '此网络暂不支持，请选择其他网络', info: 'WBTC合约地址：0x7130d2A12B9BCbFAe4f2634d864A1Ee1Ce3Ead9c' }
        ]
    },
    USDT: {
        symbol: 'USDT',
        restfulApi: 'usdt',
        unit: 'USDT',
        supported: true,
        minTxFee: 4,
        transferAddress: TRANSFER_ADDRESS,
        baseUrl: process.env.VUE_APP_USDT_WALLET_API_URL,
        networks: [
            { value: 'localhost:8545', label: 'Localhost:8545 (本地EVM测试网)', disabled: false, description: '', info: 'USDT合约地址：****' },
            { value: 'ETH', label: 'Ethereum (ERC20)', disabled: true, description: '此网络暂不支持，请选择其他网络', info: 'USDT合约地址：0xdAC17F958D2ee523a2206206994597C13D831ec7' },
            { value: 'BSC', label: 'Binance Smart Chain (BEP20)', disabled: true, description: '此网络暂不支持，请选择其他网络', info: 'USDT合约地址：0x55d398326f99059fF775485246999027B3197955' },
            { value: 'TRC20', label: 'Tron (TRC20)', disabled: true, description: '此网络暂不支持，请选择其他网络', info: 'USDT合约地址：TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t' }
        ]
    }
}

const DEFAULT_MINER_FEE = '0.0002';

export { DEFAULT_MINER_FEE, COIN_DETAIL, EXCHANGE_CONFIG }