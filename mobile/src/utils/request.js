const BASE_URL = '/api'

function request(url, method = 'GET', data = null) {
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header: { 'Content-Type': 'application/json' },
      success(res) {
        if (res.statusCode === 200) {
          resolve(res.data)
        } else {
          reject(res.data)
        }
      },
      fail(err) {
        uni.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      }
    })
  })
}

export function post(url, data) {
  return request(url, 'POST', data)
}

export function get(url, data) {
  return request(url, 'GET', data)
}
