<template>
	<div class="page-web page-home">
		<Head />
		<div class="index-banner">
			<div class="container">
				<div class="index-grid flex">
					<div class="grid-photo">
						<img src="../../assets/photo/web/banner.svg" alt="" class="wow fadeInRight animated" data-wow-duration="1s" data-wow-delay="0.2s">
					</div>
					<div class="flex_bd">
						<div class="grid-title" id="element"></div>
						<div class="grid-desc">{{$t('indexTxt2')}}</div>
					</div>
				</div>
				<div class="index-attr flex">
					<div @click="toUrl(`/exchange/${getDefaultExchangeRoute()}`)" class="animated col" data-wow-duration="1s" data-wow-delay="0.2s">
						<h3 class="shake-chunk">{{$t('indexLabel1')}}</h3>
						<p>{{$t('indexVal1')}}</p>
					</div>
					<div class="line"></div>
					<div @click="toUrl('/spot-account')" class="animated col" data-wow-duration="1s" data-wow-delay="0.2s">
						<h3 class="shake-chunk">{{$t('spotAccount')}}</h3>
						<p>{{$t('spotAccountDesc')}}</p>
					</div>
				</div>
			</div>
		</div>
	</div>
</template>

<script>
	import {
		ucAdvertise
	} from '@/api/api/user'
	import Head from '@/components/Head.vue'
	import { WOW } from 'wowjs'
	import Typed from 'typed.js'
	import { EXCHANGE_CONFIG } from '@/constant/coins.config'
	export default {
		name: 'Home',
		components: {
			Head,
		},
		data() {
			return {
				isLogin: false,
			};
		},
		created() {
			this.isLogin = this.$store.state.isLogin
		},
		mounted() {
			this.getUser()
			this.getAdv()
			
			setTimeout(()=>{
			 let wow = new WOW({
				boxClass: 'wow',
				animateClass: 'animated',
				offset: 0,
				mobile: true,
				live: false,
			})
			wow.init()
			new Typed('#element', {
				strings: [this.$t('indexTxt2')],
				typeSpeed: 100,
			})
			})
			
		},
		methods: {
			getDefaultExchangeRoute() {
				return EXCHANGE_CONFIG['eth_usdt'].route
			},
			getAdv() {
				ucAdvertise({
					sysAdvertiseLocation: 1,
					lang: localStorage.getItem('lang')
				}).then(res => {
					if (res.code == 0) {
						this.adList = res.data
					}
				})
			},
			getUser() {
				this.user = this.$store.state.user;
			},
			toExchange(val) {
				var str = val.toLowerCase()
				var arr = str.split('/')
				var unit = arr[0] + '_' + arr[1]
				this.$router.push({
					path: '/exchange/' + unit
				})
			},
			toUrl(url) {
				this.$router.push(url)
			},
		},
	}
</script>
<style scoped>
::v-deep .typed-cursor{
	display: none !important;
}
.index-download .flex_bd{
	padding-right: 80px;
}
@media (max-width: 700px) {
	.index-download .flex_bd{
	padding-right: 0;
}
}
</style>
