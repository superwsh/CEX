import { ucService as request } from '../request'
import { COIN_DETAIL } from '@/constant/coins.config'
export function checkLogin(data) {
  return request({
    url: '/uc/check/login',
    method: 'POST',
    data: data
  })
}
export function loginOut(data) {
  return request({
    url: '/uc/loginout',
    method: 'POST',
    data: data
  })
}

export function getYZMPic(data) {
  return request({
    url: '/uc/getYZMPic',
    method: 'POST',
    data: data
  })
}
export function checkYZMPic(data) {
  return request({
    url: '/uc/checkYZMPic',
    method: 'POST',
    data: data
  })
}
export function currencyFindAll(data) {
  return request({
    url: '/uc/currency/findAll',
    method: 'GET',
    params: data
  })
}

export function getCountry(data) {
  return request({
    url: '/uc/support/country',
    method: 'POST',
    data: data
  })
}

export function login(data) {
  return request({
    url: '/uc/login',
    method: 'POST',
    data: data
  })
}

export function assetWallet(data) {
  return request({
    url: '/uc/asset/wallet/' + data.symbol,
    method: 'POST',
    data: data
  })
}
export function ucAdvertise(data) {
  return request({
    url: '/uc/ancillary/system/advertise',
    method: 'POST',
    data: data
  })
}
export function checkUsername(data) {
  return request({
    url: '/uc/register/check/username',
    method: 'POST',
    data: data
  })
}

export function resetMobileCode(data) {
  return request({
    url: '/uc/mobile/reset/code',
    method: 'POST',
    data: data
  })
}

export function resetEmailCode(data) {
  return request({
    url: '/uc/reset/email/code',
    method: 'POST',
    data: data
  })
}
export function regEmail(data) {
  return request({
    url: '/uc/register/email',
    method: 'POST',
    data: data
  })
}

export function regPhone(data) {
  return request({
    url: '/uc/register/phone',
    method: 'POST',
    data: data
  })
}

export function getUserInfo() {
  return request({
    url: '/uc/member/my-info',
    method: 'POST',
  })
}

/**
 * 提现
 * @param {*} data 
 * @param {*} coinSymbol 
 * @returns 
 */
export function withdraw(data, coinSymbol) {
  const unit = COIN_DETAIL[coinSymbol].unit;
  const fee = COIN_DETAIL[coinSymbol].minTxFee;
  return request({
    url: '/uc/withdraw/applyDirect',
    method: 'POST',
    data: {
      address: data.address,
      amount: data.amount,
      unit: unit,
      fee: fee
    }
  })
}