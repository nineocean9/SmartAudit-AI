import CryptoJS from 'crypto-js'
import JSEncrypt from 'jsencrypt/bin/jsencrypt.min'

let publicKeyPromise

function getPublicKey() {
  if (!publicKeyPromise) {
    const baseURL = import.meta.env.VITE_APP_BASE_API || ''
    publicKeyPromise = fetch(`${baseURL}/crypto/public-key`)
      .then(response => {
        if (!response.ok) throw new Error('获取接口加密公钥失败')
        return response.json()
      })
      .then(result => result.data)
      .catch(error => {
        publicKeyPromise = undefined
        throw error
      })
  }
  return publicKeyPromise
}

function deriveKey(sessionKey, purpose) {
  return CryptoJS.SHA256(sessionKey.clone().concat(CryptoJS.enc.Utf8.parse(purpose)))
}

function sign(iv, cipherText, sessionKey) {
  return CryptoJS.HmacSHA256(`${iv}.${cipherText}`, deriveKey(sessionKey, 'mac'))
    .toString(CryptoJS.enc.Base64)
}

export function shouldEncrypt(config) {
  return false // 加密功能已禁用
}

export async function encryptConfig(config) {
  const publicKey = await getPublicKey()
  const sessionKey = CryptoJS.lib.WordArray.random(32)
  const ivWords = CryptoJS.lib.WordArray.random(16)
  const iv = ivWords.toString(CryptoJS.enc.Base64)
  const method = String(config.method || 'get').toLowerCase()
  const plainData = method === 'get' || method === 'delete' ? (config.params || {}) : (config.data || {})
  const cipherText = CryptoJS.AES.encrypt(JSON.stringify(plainData), deriveKey(sessionKey, 'enc'), {
    iv: ivWords,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  }).ciphertext.toString(CryptoJS.enc.Base64)

  const rsa = new JSEncrypt()
  rsa.setPublicKey(publicKey)
  const encryptedKey = rsa.encrypt(sessionKey.toString(CryptoJS.enc.Base64))
  if (!encryptedKey) throw new Error('接口会话密钥加密失败')

  config.headers['X-Encrypted-Key'] = encryptedKey
  config.headers['X-Encrypted-IV'] = iv
  config.headers['X-Encrypted-Signature'] = sign(iv, cipherText, sessionKey)
  config._cryptoSessionKey = sessionKey.toString(CryptoJS.enc.Base64)
  if (method === 'get' || method === 'delete') {
    config.params = { __enc: cipherText }
  } else {
    config.data = { data: cipherText }
  }
  return config
}

export function decryptResponse(payload, sessionKeyBase64) {
  if (!payload?.encrypted || !sessionKeyBase64) return payload
  const sessionKey = CryptoJS.enc.Base64.parse(sessionKeyBase64)
  if (sign(payload.iv, payload.data, sessionKey) !== payload.signature) {
    throw new Error('接口响应签名校验失败')
  }
  const decrypted = CryptoJS.AES.decrypt(
    { ciphertext: CryptoJS.enc.Base64.parse(payload.data) },
    deriveKey(sessionKey, 'enc'),
    {
      iv: CryptoJS.enc.Base64.parse(payload.iv),
      mode: CryptoJS.mode.CBC,
      padding: CryptoJS.pad.Pkcs7
    }
  )
  return JSON.parse(decrypted.toString(CryptoJS.enc.Utf8))
}
