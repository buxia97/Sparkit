<template>
	<view class="page-profile">
		<view class="section-title">基本资料</view>
		<view class="info-card">
			<view class="info-item">
				<text class="label">头像</text>
				<image class="avatar" :src="form.avatar || '/static/logo.png'" mode="aspectFill" @click="uploadAvatar" />
			</view>
			<view class="info-item">
				<text class="label">昵称</text>
				<input v-model="form.nickname" placeholder="请输入昵称" />
			</view>
			<view class="info-item">
				<text class="label">手机号</text>
				<text class="value">{{ form.phone || '未绑定' }}</text>
			</view>
			<view class="info-item">
				<text class="label">邮箱</text>
				<text class="value">{{ form.email || '未绑定' }}</text>
			</view>
		</view>

		<view class="section-title">安全设置</view>
		<view class="info-card">
			<view class="info-item" @click="showChangePwd = true">
				<text class="label">修改密码</text>
				<text class="value arrow">修改 ></text>
			</view>
		</view>

		<button class="save-btn" @click="saveProfile">保存修改</button>

		<!-- 修改密码弹窗 -->
		<view class="modal-mask" v-if="showChangePwd" @click="showChangePwd = false">
			<view class="modal-content" @click.stop>
				<text class="modal-title">修改密码</text>
				<input v-model="oldPassword" type="password" placeholder="原密码" />
				<input v-model="newPassword" type="password" placeholder="新密码" />
				<input v-model="confirmNewPassword" type="password" placeholder="确认新密码" />
				<view class="modal-btns">
					<button class="cancel-btn" @click="showChangePwd = false">取消</button>
					<button class="confirm-btn" @click="changePassword">确认</button>
				</view>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/store/user.js'
import api from '@/common/api.js'

const userStore = useUserStore()

const form = ref({
	avatar: '',
	nickname: '',
	phone: '',
	email: ''
})

const showChangePwd = ref(false)
const oldPassword = ref('')
const newPassword = ref('')
const confirmNewPassword = ref('')

onMounted(() => {
	if (userStore.userInfo) {
		form.value.nickname = userStore.userInfo.nickname || ''
		form.value.phone = userStore.userInfo.phone || ''
		form.value.email = userStore.userInfo.email || ''
		form.value.avatar = userStore.userInfo.avatar || ''
	}
})

/**
 * 上传头像
 */
function uploadAvatar() {
	uni.chooseImage({
		count: 1,
		sizeType: ['compressed'],
		success(res) {
			const filePath = res.tempFilePaths[0]
			api.uploadFile('/api/v1/public/storage/upload', filePath, 'file')
				.then(data => {
					form.value.avatar = data.url || filePath
					uni.showToast({ title: '头像上传成功', icon: 'success' })
				})
				.catch(() => {})
		}
	})
}

/**
 * 保存资料
 */
async function saveProfile() {
	try {
		await api.put('/api/v1/public/user/profile', {
			nickname: form.value.nickname,
			avatar: form.value.avatar
		})
		// 更新本地存储中的用户信息
		if (userStore.userInfo) {
			userStore.userInfo.nickname = form.value.nickname
			userStore.userInfo.avatar = form.value.avatar
			try {
				uni.setStorageSync('userInfo', JSON.stringify(userStore.userInfo))
			} catch (e) { /* ignore */ }
		}
		uni.showToast({ title: '保存成功', icon: 'success' })
	} catch (e) {
		// 错误已在 api 层处理
	}
}

/**
 * 修改密码
 */
async function changePassword() {
	if (!oldPassword.value) {
		uni.showToast({ title: '请输入原密码', icon: 'none' })
		return
	}
	if (!newPassword.value || newPassword.value.length < 6) {
		uni.showToast({ title: '新密码至少6位', icon: 'none' })
		return
	}
	if (newPassword.value !== confirmNewPassword.value) {
		uni.showToast({ title: '两次密码不一致', icon: 'none' })
		return
	}

	try {
		await api.put('/api/v1/public/user/password', {
			oldPassword: oldPassword.value,
			newPassword: newPassword.value
		})
		uni.showToast({ title: '密码修改成功', icon: 'success' })
		showChangePwd.value = false
	} catch (e) {
		// 错误已在 api 层处理
	}
}
</script>

<style lang="scss" scoped>
.page-profile {
	min-height: 100vh;
	background: #f5f5f5;
	padding: 20rpx;
}

.section-title {
	padding: 30rpx 10rpx 16rpx;
	font-size: 26rpx;
	color: #999;
}

.info-card {
	background: #fff;
	border-radius: 16rpx;
	overflow: hidden;
}

.info-item {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 28rpx 30rpx;
	border-bottom: 1rpx solid #f0f0f0;

	&:last-child {
		border-bottom: none;
	}

	.label {
		font-size: 28rpx;
		color: #333;
	}

	.value {
		font-size: 28rpx;
		color: #999;

		&.arrow {
			color: #007aff;
		}
	}

	input {
		text-align: right;
		font-size: 28rpx;
		color: #333;
		flex: 1;
	}

	.avatar {
		width: 80rpx;
		height: 80rpx;
		border-radius: 50%;
		background: #eee;
	}
}

.save-btn {
	margin: 40rpx 20rpx;
	height: 88rpx;
	line-height: 88rpx;
	background: #007aff;
	color: #fff;
	font-size: 32rpx;
	border-radius: 12rpx;
}

/* 弹窗样式 */
.modal-mask {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background: rgba(0, 0, 0, 0.5);
	display: flex;
	align-items: center;
	justify-content: center;
	z-index: 999;
}

.modal-content {
	width: 600rpx;
	background: #fff;
	border-radius: 16rpx;
	padding: 40rpx;

	.modal-title {
		font-size: 32rpx;
		font-weight: bold;
		text-align: center;
		margin-bottom: 30rpx;
	}

	input {
		border: 1rpx solid #e0e0e0;
		border-radius: 8rpx;
		padding: 20rpx;
		margin-bottom: 20rpx;
		font-size: 28rpx;
	}

	.modal-btns {
		display: flex;
		justify-content: space-between;
		margin-top: 20rpx;

		button {
			flex: 1;
			height: 72rpx;
			line-height: 72rpx;
			font-size: 28rpx;
			border-radius: 8rpx;
		}

		.cancel-btn {
			background: #f5f5f5;
			color: #666;
			margin-right: 20rpx;
		}

		.confirm-btn {
			background: #007aff;
			color: #fff;
		}
	}
}
</style>
