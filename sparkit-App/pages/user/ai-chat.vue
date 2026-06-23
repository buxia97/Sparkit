<template>
	<view class="page-ai-chat">
		<!-- 模型选择 -->
		<view class="model-selector">
			<text class="label">模型：</text>
			<picker
				mode="selector"
				:range="modelOptions"
				range-key="name"
				:value="modelIndex"
				@change="onModelChange"
			>
				<view class="picker-value">
					{{ modelOptions[modelIndex]?.name || '选择模型' }}
					<text class="arrow">▼</text>
				</view>
			</picker>
		</view>

		<!-- 对话区域 -->
		<scroll-view class="chat-area" scroll-y :scroll-into-view="scrollToId" @scrolltoupper="loadMore">
			<view class="chat-list">
				<view
					v-for="(msg, index) in chatList"
					:key="index"
					:id="'msg-' + index"
					:class="['chat-item', msg.role === 'user' ? 'user' : 'ai']"
				>
					<view class="bubble">
						<text v-if="msg.role === 'ai' && msg.loading" class="typing">思考中...</text>
						<text v-else>{{ msg.content }}</text>
					</view>
				</view>
			</view>
		</scroll-view>

		<!-- 输入栏 -->
		<view class="input-bar">
			<input
				v-model="inputText"
				placeholder="输入内容，向 AI 提问..."
				confirm-type="send"
				@confirm="sendMessage"
			/>
			<button class="send-btn" :disabled="!inputText.trim() || sending" @click="sendMessage">
				{{ sending ? '发送中' : '发送' }}
			</button>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import api from '@/common/api.js'

const inputText = ref('')
const sending = ref(false)
const scrollToId = ref('')
const sessionId = ref('')

const modelOptions = ref([])
const modelIndex = ref(0)

const chatList = ref([])

// 初始化获取可用模型列表
async function fetchModels() {
	try {
		const data = await api.get('/api/v1/public/ai/models/available')
		if (Array.isArray(data)) {
			modelOptions.value = data
		} else {
			// 默认模型
			modelOptions.value = [
				{ id: 1, name: 'DeepSeek', provider: 'deepseek' },
				{ id: 2, name: '阿里百炼', provider: 'bailian' },
				{ id: 3, name: '小米大模型', provider: 'xiaomi' }
			]
		}
	} catch (e) {
		modelOptions.value = [
			{ id: 1, name: 'DeepSeek', provider: 'deepseek' },
			{ id: 2, name: '阿里百炼', provider: 'bailian' },
			{ id: 3, name: '小米大模型', provider: 'xiaomi' }
		]
	}
}

fetchModels()

// 创建对话 session
async function ensureSession() {
	if (sessionId.value) return
	try {
		const data = await api.post('/api/v1/public/ai/sessions', {
			modelId: modelOptions.value[modelIndex.value]?.id,
			title: 'App 端对话'
		})
		sessionId.value = data?.id || ''
	} catch (e) {
		// ignore — sessionId 为空，使用无会话模式
	}
}

/**
 * 发送消息
 */
async function sendMessage() {
	const text = inputText.value.trim()
	if (!text || sending.value) return

	sending.value = true

	// 添加用户消息
	chatList.value.push({ role: 'user', content: text })
	inputText.value = ''

	// 添加 AI 占位消息
	chatList.value.push({ role: 'ai', content: '', loading: true })
	scrollToBottom()

	try {
		await ensureSession()

		const data = await api.post('/api/v1/public/ai/chat', {
			modelId: modelOptions.value[modelIndex.value]?.id,
			sessionId: sessionId.value || undefined,
			message: text
		})

		// 填充 AI 回复
		const lastMsg = chatList.value[chatList.value.length - 1]
		lastMsg.content = data?.reply || data?.content || '（AI 无返回内容）'
		lastMsg.loading = false
	} catch (e) {
		const lastMsg = chatList.value[chatList.value.length - 1]
		lastMsg.content = '请求失败，请稍后重试'
		lastMsg.loading = false
	} finally {
		sending.value = false
		scrollToBottom()
	}
}

/**
 * 模型切换
 */
function onModelChange(e) {
	modelIndex.value = e.detail.value
	sessionId.value = '' // 切换模型后重置会话
}

/**
 * 滚动到底部
 */
function scrollToBottom() {
	const index = chatList.value.length - 1
	scrollToId.value = ''
	setTimeout(() => {
		scrollToId.value = 'msg-' + index
	}, 50)
}

/**
 * 加载更多（上拉）
 */
function loadMore() {
	// 此处可扩展加载历史记录
}
</script>

<style lang="scss" scoped>
.page-ai-chat {
	display: flex;
	flex-direction: column;
	height: 100vh;
	background: #f5f5f5;
}

.model-selector {
	display: flex;
	align-items: center;
	background: #fff;
	padding: 16rpx 30rpx;
	border-bottom: 1rpx solid #eee;

	.label {
		font-size: 26rpx;
		color: #666;
		margin-right: 12rpx;
	}

	.picker-value {
		font-size: 26rpx;
		color: #007aff;
		display: flex;
		align-items: center;

		.arrow {
			font-size: 18rpx;
			margin-left: 8rpx;
		}
	}
}

.chat-area {
	flex: 1;
	padding: 20rpx;
}

.chat-list {
	.chat-item {
		display: flex;
		margin-bottom: 30rpx;

		.bubble {
			max-width: 80%;
			padding: 20rpx 24rpx;
			border-radius: 16rpx;
			font-size: 28rpx;
			line-height: 1.6;
			word-break: break-all;
		}

		.typing {
			color: #999;
			animation: blink 1s infinite;
		}

		&.user {
			justify-content: flex-end;

			.bubble {
				background: #007aff;
				color: #fff;
				border-bottom-right-radius: 4rpx;
			}
		}

		&.ai {
			justify-content: flex-start;

			.bubble {
				background: #fff;
				color: #333;
				border-bottom-left-radius: 4rpx;
			}
		}
	}
}

@keyframes blink {
	0%, 100% { opacity: 1; }
	50% { opacity: 0.3; }
}

.input-bar {
	display: flex;
	align-items: center;
	background: #fff;
	padding: 16rpx 20rpx;
	border-top: 1rpx solid #eee;

	input {
		flex: 1;
		height: 72rpx;
		background: #f5f5f5;
		border-radius: 36rpx;
		padding: 0 24rpx;
		font-size: 28rpx;
	}

	.send-btn {
		flex-shrink: 0;
		margin-left: 16rpx;
		padding: 0 28rpx;
		height: 64rpx;
		line-height: 64rpx;
		background: #007aff;
		color: #fff;
		font-size: 26rpx;
		border-radius: 8rpx;

		&[disabled] {
			background: #cccccc;
			color: #fff;
		}
	}
}
</style>
