<template>
  <div class="login-page">
    <div class="login-shell">
      <section class="brand-panel">
        <div class="brand-top">
          <div class="brand-mark">
            <span></span>
          </div>
          <div>
            <div class="brand-system">高校一体化智慧审计平台</div>
            <div class="brand-subtitle">Audit Intelligence Platform</div>
          </div>
        </div>

        <div class="brand-copy">
          <p class="eyebrow">统一入口</p>
          <h1>覆盖计划、实施、整改、归档的审计业务中枢</h1>
          <p class="summary">面向校领导、审计处、项目组、被审计单位和中介机构，提供角色化工作台、过程留痕和智能分析支撑。</p>
        </div>

        <div class="capability-grid">
          <div class="capability-item">
            <strong>项目全过程</strong>
            <span>计划立项、实施进度、报告归档</span>
          </div>
          <div class="capability-item">
            <strong>权限分级</strong>
            <span>校级总览、处室审批、项目协同</span>
          </div>
          <div class="capability-item">
            <strong>智能支撑</strong>
            <span>依据检索、风险分析、资料问答</span>
          </div>
          <div class="capability-item">
            <strong>安全留痕</strong>
            <span>操作记录、归档审核、整改跟踪</span>
          </div>
        </div>
      </section>

      <section class="signin-panel">
        <div class="signin-card">
          <div class="signin-header">
            <p>身份认证</p>
            <h2>{{ title }}</h2>
            <span>请使用平台授权账号登录</span>
          </div>

          <div class="demo-switch">
            <div class="demo-switch__title">
              <strong>测试账号切换</strong>
              <span>选择后自动填入测试账号和密码</span>
            </div>
            <el-select
              v-model="selectedDemoUser"
              size="large"
              class="demo-select"
              placeholder="选择测试账号"
              @change="applyDemoAccount"
            >
              <el-option
                v-for="account in demoAccounts"
                :key="account.username"
                :label="account.label"
                :value="account.username"
              >
                <div class="demo-option">
                  <strong>{{ account.role }}</strong>
                  <span>{{ account.username }}</span>
                </div>
              </el-option>
            </el-select>
          </div>

          <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                type="text"
                size="large"
                auto-complete="off"
                placeholder="账号"
              >
                <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
              </el-input>
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                size="large"
                auto-complete="off"
                placeholder="密码"
                show-password
                @keyup.enter="handleLogin"
              >
                <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
              </el-input>
            </el-form-item>

            <el-form-item prop="code" v-if="captchaEnabled" class="code-row">
              <el-input
                v-model="loginForm.code"
                size="large"
                auto-complete="off"
                placeholder="验证码"
                @keyup.enter="handleLogin"
              >
                <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
              </el-input>
              <button type="button" class="login-code" title="点击刷新验证码" @click="getCode">
                <img :src="codeUrl" class="login-code-img" />
              </button>
            </el-form-item>

            <div class="form-options">
              <el-checkbox v-model="loginForm.rememberMe">记住密码</el-checkbox>
              <router-link v-if="register" class="register-link" :to="'/register'">立即注册</router-link>
            </div>

            <el-button
              :loading="loading"
              size="large"
              type="primary"
              class="login-button"
              @click.prevent="handleLogin"
            >
              <span v-if="!loading">登录平台</span>
              <span v-else>正在登录...</span>
            </el-button>

            <div class="login-actions">
              <router-link class="create-account-link" :to="'/register'">注册新账号</router-link>
            </div>
          </el-form>

          <div class="signin-note">
            <span>建议使用校内网络或授权 VPN 访问</span>
          </div>
        </div>
      </section>
    </div>

    <div class="el-login-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script setup>
import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from "@/utils/jsencrypt"
import useUserStore from '@/store/modules/user'
import defaultSettings from '@/settings'

const title = import.meta.env.VITE_APP_TITLE
const footerContent = defaultSettings.footerContent
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const loginForm = ref({
  username: "admin",
  password: "admin123",
  rememberMe: false,
  code: "",
  uuid: ""
})

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

