/**
 * Sparkit App API 请求封装
 * 基于 uni.request，统一处理 Token、错误码、响应格式
 */

// 后端 API 基础地址
const BASE_URL = 'http://localhost:8083'

// 请求缓存，用于去重
const pendingRequests = new Map()

/**
 * 生成请求唯一标识
 */
function getRequestKey(config) {
	return `${config.method}_${config.url}_${JSON.stringify(config.data)}`
}

/**
 * 发起 HTTP 请求
 * @param {Object} config - 请求配置
 * @param {string} config.url - 请求路径（相对路径）
 * @param {string} config.method - 请求方法 GET/POST/PUT/DELETE
 * @param {Object} config.data - 请求体数据
 * @param {Object} config.header - 自定义请求头
 * @param {boolean} config.showLoading - 是否显示 loading
 * @param {number} config.timeout - 超时时间（ms）
 * @returns {Promise}
 */
function request(config = {}) {
	const {
		url,
		method = 'GET',
		data,
		header = {},
		showLoading = false,
		timeout = 15000
	} = config

	// 默认请求头
	const headers = {
		'Content-Type': 'application/json',
		...header
	}

	// 自动注入 Token
	try {
		const token = uni.getStorageSync('accessToken')
		if (token) {
			headers['Authorization'] = `Bearer ${token}`
		}
	} catch (e) {
		// ignore
	}

	if (showLoading) {
		uni.showLoading({ title: '加载中...', mask: true })
	}

	return new Promise((resolve, reject) => {
		uni.request({
			url: BASE_URL + url,
			method,
			data,
			header: headers,
			timeout,
			success(res) {
				if (showLoading) uni.hideLoading()

				const { statusCode, data: resData } = res

				// HTTP 状态码处理
				if (statusCode === 401) {
					// Token 过期，清除登录状态
					try {
						uni.removeStorageSync('accessToken')
						uni.removeStorageSync('refreshToken')
						uni.removeStorageSync('userInfo')
					} catch (e) { /* ignore */ }
					uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
					reject(new Error('登录已过期'))
					return
				}

				if (statusCode === 403) {
					uni.showToast({ title: '无权限访问', icon: 'none' })
					reject(new Error('无权限'))
					return
				}

				if (statusCode === 404) {
					uni.showToast({ title: '请求的资源不存在', icon: 'none' })
					reject(new Error('资源不存在'))
					return
				}

				if (statusCode >= 500) {
					uni.showToast({ title: '服务器错误，请稍后重试', icon: 'none' })
					reject(new Error('服务器错误'))
					return
				}

				// 业务状态码处理
				// 格式：{ code: 200, msg: "操作成功", data: {...} }
				if (resData && typeof resData.code !== 'undefined') {
					if (resData.code === 200) {
						resolve(resData.data)
					} else {
						const errMsg = resData.msg || '请求失败'
						uni.showToast({ title: errMsg, icon: 'none' })
						reject(new Error(errMsg))
					}
				} else {
					// 非标准格式，直接透传
					resolve(resData)
				}
			},
			fail(err) {
				if (showLoading) uni.hideLoading()
				uni.showToast({ title: '网络请求失败，请检查网络', icon: 'none' })
				reject(err)
			}
		})
	})
}

/**
 * GET 请求
 */
function get(url, params = {}, config = {}) {
	// 将 params 拼接为 query string
	let fullUrl = url
	if (params && Object.keys(params).length > 0) {
		const query = Object.keys(params)
			.filter(key => params[key] !== undefined && params[key] !== null && params[key] !== '')
			.map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
			.join('&')
		if (query) {
			fullUrl += '?' + query
		}
	}
	return request({ ...config, url: fullUrl, method: 'GET' })
}

/**
 * POST 请求
 */
function post(url, data = {}, config = {}) {
	return request({ ...config, url, method: 'POST', data })
}

/**
 * PUT 请求
 */
function put(url, data = {}, config = {}) {
	return request({ ...config, url, method: 'PUT', data })
}

/**
 * DELETE 请求
 */
function del(url, config = {}) {
	return request({ ...config, url, method: 'DELETE' })
}

/**
 * 文件上传
 * @param {string} url - 上传地址
 * @param {string} filePath - 本地文件路径
 * @param {string} name - 文件字段名，默认 file
 * @param {Object} formData - 额外表单数据
 * @returns {Promise}
 */
function uploadFile(url, filePath, name = 'file', formData = {}) {
	return new Promise((resolve, reject) => {
		// 自动注入 Token
		let token = ''
		try {
			token = uni.getStorageSync('accessToken') || ''
		} catch (e) { /* ignore */ }

		uni.uploadFile({
			url: BASE_URL + url,
			filePath,
			name,
			formData,
			header: {
				'Authorization': token ? `Bearer ${token}` : ''
			},
			success(res) {
				try {
					const resData = JSON.parse(res.data)
					if (resData.code === 200) {
						resolve(resData.data)
					} else {
						uni.showToast({ title: resData.msg || '上传失败', icon: 'none' })
						reject(new Error(resData.msg || '上传失败'))
					}
				} catch (e) {
					reject(new Error('解析响应失败'))
				}
			},
			fail(err) {
				uni.showToast({ title: '上传失败，请检查网络', icon: 'none' })
				reject(err)
			}
		})
	})
}

export default {
	request,
	get,
	post,
	put,
	del,
	uploadFile,
	BASE_URL
}
