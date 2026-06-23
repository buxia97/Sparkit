<template>
	<view class="page-payment">
		<view class="balance-card">
			<text class="balance-label">账户余额</text>
			<text class="balance-amount">¥ {{ balance }}</text>
			<view class="balance-actions">
				<button class="action-btn" @click="showRecharge = true">充值</button>
			</view>
		</view>

		<!-- 快捷充值金额 -->
		<view class="section-title">快捷充值</view>
		<view class="amount-grid">
			<view
				v-for="item in amountList"
				:key="item"
				:class="['amount-item', rechargeAmount === item ? 'active' : '']"
				@click="rechargeAmount = item"
			>
				¥ {{ item }}
			</view>
		</view>

		<!-- 支付渠道 -->
		<view class="section-title">选择支付方式</view>
		<view class="channel-list">
			<view
				v-for="ch in channels"
				:key="ch.code"
				:class="['channel-item', selectedChannel === ch.code ? 'active' : '']"
				@click="selectedChannel = ch.code"
			>
				<text class="channel-icon">{{ ch.icon }}</text>
				<text class="channel-name">{{ ch.name }}</text>
				<view class="channel-check" v-if="selectedChannel === ch.code">✓</view>
			</view>
		</view>

		<!-- 自定义金额 -->
		<view class="custom-amount">
			<text class="prefix">¥</text>
			<input
				v-model="customAmount"
				type="digit"
				placeholder="自定义金额"
				@focus="rechargeAmount = 0"
			/>
		</view>

		<button class="pay-btn" @click="handlePay">
			{{ paying ? '支付中...' : '立即充值 ¥' + actualAmount }}
		</button>

		<!-- 充值记录 -->
		<view class="section-title">充值记录</view>
		<view class="order-list">
			<view class="order-item" v-for="order in orders" :key="order.id">
				<view class="order-info">
					<text class="order-no">{{ order.orderNo || order.id }}</text>
					<text :class="['order-status', statusClass(order.status)]">
						{{ statusText(order.status) }}
					</text>
				</view>
				<view class="order-meta">
					<text>¥ {{ order.amount }}</text>
					<text>{{ order.createTime }}</text>
				</view>
			</view>
			<view class="empty" v-if="orders.length === 0">
				<text>暂无充值记录</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import api from '@/common/api.js'

const balance = ref('0.00')
const rechargeAmount = ref(50)
const customAmount = ref('')
const selectedChannel = ref('wechat_native')
const paying = ref(false)
const showRecharge = ref(false)
const orders = ref([])

const amountList = [10, 30, 50, 100, 200, 500]

const channels = ref([
	{ code: 'wechat_native', name: '微信支付', icon: 'W' },
	{ code: 'alipay_native', name: '支付宝', icon: 'A' },
	{ code: 'apple_pay', name: 'Apple Pay', icon: 'AP' },
	{ code: 'google_pay', name: 'Google Pay', icon: 'GP' }
])

const actualAmount = computed(() => {
	if (customAmount.value) return parseFloat(customAmount.value).toFixed(2)
	return rechargeAmount.value.toFixed(2)
})

/**
 * 发起支付
 */
async function handlePay() {
	const amount = parseFloat(actualAmount.value)
	if (!amount || amount <= 0) {
		uni.showToast({ title: '请输入有效金额', icon: 'none' })
		return
	}

	paying.value = true
	try {
		const data = await api.post('/api/v1/public/payment/virtual-pay', {
			amount: amount,
			channelCode: selectedChannel.value,
			title: 'Sparkit 账户充值',
			description: '充值 ¥' + actualAmount.value
		})

		uni.showToast({ title: '充值成功', icon: 'success' })
		balance.value = (parseFloat(balance.value) + amount).toFixed(2)
		// 刷新充值记录
		fetchOrders()
	} catch (e) {
		// 错误已在 api 层提示
	} finally {
		paying.value = false
	}
}

/**
 * 获取充值记录
 */
async function fetchOrders() {
	try {
		const data = await api.get('/api/v1/public/payment/orders', {
			page: 1,
			pageSize: 10
		})
		orders.value = data?.records || []
	} catch (e) {
		// ignore
	}
}

/**
 * 订单状态文本
 */
function statusText(status) {
	const map = { 0: '待支付', 1: '已支付', 2: '已取消', 3: '已退款' }
	return map[status] || '未知'
}

