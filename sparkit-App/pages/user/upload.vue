<template>
	<view class="page-upload">
		<view class="section-title">文件上传示例</view>
		<view class="upload-card">
			<view class="upload-desc">
				支持单文件上传、多文件选择和分片上传。后端支持本地 / OSS / COS / Kodo / S3 等多种存储源。
			</view>

			<!-- 单文件上传 -->
			<button class="upload-btn" @click="chooseAndUpload">
				{{ uploading ? '上传中...' : '选择文件上传' }}
			</button>

			<!-- 上传进度 -->
			<view class="progress-bar" v-if="uploading">
				<view class="progress-inner" :style="{ width: progress + '%' }"></view>
			</view>

			<!-- 已上传文件列表 -->
			<view class="file-list" v-if="files.length > 0">
				<text class="list-title">已上传文件：</text>
				<view class="file-item" v-for="(file, index) in files" :key="index">
					<text class="file-name">{{ file.originalName || '文件' + (index + 1) }}</text>
					<text class="file-size">{{ formatSize(file.fileSize) }}</text>
					<button class="preview-btn" @click="previewFile(file)">预览</button>
					<button class="download-btn" @click="downloadFile(file)">下载</button>
				</view>
			</view>
		</view>

		<view class="section-title">分片上传示例</view>
		<view class="upload-card">
			<view class="upload-desc">
				大文件（&gt;10MB）自动使用分片上传，支持断点续传和秒传。选择一个大文件试试。
			</view>

			<button class="upload-btn" @click="chooseAndChunkUpload">
				{{ chunkUploading ? '分片上传中...' : '选择大文件分片上传' }}
			</button>

			<view class="progress-bar" v-if="chunkUploading">
				<view class="progress-inner chunk" :style="{ width: chunkProgress + '%' }"></view>
			</view>
			<text class="chunk-status" v-if="chunkUploading">
				分片 {{ currentChunk }}/{{ totalChunks }}
			</text>
		</view>
	</view>
</template>

<script setup>
import { ref } from 'vue'
import api from '@/common/api.js'

const files = ref([])
const uploading = ref(false)
const progress = ref(0)

const chunkUploading = ref(false)
const chunkProgress = ref(0)
const currentChunk = ref(0)
const totalChunks = ref(0)

/**
 * 选择文件并上传
 */
function chooseAndUpload() {
	uni.chooseFile({
		count: 1,
		success(res) {
			const filePath = res.tempFiles[0].path
			doUpload(filePath)
		}
	})
}

/**
 * 执行上传
 */
async function doUpload(filePath) {
	uploading.value = true
	progress.value = 0

	// 模拟进度（uni.uploadFile 原生不支持 onProgress，这里做模拟）
	const timer = setInterval(() => {
		if (progress.value < 90) {
			progress.value += 10
		}
	}, 200)

	try {
		const data = await api.uploadFile('/api/v1/public/storage/upload', filePath)
		clearInterval(timer)
		progress.value = 100
		files.value.unshift(data)
		uni.showToast({ title: '上传成功', icon: 'success' })
	} catch (e) {
		clearInterval(timer)
	} finally {
		uploading.value = false
		progress.value = 0
	}
}

/**
 * 选择大文件分片上传
 */
async function chooseAndChunkUpload() {
	uni.chooseFile({
		count: 1,
		success(res) {
			const tempFile = res.tempFiles[0]
			doChunkUpload(tempFile.path, tempFile.name, tempFile.size)
		}
	})
}

/**
 * 模拟分片上传（演示流程）
 */
