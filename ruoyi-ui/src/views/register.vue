<template>
  <div class="register-page">
    <div class="register-shell">
      <section class="register-intro">
        <div class="brand-top">
          <div class="brand-mark"><span></span></div>
          <div>
            <div class="brand-system">高校一体化智慧审计平台</div>
            <div class="brand-subtitle">Account Registration</div>
          </div>
        </div>
        <div>
          <p class="eyebrow">账号申请</p>
          <h1>按组织归属创建审计协同账号</h1>
          <p>可选择已有学校、学院、部门、公司，也可新建独立组织。注册账号默认作为被审计单位联络员，后续由管理员或审计处调整角色。</p>
        </div>
      </section>

      <section class="register-card">
        <div class="register-header">
          <h2>注册账号</h2>
          <span>请填写真实组织归属，便于项目数据按部门隔离</span>
        </div>

        <el-form ref="registerRef" :model="registerForm" :rules="registerRules" label-position="top" class="register-form">
          <div class="form-section-title">账号信息</div>
          <el-row :gutter="14">
            <el-col :span="12">
              <el-form-item label="账号" prop="username">
                <el-input v-model="registerForm.username" size="large" placeholder="2-20位账号" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="姓名/联系人" prop="nickName">
                <el-input v-model="registerForm.nickName" size="large" placeholder="真实姓名" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="14">
            <el-col :span="12">
              <el-form-item label="密码" prop="password">
                <el-input v-model="registerForm.password" type="password" size="large" show-password placeholder="5-20位密码" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model="registerForm.confirmPassword" type="password" size="large" show-password placeholder="再次输入密码" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="14">
            <el-col :span="12">
              <el-form-item label="手机号" prop="phonenumber">
                <el-input v-model="registerForm.phonenumber" size="large" placeholder="可选" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="registerForm.email" size="large" placeholder="可选" />
              </el-form-item>
            </el-col>
          </el-row>

          <div class="form-section-title">组织归属</div>
          <el-form-item label="组织方式" prop="deptMode">
            <el-segmented v-model="registerForm.deptMode" :options="deptModeOptions" />
          </el-form-item>

          <template v-if="registerForm.deptMode === 'existing'">
            <el-form-item label="选择已有组织" prop="deptRef">
              <el-tree-select
                v-model="registerForm.deptRef"
                :data="deptOptions"
                :props="{ value: 'id', label: 'label', children: 'children' }"
                value-key="id"
                check-strictly
                filterable
                size="large"
                placeholder="请选择学校、学院、部门或公司"
              />
            </el-form-item>
          </template>

          <template v-else>
            <el-row :gutter="14">
              <el-col :span="10">
                <el-form-item label="组织类型" prop="orgType">
                  <el-select v-model="registerForm.orgType" size="large" placeholder="请选择">
                    <el-option label="学校" value="school" />
                    <el-option label="学院" value="college" />
                    <el-option label="公司" value="company" />
                    <el-option label="部门" value="dept" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="14">
                <el-form-item label="组织名称" prop="orgName">
                  <el-input v-model="registerForm.orgName" size="large" placeholder="如 A公司、商学院、后勤处" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="挂靠到" prop="parentDeptId">
              <el-tree-select
                v-model="registerForm.parentDeptId"
                :data="deptOptions"
                :props="{ value: 'id', label: 'label', children: 'children' }"
                value-key="id"
                check-strictly
                filterable
                clearable
                size="large"
                placeholder="默认挂靠到示范高校或第一个可用组织"
              />
            </el-form-item>
          </template>

          <el-form-item label="验证码" prop="code" v-if="captchaEnabled">
            <div class="code-row">
              <el-input v-model="registerForm.code" size="large" placeholder="验证码" @keyup.enter="handleRegister" />
              <button type="button" class="register-code" title="点击刷新验证码" @click="getCode">
                <img :src="codeUrl" class="register-code-img" />
              </button>
            </div>
          </el-form-item>

          <el-button :loading="loading" size="large" type="primary" class="register-button" @click.prevent="handleRegister">
            <span v-if="!loading">提交注册</span>
            <span v-else>正在提交...</span>
          </el-button>

          <div class="form-links">
            <router-link class="link-type" :to="'/login'">已有账号，返回登录</router-link>
          </div>
        </el-form>
      </section>
    </div>

    <div class="el-register-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script setup>
import { ElMessageBox } from "element-plus"
import { getCodeImg, register, getRegisterDeptTree } from "@/api/login"
import defaultSettings from '@/settings'

const title = import.meta.env.VITE_APP_TITLE
const footerContent = defaultSettings.footerContent
const router = useRouter()
const { proxy } = getCurrentInstance()

const registerForm = ref({
  username: "",
  nickName: "",
  password: "",
  confirmPassword: "",
  phonenumber: "",
  email: "",
  deptMode: "existing",
  deptRef: undefined,
  deptId: undefined,
  parentDeptId: undefined,
  orgType: "company",
  orgName: "",
  code: "",
  uuid: ""
})

const deptModeOptions = [
  { label: "选择已有组织", value: "existing" },
  { label: "新建组织", value: "new" }
]

const equalToPassword = (rule, value, callback) => {
  if (registerForm.value.password !== value) {
    callback(new Error("两次输入的密码不一致"))
  } else {
    callback()
  }
}

const requireDept = (rule, value, callback) => {
  if (registerForm.value.deptMode === "existing" && !registerForm.value.deptRef) {
    callback(new Error("请选择已有组织"))
  } else {
    callback()
  }
}