function statusClass(status) {
	const map = { 0: 'pending', 1: 'success', 2: 'cancel', 3: 'refund' }
	return map[status] || ''
}
</script>

<style lang="scss" scoped>
.page-payment {
	min-height: 100vh;
	background: #f5f5f5;
	padding: 20rpx;
}

.balance-card {
	background: linear-gradient(135deg, #ff6b35, #ff9500);
	border-radius: 16rpx;
	padding: 40rpx;
	margin-bottom: 30rpx;

	.balance-label {
		font-size: 26rpx;
		color: rgba(255, 255, 255, 0.8);
	}

	.balance-amount {
		font-size: 60rpx;
		font-weight: bold;
		color: #fff;
		margin: 16rpx 0 24rpx;
	}

	.balance-actions {
		.action-btn {
			display: inline-block;
			background: rgba(255, 255, 255, 0.2);
			color: #fff;
			font-size: 26rpx;
			padding: 12rpx 40rpx;
			border-radius: 40rpx;
		}
	}
}

.section-title {
	padding: 20rpx 10rpx 16rpx;
	font-size: 28rpx;
	color: #666;
	font-weight: bold;
}

.amount-grid {
	display: flex;
	flex-wrap: wrap;
	gap: 16rpx;
	margin-bottom: 20rpx;

	.amount-item {
		flex: 1;
		min-width: 150rpx;
		text-align: center;
		padding: 24rpx;
		background: #fff;
		border-radius: 12rpx;
		font-size: 30rpx;
		color: #333;
		border: 2rpx solid transparent;

		&.active {
			border-color: #ff6b35;
			color: #ff6b35;
			background: #fff5f0;
		}
	}
}

.channel-list {
	background: #fff;
	border-radius: 16rpx;
	margin-bottom: 20rpx;

	.channel-item {
		display: flex;
		align-items: center;
		padding: 28rpx 30rpx;
		border-bottom: 1rpx solid #f0f0f0;

		&:last-child {
			border-bottom: none;
		}

		.channel-icon {
			width: 56rpx;
			height: 56rpx;
			line-height: 56rpx;
			text-align: center;
			background: #007aff;
			color: #fff;
			border-radius: 12rpx;
			font-size: 24rpx;
			font-weight: bold;
			margin-right: 20rpx;
		}

		.channel-name {
			flex: 1;
			font-size: 28rpx;
			color: #333;
		}

		.channel-check {
			width: 36rpx;
			height: 36rpx;
			line-height: 36rpx;
			text-align: center;
			background: #ff6b35;
			color: #fff;
			border-radius: 50%;
			font-size: 22rpx;
		}
	}
}

.custom-amount {
	display: flex;
	align-items: center;
	background: #fff;
	border-radius: 12rpx;
	padding: 0 30rpx;
	margin-bottom: 30rpx;

	.prefix {
		font-size: 40rpx;
		font-weight: bold;
		color: #333;
		margin-right: 16rpx;
	}

	input {
		flex: 1;
		height: 88rpx;
		font-size: 40rpx;
		font-weight: bold;
	}
}

.pay-btn {
	margin: 20rpx 0 40rpx;
	height: 88rpx;
	line-height: 88rpx;
	background: linear-gradient(135deg, #ff6b35, #ff9500);
	color: #fff;
	font-size: 32rpx;
	border-radius: 12rpx;
}

.order-list {
	background: #fff;
	border-radius: 16rpx;
	padding: 20rpx 30rpx;

	.order-item {
		padding: 20rpx 0;
		border-bottom: 1rpx solid #f5f5f5;

		&:last-child {
			border-bottom: none;
		}

		.order-info {
			display: flex;
			justify-content: space-between;
			align-items: center;
			margin-bottom: 8rpx;

			.order-no {
				font-size: 26rpx;
				color: #333;
			}

			.order-status {
				font-size: 24rpx;
				padding: 4rpx 12rpx;
				border-radius: 6rpx;

				&.success {
					color: #34c759;
					background: #e8f8ed;
				}

				&.pending {
					color: #ff9500;
					background: #fff5e6;
				}

				&.cancel {
					color: #999;
					background: #f5f5f5;
				}

				&.refund {
					color: #007aff;
					background: #e8f0fe;
				}
			}
		}

		.order-meta {
			display: flex;
			justify-content: space-between;
			font-size: 24rpx;
			color: #999;
		}
	}

	.empty {
		text-align: center;
		padding: 40rpx;
		font-size: 28rpx;
		color: #999;
	}
}
</style>
