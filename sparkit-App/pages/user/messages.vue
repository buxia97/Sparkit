<template>
	<view class="page-messages">
		<!-- 未读数 -->
		<view class="header-bar" v-if="unreadCount > 0">
			<text>您有 {{ unreadCount }} 条未读消息</text>
			<text class="read-all" @click="readAll">全部已读</text>
		</view>

		<view class="message-list">
			<view
				v-for="msg in messages"
				:key="msg.id"
				:class="['message-item', { unread: !msg.isRead }]"
				@click="goDetail(msg)"
			>
				<view class="msg-header">
					<text class="msg-title">{{ msg.title }}</text>
					<view class="unread-dot" v-if="!msg.isRead"></view>
				</view>
				<text class="msg-content">{{ msg.content }}</text>
				<text class="msg-time">{{ msg.createTime }}</text>

				<view class="msg-actions">
					<text class="action-item" @click.stop="markRead(msg)">标为已读</text>
					<text class="action-item danger" @click.stop="deleteMsg(msg)">删除</text>
				</view>
			</view>

			<view class="empty" v-if="messages.length === 0">
				<text>暂无消息</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/common/api.js'

const messages = ref([])
const unreadCount = ref(0)

onMounted(() => {
	fetchMessages()
	fetchUnreadCount()
})

/**
 * 获取消息列表
 */
async function fetchMessages() {
	try {
		const data = await api.get('/api/v1/public/site-message', {
			page: 1,
			pageSize: 20
		})
		messages.value = data?.records || []
	} catch (e) {
		// ignore
	}
}

/**
 * 获取未读数
 */
async function fetchUnreadCount() {
	try {
		const data = await api.get('/api/v1/public/site-message/unread-count')
		unreadCount.value = data?.count || 0
	} catch (e) {
		// ignore
	}
}

/**
 * 标记已读
 */
async function markRead(msg) {
	try {
		await api.put(`/api/v1/public/site-message/${msg.id}/read`)
		msg.isRead = true
		unreadCount.value = Math.max(0, unreadCount.value - 1)
		uni.showToast({ title: '已标记为已读', icon: 'success' })
	} catch (e) {
		// ignore
	}
}

/**
 * 全部标记已读
 */
async function readAll() {
	try {
		await api.put('/api/v1/public/site-message/read-all')
		messages.value.forEach(m => m.isRead = true)
		unreadCount.value = 0
		uni.showToast({ title: '全部已读', icon: 'success' })
	} catch (e) {
		// ignore
	}
}

/**
 * 删除消息
 */
async function deleteMsg(msg) {
	uni.showModal({
		title: '提示',
		content: '确定删除此消息？',
		success: async (res) => {
			if (res.confirm) {
				try {
					await api.del(`/api/v1/public/site-message/${msg.id}`)
					messages.value = messages.value.filter(m => m.id !== msg.id)
					uni.showToast({ title: '已删除', icon: 'success' })
				} catch (e) {
					// ignore
				}
			}
		}
	})
}

/**
 * 查看详情
 */
function goDetail(msg) {
	if (!msg.isRead) {
		markRead(msg)
	}
	uni.showModal({
		title: msg.title,
		content: msg.content,
		showCancel: false
	})
}
</script>

<style lang="scss" scoped>
.page-messages {
	min-height: 100vh;
	background: #f5f5f5;
}

.header-bar {
	display: flex;
	justify-content: space-between;
	align-items: center;
	background: #fff;
	padding: 20rpx 30rpx;
	font-size: 26rpx;
	color: #007aff;
	border-bottom: 1rpx solid #f0f0f0;

	.read-all {
		color: #007aff;
	}
}

.message-list {
	.message-item {
		background: #fff;
		padding: 24rpx 30rpx;
		border-bottom: 1rpx solid #f0f0f0;
		position: relative;

		&.unread {
			background: #f8fbff;
		}

		.msg-header {
			display: flex;
			align-items: center;
			margin-bottom: 8rpx;
		}

		.msg-title {
			font-size: 30rpx;
			color: #333;
			font-weight: bold;
			flex: 1;
		}

		.unread-dot {
			width: 14rpx;
			height: 14rpx;
			border-radius: 50%;
			background: #ff3b30;
		}

		.msg-content {
			font-size: 26rpx;
			color: #666;
			display: -webkit-box;
			-webkit-box-orient: vertical;
			-webkit-line-clamp: 2;
			overflow: hidden;
			line-height: 1.5;
			margin: 8rpx 0;
		}

		.msg-time {
			font-size: 22rpx;
			color: #ccc;
		}

		.msg-actions {
			display: flex;
			justify-content: flex-end;
			margin-top: 8rpx;

			.action-item {
				font-size: 22rpx;
				color: #007aff;
				margin-left: 24rpx;

				&.danger {
					color: #ff3b30;
				}
			}
		}
	}

	.empty {
		text-align: center;
		padding: 100rpx 0;
		font-size: 28rpx;
		color: #999;
	}
}
</style>
