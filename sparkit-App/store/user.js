/**
 * 用户状态管理 - Pinia Store
 * 管理登录态、Token、用户信息
 */
import { defineStore } from 'pinia'
import api from '@/common/api.js'

export const useUserStore = defineStore('user', {
	state: () => ({
		// 令牌
		accessToken: '',
		refreshToken: '',
		// 用户信息
		userInfo: null,
		// 是否已登录
		isLogin: false,
		// 未读消息数
		unreadCount: 0
	}),

	getters: {
		// 用户 ID
		userId: (state) => state.userInfo?.id || '',
		// 用户昵称
		nickname: (state) => state.userInfo?.nickname || '未登录',
		// 用户头像
		avatar: (state) => state.userInfo?.avatar || '/static/logo.png'
	},

	actions: {
		/**
		 * 初始化：从本地存储恢复登录态
		 */
		initFromStorage() {
			try {
				const accessToken = uni.getStorageSync('accessToken')
				const refreshToken = uni.getStorageSync('refreshToken')
				const userInfo = uni.getStorageSync('userInfo')
				if (accessToken) {
					this.accessToken = accessToken
					this.refreshToken = refreshToken || ''
					this.userInfo = userInfo ? JSON.parse(userInfo) : null
					this.isLogin = true
				}
			} catch (e) {
				// ignore
			}
		},

		/**
		 * 保存登录态到本地
		 */
		_saveToStorage() {
			try {
				uni.setStorageSync('accessToken', this.accessToken)
				uni.setStorageSync('refreshToken', this.refreshToken)
				uni.setStorageSync('userInfo', JSON.stringify(this.userInfo || {}))
			} catch (e) {
				// ignore
			}
		},

		/**
		 * 发送验证码
		 * @param {string} target - 手机号或邮箱
		 * @param {string} type - phone / email
		 */
		async sendVerifyCode(target, type = 'phone') {
			return await api.post('/api/v1/public/user/verify-code', {
				target,
				type
			})
		},

		/**
		 * 手机验证码登录
		 */
		async loginByPhone(phone, code) {
			const data = await api.post('/api/v1/public/user/login/phone', {
				phone,
				code
			})
			this._setLoginData(data)
			return data
		},

		/**
		 * 账号密码登录
		 */
		async loginByPassword(account, password) {
			const data = await api.post('/api/v1/public/user/login/password', {
				account,
				password
			})
			this._setLoginData(data)
			return data
		},

		/**
		 * 手机号注册
		 */
		async registerByPhone(phone, password, code) {
			const data = await api.post('/api/v1/public/user/register/phone', {
				phone,
				password,
				code
			})
			this._setLoginData(data)
			return data
		},

		/**
		 * 邮箱注册
		 */
		async registerByEmail(email, password, code) {
			const data = await api.post('/api/v1/public/user/register/email', {
				email,
				password,
				code
			})
			this._setLoginData(data)
			return data
		},

		/**
		 * 设置登录数据
		 */
		_setLoginData(data) {
			if (data && data.accessToken) {
				this.accessToken = data.accessToken
				this.refreshToken = data.refreshToken || ''
				this.userInfo = data.userInfo || data
				this.isLogin = true
				this._saveToStorage()
			}
		},

		/**
		 * 退出登录
		 */
		logout() {
			this.accessToken = ''
			this.refreshToken = ''
			this.userInfo = null
			this.isLogin = false
			this.unreadCount = 0
			try {
				uni.removeStorageSync('accessToken')
				uni.removeStorageSync('refreshToken')
				uni.removeStorageSync('userInfo')
			} catch (e) {
				// ignore
			}
		}
	}
})
