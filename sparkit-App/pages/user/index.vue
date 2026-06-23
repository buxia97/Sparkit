<template>
	<view class="page-user-center">
		<!-- 用户头部 -->
		<view class="user-header">
			<image class="avatar" :src="userStore.avatar" mode="aspectFill" />
			<text class="nickname">{{ userStore.nickname }}</text>
			<text class="uid" v-if="userStore.userId">ID: {{ userStore.userId }}</text>
			<button v-if="!userStore.isLogin" class="go-login-btn" @click="goLogin">
				登录 / 注册
			</button>
		</view>

		<!-- 功能菜单 -->
		<view class="menu-section" v-if="userStore.isLogin">
			<view class="section-title">我的服务</view>
			<view class="menu-list">
				<view class="menu-item" @click="goPage('/pages/user/profile')">
					<text class="menu-icon">&#xe601;</text>
					<text class="menu-label">个人信息</text>
					<text class="menu-arrow">></text>
				</view>
				<view class="menu-item" @click="goPage('/pages/user/upload')">
					<text class="menu-icon">&#xe602;</text>
					<text class="menu-label">文件上传</text>
					<text class="menu-arrow">></text>
				</view>
				<view class="menu-item" @click="goPage('/pages/user/payment')">
					<text class="menu-icon">&#xe603;</text>
					<text class="menu-label">充值中心</text>
					<text class="menu-arrow">></text>
				</view>
				<view class="menu-item" @click="goPage('/pages/user/messages')">
					<text class="menu-icon">&#xe604;</text>
					<text class="menu-label">消息中心</text>
					<view class="menu-right">
						<text class="badge" v-if="userStore.unreadCount > 0">{{ userStore.unreadCount }}</text>
						<text class="menu-arrow">></text>
					</view>
				</view>
				<view class="menu-item" @click="goPage('/pages/user/ai-chat')">
					<text class="menu-icon">&#xe605;</text>
					<text class="menu-label">AI 助手</text>
					<text class="menu-arrow">></text>
				</view>
			</view>
		</view>

		<!-- 退出登录 -->
		<view class="logout-section" v-if="userStore.isLogin">
			<button class="logout-btn" @click="handleLogout">退出登录</button>
		</view>
	</view>
</template>

<script setup>
import { onShow } from 'vue'
import { useUserStore } from '@/store/user.js'

const userStore = useUserStore()

onShow(() => {
	userStore.initFromStorage()
})

/**
 * 跳转页面
 */
function goPage(url) {
	uni.navigateTo({ url })
}

/**
 * 跳转登录页
 */
function goLogin() {
	uni.navigateTo({ url: '/pages/user/login' })
}

/**
 * 退出登录
 */
function handleLogout() {
	uni.showModal({
		title: '提示',
		content: '确定退出登录？',
		success(res) {
			if (res.confirm) {
				userStore.logout()
				uni.showToast({ title: '已退出登录', icon: 'success' })
			}
		}
	})
}
</script>

<style lang="scss" scoped>
.page-user-center {
	min-height: 100vh;
	background: #f5f5f5;
}

.user-header {
	background: linear-gradient(135deg, #007aff, #5856d6);
	padding: 60rpx 40rpx 40rpx;
	display: flex;
	flex-direction: column;
	align-items: center;

	.avatar {
		width: 120rpx;
		height: 120rpx;
		border-radius: 50%;
		border: 4rpx solid rgba(255, 255, 255, 0.5);
		background: #ccc;
	}

	.nickname {
		margin-top: 20rpx;
		font-size: 36rpx;
		color: #fff;
		font-weight: bold;
	}

	.uid {
		margin-top: 8rpx;
		font-size: 24rpx;
		color: rgba(255, 255, 255, 0.7);
	}

	.go-login-btn {
		margin-top: 30rpx;
		background: rgba(255, 255, 255, 0.2);
		color: #fff;
		border: 2rpx solid rgba(255, 255, 255, 0.5);
		border-radius: 40rpx;
		font-size: 28rpx;
		padding: 12rpx 60rpx;
	}
}

.menu-section {
	margin: 20rpx;
	background: #fff;
	border-radius: 16rpx;
	overflow: hidden;

	.section-title {
		padding: 24rpx 30rpx 12rpx;
		font-size: 26rpx;
		color: #999;
	}

	.menu-list {
		.menu-item {
			display: flex;
			align-items: center;
			padding: 28rpx 30rpx;
			border-bottom: 1rpx solid #f0f0f0;

			&:last-child {
				border-bottom: none;
			}

			.menu-icon {
				font-size: 36rpx;
				margin-right: 20rpx;
				color: #007aff;
			}

			.menu-label {
				flex: 1;
				font-size: 30rpx;
				color: #333;
			}

			.menu-right {
				display: flex;
				align-items: center;
			}

			.menu-arrow {
				font-size: 28rpx;
				color: #ccc;
			}

			.badge {
				background: #ff3b30;
				color: #fff;
				font-size: 20rpx;
				padding: 4rpx 12rpx;
				border-radius: 20rpx;
				margin-right: 12rpx;
			}
		}
	}
}

.logout-section {
	padding: 40rpx 20rpx;

	.logout-btn {
		background: #fff;
		color: #ff3b30;
		font-size: 30rpx;
		border-radius: 12rpx;
	}
}
</style>
