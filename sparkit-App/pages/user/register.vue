<template>
	<view class="page-register">
		<view class="header">
			<text class="title">注册账号</text>
		</view>

		<!-- 注册方式切换 -->
		<view class="tabs">
			<view
				:class="['tab-item', regType === 'phone' ? 'active' : '']"
				@click="regType = 'phone'"
			>手机号注册</view>
			<view
				:class="['tab-item', regType === 'email' ? 'active' : '']"
				@click="regType = 'email'"
			>邮箱注册</view>
		</view>

		<view class="form">
			<!-- 手机号注册 -->
			<template v-if="regType === 'phone'">
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

			<!-- 邮箱注册 -->
			<template v-else>
				<view class="form-item">
					<input
						v-model="email"
						placeholder="请输入邮箱地址"
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

			<view class="form-item">
				<input
					v-model="password"
					type="password"
					placeholder="请设置密码（6-20位）"
				/>
			</view>

			<view class="form-item">
				<input
					v-model="confirmPassword"
					type="password"
					placeholder="请确认密码"
				/>
			</view>

			<button class="register-btn" @click="handleRegister">
				{{ submitting ? '注册中...' : '注册' }}
			</button>
		</view>

		<view class="bottom-links">
			<text @click="goLogin">已有账号？立即登录</text>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import { useUserStore } from '@/store/user.js'

const userStore = useUserStore()

const regType = ref('phone')
const phone = ref('')
const email = ref('')
const code = ref('')
const password = ref('')
const confirmPassword = ref('')
const countdown = ref(0)
const submitting = ref(false)

/**
 * 发送验证码
 */
async function sendCode() {
	const target = regType.value === 'phone' ? phone.value : email.value
	const type = regType.value

	if (type === 'phone' && (!target || target.length !== 11)) {
		uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
		return
	}
	if (type === 'email' && (!target || !target.includes('@'))) {
		uni.showToast({ title: '请输入正确的邮箱', icon: 'none' })
		return
	}

	try {
		await userStore.sendVerifyCode(target, type)
		uni.showToast({ title: '验证码已发送', icon: 'success' })
		countdown.value = 60
		const timer = setInterval(() => {
			countdown.value--
			if (countdown.value <= 0) clearInterval(timer)
		}, 1000)
	} catch (e) {
		// 错误已在 api 层处理
	}
}

/**
 * 注册
 */
async function handleRegister() {
	if (!code.value) {
		uni.showToast({ title: '请输入验证码', icon: 'none' })
		return
	}
	if (!password.value || password.value.length < 6) {
		uni.showToast({ title: '密码至少6位', icon: 'none' })
		return
	}
	if (password.value !== confirmPassword.value) {
		uni.showToast({ title: '两次密码不一致', icon: 'none' })
		return
	}

	submitting.value = true
	try {
		if (regType.value === 'phone') {
			if (!phone.value || phone.value.length !== 11) {
				uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
				return
			}
			await userStore.registerByPhone(phone.value, password.value, code.value)
		} else {
			if (!email.value || !email.value.includes('@')) {
				uni.showToast({ title: '请输入正确的邮箱', icon: 'none' })
				return
			}
			await userStore.registerByEmail(email.value, password.value, code.value)
		}

		uni.showToast({ title: '注册成功', icon: 'success' })
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
 * 跳转登录页
 */
function goLogin() {
	uni.navigateTo({ url: '/pages/user/login' })
}
</script>

<style lang="scss" scoped>
.page-register {
	min-height: 100vh;
	background: #f5f5f5;
	padding: 40rpx;
}

.header {
	padding: 40rpx 0;

	.title {
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

	.register-btn {
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