const requireOrgName = (rule, value, callback) => {
  if (registerForm.value.deptMode === "new" && !value) {
    callback(new Error("请输入组织名称"))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, trigger: "blur", message: "请输入账号" },
    { min: 2, max: 20, message: "账号长度必须介于 2 和 20 之间", trigger: "blur" }
  ],
  nickName: [{ required: true, trigger: "blur", message: "请输入姓名或联系人" }],
  password: [
    { required: true, trigger: "blur", message: "请输入密码" },
    { min: 5, max: 20, message: "密码长度必须介于 5 和 20 之间", trigger: "blur" },
    { pattern: /^[^<>"'|\\]+$/, message: "不能包含非法字符：< > \" ' \\ |", trigger: "blur" }
  ],
  confirmPassword: [
    { required: true, trigger: "blur", message: "请再次输入密码" },
    { validator: equalToPassword, trigger: "blur" }
  ],
  deptId: [{ validator: requireDept, trigger: "change" }],
  deptRef: [{ validator: requireDept, trigger: "change" }],
  orgName: [{ validator: requireOrgName, trigger: "blur" }],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

const codeUrl = ref("")
const loading = ref(false)
const captchaEnabled = ref(true)
const deptOptions = ref([])

function handleRegister() {
  proxy.$refs.registerRef.validate(valid => {
    if (!valid) return
    loading.value = true
    register(registerForm.value).then(() => {
      const username = registerForm.value.username
      ElMessageBox.alert(`账号 ${username} 注册成功，请使用该账号登录。`, "系统提示", {
        type: "success"
      }).then(() => {
        router.push("/login")
      }).catch(() => {
        router.push("/login")
      })
    }).catch(() => {
      if (captchaEnabled.value) {
        getCode()
      }
    }).finally(() => {
      loading.value = false
    })
  })
}

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img
      registerForm.value.uuid = res.uuid
      registerForm.value.code = ""
    }
  })
}

function getDeptTree() {
  getRegisterDeptTree().then(res => {
    deptOptions.value = res.data || []
    const firstGroup = deptOptions.value.find(item => item.children && item.children.length > 0)
    if (!registerForm.value.deptRef && firstGroup) {
      registerForm.value.deptRef = firstGroup.children[0].id
      if (String(registerForm.value.deptRef).startsWith("dept:")) {
        registerForm.value.deptId = Number(String(registerForm.value.deptRef).substring(5))
        registerForm.value.parentDeptId = registerForm.value.deptId
      }
    }
  }).catch(() => {
    deptOptions.value = []
    registerForm.value.deptMode = "new"
  })
}

getCode()
getDeptTree()
</script>

<style lang="scss" scoped>
.register-page {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 44px 28px 64px;
  background:
    linear-gradient(135deg, rgba(9, 63, 70, 0.92), rgba(16, 81, 89, 0.84)),
    url("../assets/images/login-background.jpg") center/cover no-repeat;
}

.register-shell {
  width: min(1120px, 100%);
  display: grid;
  grid-template-columns: 0.85fr 1.15fr;
  background: rgba(248, 251, 250, 0.97);
  border: 1px solid rgba(255, 255, 255, 0.48);
  box-shadow: 0 28px 70px rgba(3, 24, 30, 0.28);
}

.register-intro {
  padding: 42px;
  border-right: 1px solid #d8e5e3;
  background: linear-gradient(180deg, rgba(248, 251, 250, 0.98), rgba(236, 246, 244, 0.95));
  display: flex;
  flex-direction: column;
  justify-content: space-between;

  h1 {
    margin: 0;
    color: #102b31;
    font-size: 36px;
    line-height: 1.22;
    font-weight: 800;
    letter-spacing: 0;
  }

  p {
    color: #52676d;
    line-height: 1.85;
  }
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

.eyebrow {
  margin: 0 0 16px;
  color: #0f6d66;
  font-size: 14px;
  font-weight: 700;
}

.register-card {
  padding: 34px 38px;
  background: #f7faf9;
}

.register-header {
  margin-bottom: 22px;

  h2 {
    margin: 0;
    color: #142f35;
    font-size: 28px;
    font-weight: 800;
  }

  span {
    display: block;
    margin-top: 8px;
    color: #6a7c81;
    font-size: 14px;
  }
}

.form-section-title {
  margin: 18px 0 12px;
  color: #0f6d66;
  font-weight: 700;
}

.register-form {
  :deep(.el-form-item) {
    margin-bottom: 16px;
  }

  :deep(.el-input__wrapper),
  :deep(.el-select__wrapper),
  :deep(.el-tree-select__wrapper) {
    min-height: 44px;
    border-radius: 4px;
    box-shadow: 0 0 0 1px #d7e2e0 inset;
    background: #ffffff;
  }
}

.code-row {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr 126px;
  gap: 10px;
}

.register-code {
  height: 44px;
  padding: 0;
  border: 1px solid #d7e2e0;
  border-radius: 4px;
  background: #ffffff;
  cursor: pointer;
  overflow: hidden;
}

.register-code-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.register-button {
  width: 100%;
  height: 46px;
  margin-top: 4px;
  border: none;
  border-radius: 4px;
  background: #0f6d66;
  font-weight: 700;
}

.form-links {
  margin-top: 16px;
  text-align: center;

  a {
    color: #0f6d66;
    text-decoration: none;
    font-weight: 600;
  }
}

.el-register-footer {
  position: fixed;
  bottom: 14px;
  left: 0;
  width: 100%;
  text-align: center;
  color: rgba(255, 255, 255, 0.76);
  font-size: 12px;
}

@media (max-width: 960px) {
  .register-shell {
    grid-template-columns: 1fr;
  }

  .register-intro {
    min-height: 260px;
    border-right: none;
    border-bottom: 1px solid #d8e5e3;
  }
}

@media (max-width: 640px) {
  .register-page {
    padding: 18px 14px 58px;
  }

  .register-intro,
  .register-card {
    padding: 24px;
  }
}
</style>
