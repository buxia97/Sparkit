<template>
	<view class="page-login">
		<view class="logo-area">
			<image class="logo" src="/static/logo.png" mode="aspectFit" />
			<text class="app-name">Sparkit</text>
		</view>

		<!-- 登录方式切换 -->
		<view class="tabs">
			<view
				:class="['tab-item', loginType === 'code' ? 'active' : '']"
				@click="loginType = 'code'"
			>验证码登录</view>
			<view
				:class="['tab-item', loginType === 'password' ? 'active' : '']"
				@click="loginType = 'password'"
			>密码登录</view>
		</view>

		<view class="form">
			<!-- 验证码登录 -->
			<template v-if="loginType === 'code'">
				<view class="form-item">
					<input
						v-model="phone"
						type="number"
						maxlength="11"
						placeholder="请输入手机号"
					/>
				</view>
				<view class="form-item code-row">
					<input
						v-model="code"
						type="number"
						maxlength="6"
						placeholder="请输入验证码"
					/>
					<button
						class="code-btn"
						:disabled="countdown > 0"
						@click="sendCode"
					>
						{{ countdown > 0 ? countdown + 's' : '获取验证码' }}
					</button>
				</view>
			</template>

			<!-- 密码登录 -->
			<template v-else>
				<view class="form-item">
					<input
						v-model="account"
						placeholder="请输入手机号/邮箱/用户名"
					/>
				</view>
				<view class="form-item">
					<input
						v-model="password"
						type="password"
						placeholder="请输入密码"
					/>
				</view>
			</template>

			<button class="login-btn" @click="handleLogin">
				{{ submitting ? '登录中...' : '登录' }}
			</button>
		</view>

		<view class="bottom-links">
			<text @click="goRegister">没有账号？立即注册</text>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { useUserStore } from '@/store/user.js'

const userStore = useUserStore()

const loginType = ref('code')
const phone = ref('')
const code = ref('')
const account = ref('')
const password = ref('')
const countdown = ref(0)
const submitting = ref(false)

/**
 * 发送验证码
 */
async function sendCode() {
	if (!phone.value || phone.value.length !== 11) {
		uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
		return
	}
	try {
		await userStore.sendVerifyCode(phone.value, 'phone')
		uni.showToast({ title: '验证码已发送', icon: 'success' })
		// 开始倒计时
		countdown.value = 60
		const timer = setInterval(() => {
			countdown.value--
			if (countdown.value <= 0) {
				clearInterval(timer)
			}
		}, 1000)
	} catch (e) {
		// 错误已在 api 层处理
	}
}

/**
 * 登录
 */
async function handleLogin() {
	submitting.value = true
	try {
		if (loginType.value === 'code') {
			if (!phone.value) {
				uni.showToast({ title: '请输入手机号', icon: 'none' })
				return
			}
			if (!code.value) {
				uni.showToast({ title: '请输入验证码', icon: 'none' })
				return
			}
			await userStore.loginByPhone(phone.value, code.value)
		} else {
			if (!account.value) {
				uni.showToast({ title: '请输入账号', icon: 'none' })
				return
			}
			if (!password.value) {
				uni.showToast({ title: '请输入密码', icon: 'none' })
				return
			}
			await userStore.loginByPassword(account.value, password.value)
		}

		uni.showToast({ title: '登录成功', icon: 'success' })
		setTimeout(() => {
			uni.switchTab({ url: '/pages/user/index' })
		}, 500)
	} catch (e) {
		// 错误已在 api 层提示
	} finally {
		submitting.value = false
	}
}

/**
 * 跳转注册页
 */
function goRegister() {
	uni.navigateTo({ url: '/pages/user/register' })
}
</script>

<style lang="scss" scoped>
.page-login {
	min-height: 100vh;
	background: #f5f5f5;
	padding: 60rpx 40rpx;
}

.logo-area {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-bottom: 60rpx;

	.logo {
		width: 120rpx;
		height: 120rpx;
		border-radius: 24rpx;
	}

	.app-name {
		margin-top: 20rpx;
		font-size: 40rpx;
		font-weight: bold;
		color: #333;
	}
}

.tabs {
	display: flex;
	margin-bottom: 40rpx;

	.tab-item {
		flex: 1;
		text-align: center;
		padding: 20rpx;
		font-size: 32rpx;
		color: #999;
		border-bottom: 4rpx solid transparent;
		transition: all 0.3s;

		&.active {
			color: #007aff;
			border-bottom-color: #007aff;
		}
	}
}

.form {
	.form-item {
		background: #fff;
		border-radius: 12rpx;
		margin-bottom: 24rpx;
		padding: 0 24rpx;

		input {
			height: 88rpx;
			font-size: 28rpx;
		}
	}

	.code-row {
		display: flex;
		align-items: center;

		input {
			flex: 1;
		}

		.code-btn {
			flex-shrink: 0;
			height: 64rpx;
			line-height: 64rpx;
			padding: 0 20rpx;
			font-size: 24rpx;
			color: #007aff;
			background: transparent;
			border: 1px solid #007aff;
			border-radius: 8rpx;

			&[disabled] {
				color: #999;
				border-color: #ddd;
			}
		}
	}

	.login-btn {
		margin-top: 40rpx;
		height: 88rpx;
		line-height: 88rpx;
		background: #007aff;
		color: #fff;
		font-size: 32rpx;
		border-radius: 12rpx;
	}
}

.bottom-links {
	text-align: center;
	margin-top: 40rpx;

	text {
		color: #007aff;
		font-size: 28rpx;
	}
}
</style>