const codeUrl = ref("")
const demoCaptchaCode = ref("")
const loading = ref(false)
const captchaEnabled = ref(true)
const register = ref(true)
const redirect = ref(undefined)
const selectedDemoUser = ref("admin")

const demoAccounts = [
  { role: '系统管理员（平台管理员）', label: '系统管理员（平台管理员） admin', username: 'admin', password: 'admin123' },
  { role: '张三（某高校 校领导）', label: '张三（某高校 校领导） school_leader', username: 'school_leader', password: 'admin123' },
  { role: '王明（某高校 审计处长）', label: '王明（某高校 审计处长） audit_director', username: 'audit_director', password: 'admin123' },
  { role: '刘强（某高校 项目组长/主审）', label: '刘强（某高校 项目组长/主审） audit_project_leader', username: 'audit_project_leader', password: 'admin123' },
  { role: '李娜（某高校 普通审计人员）', label: '李娜（某高校 普通审计人员） audit_staff', username: 'audit_staff', password: 'admin123' },
  { role: '赵敏（信息工程学院 被审计单位负责人）', label: '赵敏（信息工程学院 被审计单位负责人） audited_unit_principal', username: 'audited_unit_principal', password: 'admin123' },
  { role: '陈晨（信息工程学院 被审计单位联络员）', label: '陈晨（信息工程学院 被审计单位联络员） audited_unit_liaison', username: 'audited_unit_liaison', password: 'admin123' },
  { role: '孙丽（A公司 被审计单位负责人）', label: '孙丽（A公司 被审计单位负责人） a_company_principal', username: 'a_company_principal', password: 'admin123' },
  { role: '吴磊（A公司 被审计单位联络员）', label: '吴磊（A公司 被审计单位联络员） a_company_liaison', username: 'a_company_liaison', password: 'admin123' },
  { role: '钱莹（商学院 被审计单位联络员）', label: '钱莹（商学院 被审计单位联络员） business_liaison', username: 'business_liaison', password: 'admin123' },
  { role: '郑凯（后勤处 被审计单位联络员）', label: '郑凯（后勤处 被审计单位联络员） logistics_liaison', username: 'logistics_liaison', password: 'admin123' },
  { role: '胡敏（财务处 被审计单位联络员）', label: '胡敏（财务处 被审计单位联络员） finance_liaison', username: 'finance_liaison', password: 'admin123' },
  { role: '高远（图书馆 被审计单位联络员）', label: '高远（图书馆 被审计单位联络员） library_liaison', username: 'library_liaison', password: 'admin123' },
  { role: '周航（外部中介机构 中介审计人员）', label: '周航（外部中介机构 中介审计人员） intermediary_auditor', username: 'intermediary_auditor', password: 'admin123' }
]

watch(route, (newRoute) => {
  redirect.value = newRoute.query && newRoute.query.redirect
}, { immediate: true })

function handleLogin() {
  loginForm.value.username = (loginForm.value.username || "").trim()
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      loading.value = true
      if (loginForm.value.rememberMe) {
        Cookies.set("username", loginForm.value.username, { expires: 30 })
        Cookies.set("password", encrypt(loginForm.value.password), { expires: 30 })
        Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 })
      } else {
        Cookies.remove("username")
        Cookies.remove("password")
        Cookies.remove("rememberMe")
      }
      userStore.login(loginForm.value).then(() => {
        const query = route.query
        const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
          if (cur !== "redirect") {
            acc[cur] = query[cur]
          }
          return acc
        }, {})
        router.push({ path: redirect.value || "/", query: otherQueryParams })
      }).catch(() => {
        loading.value = false
        if (captchaEnabled.value) {
          getCode()
        }
      })
    }
  })
}

function getCode() {
  return getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img
      loginForm.value.uuid = res.uuid
      demoCaptchaCode.value = ""
      loginForm.value.code = ""
    } else {
      demoCaptchaCode.value = ""
      loginForm.value.code = ""
    }
  })
}

