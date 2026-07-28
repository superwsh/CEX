import axios from 'axios'
import {
	Message
} from 'element-ui'
const defaultConfig = {
	timeout: 100000,
	headers: {
		'Content-Type': 'application/x-www-form-urlencoded',
		'lang': 'en_US'
	}
}

const marketService = axios.create({
	baseURL: `${process.env.VUE_APP_MARKET_API_URL}/`,
	...defaultConfig
})
const ucService = axios.create({
	baseURL: `${process.env.VUE_APP_UC_API_URL}/`,
	...defaultConfig
})
const exchangeService = axios.create({
	baseURL: `${process.env.VUE_APP_EXCHANGE_API_URL}/`,
	...defaultConfig
});
const walletService = axios.create({
	baseURL: `${process.env.VUE_APP_WALLET_API_URL}/`,
	...defaultConfig
});

function middleware(service) {
	// token 和 lang 的设置
	service.interceptors.request.use(config => {
		let token = localStorage.getItem('token')
		let lang = localStorage.getItem('lang')
		if (token) {
			config.headers['x-auth-token'] = token
		}
		if (lang) {
			config.headers['lang'] = lang
		}
		return config
	}, error => {
		Promise.reject(error)
	})
	// 登陆状态设置
	service.interceptors.response.use(
		response => {
			const res = response.data
			if (res.code == 4000) {
				window.location.href = "#/login";
			}
			return res;
		},
		error => {
			Message({
				message: error.message,
				type: 'error',
				duration: 3000
			})
			return Promise.reject(error)
		})
}

[marketService, ucService, exchangeService, walletService].forEach(middleware)
export {
	marketService,
	ucService,
	exchangeService,
	walletService
}