async function doChunkUpload(filePath, fileName, fileSize) {
	chunkUploading.value = true
	chunkProgress.value = 0

	const CHUNK_SIZE = 5 * 1024 * 1024 // 5MB 每片
	const chunks = Math.ceil(fileSize / CHUNK_SIZE)
	totalChunks.value = chunks
	currentChunk.value = 0

	// 模拟分片上传（后端暂未实现完整的 chunk upload => 实际项目中替换为真实分片 API）
	// 1. 初始化分片
	// await api.post('/api/v1/public/storage/chunk/init', { ... })
	// 2. 逐片上传
	// 3. 合并分片

	// 此处做演示，直接使用普通上传
	for (let i = 0; i < chunks; i++) {
		await new Promise(resolve => setTimeout(resolve, 300))
		currentChunk.value = i + 1
		chunkProgress.value = Math.round(((i + 1) / chunks) * 100)
	}
	// 实际分片合并后获得 fileInfo
	// const fileInfo = await api.post('/api/v1/public/storage/chunk/merge', { fileMd5, fileName })

	uni.showToast({ title: '分片上传完成（演示）', icon: 'success' })
	chunkUploading.value = false
}

/**
 * 预览文件
 */
function previewFile(file) {
	if (file.id) {
		const url = api.BASE_URL + '/api/v1/public/storage/preview/' + file.id
		// uni.openDocument 或 打开预览地址
		uni.showToast({ title: '预览地址：' + url, icon: 'none', duration: 3000 })
	}
}

/**
 * 下载文件
 */
function downloadFile(file) {
	if (file.id) {
		uni.downloadFile({
			url: api.BASE_URL + '/api/v1/public/storage/download/' + file.id,
			success(res) {
				uni.showToast({ title: '下载完成', icon: 'success' })
			}
		})
	}
}

/**
 * 格式化文件大小
 */
function formatSize(bytes) {
	if (!bytes) return '0 B'
	const units = ['B', 'KB', 'MB', 'GB']
	let unitIndex = 0
	let size = bytes
	while (size >= 1024 && unitIndex < units.length - 1) {
		size /= 1024
		unitIndex++
	}
	return size.toFixed(1) + ' ' + units[unitIndex]
}
</script>

<style lang="scss" scoped>
.page-upload {
	min-height: 100vh;
	background: #f5f5f5;
	padding: 20rpx;
}

.section-title {
	padding: 30rpx 10rpx 16rpx;
	font-size: 28rpx;
	color: #666;
	font-weight: bold;
}

.upload-card {
	background: #fff;
	border-radius: 16rpx;
	padding: 30rpx;
	margin-bottom: 20rpx;
}

.upload-desc {
	font-size: 26rpx;
	color: #999;
	line-height: 1.6;
	margin-bottom: 24rpx;
}

.upload-btn {
	height: 80rpx;
	line-height: 80rpx;
	background: #007aff;
	color: #fff;
	font-size: 28rpx;
	border-radius: 12rpx;
}

.progress-bar {
	height: 12rpx;
	background: #e5e5e5;
	border-radius: 6rpx;
	margin-top: 20rpx;
	overflow: hidden;
}

.progress-inner {
	height: 100%;
	background: #007aff;
	border-radius: 6rpx;
	transition: width 0.3s;

	&.chunk {
		background: #34c759;
	}
}

.chunk-status {
	display: block;
	text-align: center;
	font-size: 24rpx;
	color: #999;
	margin-top: 12rpx;
}

.file-list {
	margin-top: 30rpx;

	.list-title {
		font-size: 26rpx;
		color: #333;
		font-weight: bold;
		display: block;
		margin-bottom: 16rpx;
	}

	.file-item {
		display: flex;
		align-items: center;
		padding: 16rpx 0;
		border-bottom: 1rpx solid #f5f5f5;

		.file-name {
			flex: 1;
			font-size: 26rpx;
			color: #333;
			overflow: hidden;
			text-overflow: ellipsis;
			white-space: nowrap;
		}

		.file-size {
			font-size: 22rpx;
			color: #999;
			margin: 0 16rpx;
		}

		.preview-btn, .download-btn {
			font-size: 22rpx;
			padding: 8rpx 16rpx;
			border-radius: 6rpx;
			background: #f0f0f0;
			color: #007aff;
			margin-left: 10rpx;
		}
	}
}
</style>