function getCookie() {
  const username = Cookies.get("username")
  const password = Cookies.get("password")
  const rememberMe = Cookies.get("rememberMe")
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe),
    code: loginForm.value.code,
    uuid: loginForm.value.uuid
  }
  const matched = demoAccounts.find(account => account.username === loginForm.value.username)
  selectedDemoUser.value = matched ? matched.username : ""
}

function applyDemoAccount(username) {
  const account = demoAccounts.find(item => item.username === username)
  if (!account) return
  loginForm.value.username = account.username
  loginForm.value.password = account.password
  loginForm.value.rememberMe = false
  loginForm.value.code = ""
  if (captchaEnabled.value) {
    getCode()
  }
}

getCode()
getCookie()
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 32px 64px;
  background:
    linear-gradient(135deg, rgba(9, 63, 70, 0.92), rgba(16, 81, 89, 0.84)),
    url("../assets/images/login-background.jpg") center/cover no-repeat;
  color: #17242a;
  position: relative;
}

.login-shell {
  width: min(1120px, 100%);
  min-height: 640px;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) 430px;
  background: rgba(248, 251, 250, 0.96);
  border: 1px solid rgba(255, 255, 255, 0.48);
  box-shadow: 0 28px 70px rgba(3, 24, 30, 0.28);
  overflow: hidden;
}

.brand-panel {
  padding: 48px;
  background:
    linear-gradient(180deg, rgba(248, 251, 250, 0.98), rgba(236, 246, 244, 0.95));
  border-right: 1px solid #d8e5e3;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.brand-top {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  background: #0f6d66;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.24);

  span {
    width: 22px;
    height: 22px;
    border: 3px solid #ffffff;
    border-top-color: transparent;
    transform: rotate(45deg);
  }
}

.brand-system {
  font-size: 20px;
  font-weight: 700;
  color: #123438;
}

.brand-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #60747a;
  text-transform: uppercase;
}

.brand-copy {
  max-width: 620px;
}

.eyebrow {
  margin: 0 0 16px;
  color: #0f6d66;
  font-size: 14px;
  font-weight: 700;
}

.brand-copy h1 {
  margin: 0;
  color: #102b31;
  font-size: 42px;
  line-height: 1.18;
  font-weight: 800;
  letter-spacing: 0;
}

.summary {
  margin: 22px 0 0;
  max-width: 560px;
  color: #52676d;
  font-size: 16px;
  line-height: 1.85;
}

.capability-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.capability-item {
  padding: 18px;
  background: #ffffff;
  border: 1px solid #dbe7e5;
  border-radius: 4px;

  strong {
    display: block;
    margin-bottom: 8px;
    color: #16363b;
    font-size: 15px;
  }

  span {
    color: #64777c;
    font-size: 13px;
    line-height: 1.6;
  }
}

.signin-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 42px;
  background: #f7faf9;
}

.signin-card {
  width: 100%;
}

.signin-header {
  margin-bottom: 28px;

  p {
    margin: 0 0 10px;
    color: #0f6d66;
    font-size: 14px;
    font-weight: 700;
  }

  h2 {
    margin: 0;
    color: #142f35;
    font-size: 28px;
    line-height: 1.25;
    font-weight: 800;
    letter-spacing: 0;
  }

  span {
    display: block;
    margin-top: 10px;
    color: #6a7c81;
    font-size: 14px;
  }
}

.demo-switch {
  margin-bottom: 18px;
  padding: 14px;
  border: 1px solid #dbe7e5;
  border-radius: 4px;
  background: #ffffff;
}

.demo-switch__title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;

  strong {
    color: #16363b;
    font-size: 14px;
  }

  span {
    color: #74858a;
    font-size: 12px;
    line-height: 1.5;
    text-align: right;
  }
}

.demo-select {
  width: 100%;
}

.demo-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;

  strong {
    color: #16363b;
    font-size: 14px;
    font-weight: 600;
  }

  span {
    color: #7b8b90;
    font-size: 12px;
  }
}

.login-form {
  :deep(.el-form-item) {
    margin-bottom: 18px;
  }

  :deep(.el-input__wrapper) {
    min-height: 46px;
    border-radius: 4px;
    box-shadow: 0 0 0 1px #d7e2e0 inset;
    background: #ffffff;
  }

  :deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 1px #0f6d66 inset, 0 0 0 3px rgba(15, 109, 102, 0.12);
  }

  .input-icon {
    width: 15px;
    height: 15px;
    color: #60747a;
  }
}

.code-row {
  :deep(.el-form-item__content) {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 122px;
    gap: 10px;
  }
}

.login-code {
  height: 46px;
  padding: 0;
  border: 1px solid #d7e2e0;
  border-radius: 4px;
  background: #ffffff;
  cursor: pointer;
  overflow: hidden;
}

.login-code-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.form-options {
  min-height: 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 2px 0 18px;
}

.register-link {
  color: #0f6d66;
  font-size: 14px;
  text-decoration: none;

  &:hover {
    color: #0b5752;
  }
}

.login-button {
  width: 100%;
  height: 46px;
  border-radius: 4px;
  border: none;
  background: #0f6d66;
  font-weight: 700;

  &:hover,
  &:focus {
    background: #0b5f59;
  }
}

.login-actions {
  display: grid;
  gap: 12px;
  margin-top: 12px;
  text-align: center;
}

.create-account-link {
  color: #0f6d66;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;

  &:hover {
    color: #0b5752;
  }
}

.signin-note {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid #dbe7e5;
  color: #74858a;
  font-size: 13px;
  line-height: 1.6;
}

.el-login-footer {
  position: fixed;
  bottom: 14px;
  left: 0;
  width: 100%;
  text-align: center;
  color: rgba(255, 255, 255, 0.76);
  font-size: 12px;
}

@media (max-width: 920px) {
  .login-page {
    align-items: flex-start;
    padding: 24px 16px 58px;
  }

  .login-shell {
    grid-template-columns: 1fr;
    min-height: 0;
  }

  .brand-panel {
    padding: 30px 24px;
    gap: 34px;
    border-right: none;
    border-bottom: 1px solid #d8e5e3;
  }

  .brand-copy h1 {
    font-size: 30px;
  }

  .summary {
    font-size: 15px;
  }

  .signin-panel {
    padding: 28px 24px 32px;
  }
}

@media (max-width: 520px) {
  .login-page {
    min-height: 100svh;
    padding: 0;
    background: #f7faf9;
  }

  .login-shell {
    width: 100%;
    min-height: 100svh;
    border: 0;
    box-shadow: none;
  }

  .brand-panel {
    padding: 16px 18px;
    gap: 0;
    background: #ffffff;
  }

  .brand-top {
    align-items: center;
    gap: 10px;
  }

  .brand-mark {
    width: 38px;
    height: 38px;

    span {
      width: 18px;
      height: 18px;
    }
  }

  .brand-system {
    font-size: 16px;
  }

  .brand-subtitle {
    margin-top: 2px;
    font-size: 10px;
  }

  .brand-copy,
  .capability-grid {
    display: none;
  }

  .signin-panel {
    align-items: flex-start;
    padding: 18px 18px 24px;
  }

  .signin-header {
    margin-bottom: 18px;

    p {
      margin-bottom: 6px;
    }

    span {
      margin-top: 6px;
    }
  }

  .signin-header h2 {
    font-size: 22px;
  }

  .demo-switch {
    margin-bottom: 16px;
    padding: 12px;
  }

  .demo-switch__title {
    margin-bottom: 8px;

    span {
      display: none;
    }
  }

  .login-form {
    :deep(.el-form-item) {
      margin-bottom: 14px;
    }
  }

  .code-row {
    :deep(.el-form-item__content) {
      grid-template-columns: minmax(0, 1fr) 110px;
      gap: 8px;
    }
  }

  .login-code {
    width: 110px;
  }

  .form-options {
    margin-bottom: 14px;
  }

  .login-actions,
  .signin-note,
  .el-login-footer {
    display: none;
  }
}
</style>
