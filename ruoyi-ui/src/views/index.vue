<template>
  <div class="app-container audit-home">
    <section class="page-header">
      <div>
        <div class="system-label">{{ currentHome.label }}</div>
        <h1>{{ currentHome.title }}</h1>
        <p>{{ currentHome.subtitle }}</p>
      </div>
      <div class="header-brief">
        <span>{{ currentHome.briefLabel }}</span>
        <strong>{{ currentHome.briefValue }}</strong>
        <small>{{ currentHome.briefHint }}</small>
      </div>
    </section>

    <section class="stats-grid">
      <div v-for="item in currentHome.overview" :key="item.label" class="stat-card">
        <div class="stat-label">{{ item.label }}</div>
        <div class="stat-value">{{ item.value }}</div>
        <div class="stat-desc">{{ item.hint }}</div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <span>常用功能</span>
          <h2>审计业务办理入口</h2>
        </div>
      </div>
      <div class="quick-grid">
        <button v-for="item in accessibleQuickActions" :key="item.title" class="quick-card" type="button" @click="goPage(item)">
          <span class="quick-icon">
            <el-icon><component :is="item.icon" /></el-icon>
          </span>
          <span class="quick-text">
            <strong>{{ item.title }}</strong>
            <small>{{ item.desc }}</small>
          </span>
        </button>
      </div>
      <el-empty v-if="accessibleQuickActions.length === 0" description="当前角色暂无可用业务入口" :image-size="80" />
    </section>

    <el-row :gutter="18" class="content-row">
      <el-col :xs="24" :lg="15">
        <section class="panel fill-height">
          <div class="panel-title">
            <div>
              <span>{{ currentHome.flowLabel }}</span>
              <h2>{{ currentHome.flowTitle }}</h2>
            </div>
            <el-button v-if="canAccess(progressAction)" link type="primary" icon="Right" @click="goPage(progressAction)">查看进度</el-button>
          </div>
          <div class="flow-list">
            <div v-for="(step, index) in currentHome.flow" :key="step.title" class="flow-item">
              <div class="step-index">{{ index + 1 }}</div>
              <div>
                <div class="step-title">
                  <h3>{{ step.title }}</h3>
                  <el-tag :type="step.type" effect="plain">{{ step.status }}</el-tag>
                </div>
                <p>{{ step.desc }}</p>
              </div>
            </div>
          </div>
        </section>
      </el-col>

      <el-col :xs="24" :lg="9">
        <section class="panel fill-height">
          <div class="panel-title">
            <div>
              <span>{{ currentHome.assistLabel }}</span>
              <h2>{{ currentHome.assistTitle }}</h2>
            </div>
          </div>
          <div class="ai-list">
            <div v-for="item in accessibleAiCapabilities" :key="item.title" class="ai-item">
              <div>
                <h3>{{ item.title }}</h3>
                <p>{{ item.desc }}</p>
              </div>
              <el-button link type="primary" icon="Position" @click="goPage(item)">进入</el-button>
            </div>
          </div>
          <el-empty v-if="accessibleAiCapabilities.length === 0" description="当前角色暂无可用智能能力" :image-size="80" />
        </section>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="Index">
import { useRouter } from 'vue-router'
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import useUserStore from '@/store/modules/user'
import {
  Document,
  DocumentChecked,
  Files,
  FolderOpened,
  Histogram,
  Tickets
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const allPermission = '*:*:*'
const superAdminRole = 'admin'

const rolePriority = [
  'admin',
  'school_leader',
  'audit_director',
  'audit_project_leader',
  'audit_staff',
  'audited_unit_principal',
  'audited_unit_liaison',
  'intermediary_auditor'
]

const commonOverview = [
  { label: '年度计划', value: '12', hint: '年度审计与专项审计统筹' },
  { label: '在审项目', value: '8', hint: '覆盖校内重点单位与专项资金' },
  { label: '待整改问题', value: '26', hint: '按责任单位持续跟踪' },
  { label: '归档材料', value: '14', hint: '报告、底稿、证明与整改资料' }
]

const roleHomes = {
  admin: {
    label: '平台管理视图',
    title: '高校智慧审计管理平台',
    subtitle: '面向系统管理、权限配置、审计业务配置和运行维护的综合工作台。',
    briefLabel: '当前视角',
    briefValue: '系统管理员',
    briefHint: '关注组织、角色、菜单、审计业务基础数据。',
    flowLabel: '管理闭环',
    flowTitle: '平台配置与业务支撑',
    assistLabel: '智能能力',
    assistTitle: '系统级辅助工具',
    overview: commonOverview,
    flow: [
      { title: '组织与角色', status: '管理', type: 'info', desc: '维护学校、审计处、被审计单位、中介机构和业务角色授权。' },
      { title: '菜单权限', status: '管理', type: 'info', desc: '配置审计业务菜单、按钮权限和不同角色可访问范围。' },
      { title: '基础数据', status: '维护', type: 'success', desc: '维护审计依据、项目资料、报告模板和归档分类。' },
      { title: '运行检查', status: '跟踪', type: 'warning', desc: '跟踪系统服务、AI 能力、数据初始化和演示账号状态。' }
    ]
  },
  school_leader: {
    label: '校领导决策视图',
    title: '全校审计监督驾驶舱',
    subtitle: '聚焦审计项目进展、重点问题、整改成效和报告归档情况，为校级决策提供审计支撑。',
    briefLabel: '今日关注',
    briefValue: '1 项超期预警',
    briefHint: '建议优先查看项目进度和整改闭环情况。',
    flowLabel: '监督重点',
    flowTitle: '审计态势与决策关注',
    assistLabel: '决策辅助',
    assistTitle: '领导可用智能能力',
    overview: [
      { label: '年度覆盖项目', value: '8', hint: '预算、工程、经责和专项审计' },
      { label: '实施中项目', value: '3', hint: '重点跟踪进度偏差和阶段成果' },
      { label: '待关注问题', value: '5', hint: '涉及预算执行和采购管理' },
      { label: '已归档项目', value: '3', hint: '可查看报告和整改结果' }
    ],
    flow: [
      { title: '看总体态势', status: '只读', type: 'info', desc: '查看年度项目覆盖、实施进度、问题分布和整改趋势。' },
      { title: '看重点项目', status: '只读', type: 'info', desc: '关注超期、重大问题、重要单位和专项资金相关审计项目。' },
      { title: '看报告结论', status: '只读', type: 'success', desc: '查阅已定稿报告、归档材料和整改销号情况。' },
      { title: '问审计助手', status: '可用', type: 'success', desc: '用自然语言查询项目、问题、依据和报告摘要。' }
    ]
  },
  audit_director: {
    label: '审计处长工作视图',
    title: '审计业务统筹与质量控制',
    subtitle: '面向审计处负责人，集中处理计划安排、项目调度、流程审批、质量复核和整改督办。',
    briefLabel: '处内待办',
    briefValue: '4 项需处理',
    briefHint: '包含报告审核、归档审核和整改督办。',
    flowLabel: '管理闭环',
    flowTitle: '计划、实施、报告、整改全流程',
    assistLabel: '管理辅助',
    assistTitle: '处长可用智能能力',
    overview: [
      { label: '年度计划', value: '12', hint: '计划编制与项目分解' },
      { label: '待审核报告', value: '1', hint: '征求意见稿待复核' },
      { label: '整改督办', value: '5', hint: '跨单位整改跟踪' },
      { label: '归档审核', value: '2', hint: '待确认归档完整性' }
    ],
    flow: [
      { title: '计划统筹', status: '管理', type: 'success', desc: '维护年度计划、专项计划和审计项目优先级。' },
      { title: '项目调度', status: '管理', type: 'success', desc: '查看项目进度、人员分工、超期预警和关键节点。' },
      { title: '质量复核', status: '审核', type: 'warning', desc: '复核方案、底稿、报告和取证材料完整性。' },
      { title: '整改督办', status: '跟踪', type: 'warning', desc: '跟踪责任单位反馈、佐证材料和销号结果。' }
    ]
  },
  audit_project_leader: {
    label: '项目组长工作视图',
    title: '审计项目作业工作台',
    subtitle: '围绕本人负责项目，集中处理资料、方案、底稿、问题、报告和归档提交。',
    briefLabel: '项目重点',
    briefValue: '2 个项目在办',
    briefHint: '优先完善底稿、报告和归档材料。',
    flowLabel: '作业闭环',
    flowTitle: '项目组长办理事项',
    assistLabel: '作业辅助',
    assistTitle: '项目组可用智能能力',
    overview: [
      { label: '负责项目', value: '2', hint: '预算执行与工程审计' },
      { label: '待完善底稿', value: '4', hint: '含工程变更和整改跟踪' },
      { label: '待提交报告', value: '1', hint: '征求意见稿待完善' },
      { label: '归档待补', value: '2', hint: '立项类、结论类材料' }
    ],
    flow: [
      { title: '组织资料', status: '办理', type: 'success', desc: '汇总被审计单位材料、项目文档和前期准备资料。' },
      { title: '编制方案', status: '办理', type: 'success', desc: '维护审计方案、人员分工、重点事项和实施步骤。' },
      { title: '复核底稿', status: '复核', type: 'warning', desc: '检查审计底稿、问题依据和取证材料质量。' },
      { title: '提交归档', status: '提交', type: 'warning', desc: '提交报告、整改结果和项目归档材料。' }
    ]
  },
  audit_staff: {
    label: '审计人员作业视图',
    title: '审计作业与问题核查',
    subtitle: '面向普通审计人员，突出资料核查、底稿编写、问题登记、依据检索和 AI 辅助分析。',
    briefLabel: '作业提示',
    briefValue: '3 项待完善',
    briefHint: '建议先补齐底稿和问题依据。',
    flowLabel: '个人作业',
    flowTitle: '审计人员办理事项',
    assistLabel: '作业辅助',
    assistTitle: '审计人员可用工具',
    overview: [
      { label: '参与项目', value: '3', hint: '按分工查看资料和底稿' },
      { label: '待写底稿', value: '3', hint: '预算、采购、资产相关底稿' },
      { label: '待核问题', value: '6', hint: '需补充依据和证据链' },
      { label: '可用依据', value: '5类', hint: '法规、制度、案例、风险、模板' }
    ],
    flow: [
      { title: '查阅资料', status: '办理', type: 'success', desc: '查看项目资料、上传材料和解析后的文本内容。' },
      { title: '编写底稿', status: '办理', type: 'success', desc: '整理审计过程、事实描述、证据和法规依据。' },
      { title: '登记问题', status: '办理', type: 'warning', desc: '登记问题事实、影响金额、整改建议和责任单位。' },
      { title: '辅助分析', status: '可用', type: 'info', desc: '使用 AI 进行依据匹配、风险分析和文档核查。' }
    ]
  },
  audited_unit_principal: {
    label: '被审计单位负责人视图',
    title: '本单位审计整改工作台',
    subtitle: '面向被审计单位负责人，集中查看本单位审计报告、问题清单、整改要求和销号结果。',
    briefLabel: '整改关注',
    briefValue: '2 项待反馈',
    briefHint: '请关注整改责任、完成时限和佐证材料。',
    flowLabel: '整改闭环',
    flowTitle: '本单位整改责任事项',
    assistLabel: '查询辅助',
    assistTitle: '可用查询能力',
    overview: [
      { label: '本单位项目', value: '2', hint: '预算执行与经济责任审计' },
      { label: '待整改问题', value: '2', hint: '需责任部门反馈' },
      { label: '已反馈事项', value: '1', hint: '等待审计复核' },
      { label: '报告材料', value: '3', hint: '可查看正式稿和附件' }
    ],
    flow: [
      { title: '查看报告', status: '只读', type: 'info', desc: '查看本单位审计报告、问题清单和整改要求。' },
      { title: '分解责任', status: '办理', type: 'warning', desc: '明确整改责任部门、责任人和完成时限。' },
      { title: '组织反馈', status: '办理', type: 'warning', desc: '协调上传整改说明、制度文件和佐证材料。' },
      { title: '查看销号', status: '只读', type: 'success', desc: '查看审计复核结果和整改销号情况。' }
    ]
  },
  audited_unit_liaison: {
    label: '被审计单位联络员视图',
    title: '资料报送与整改反馈',
    subtitle: '面向被审计单位联络员，突出资料提交、取证回复、整改材料上传和沟通协同。',
    briefLabel: '报送提醒',
    briefValue: '3 份材料待补',
    briefHint: '优先补齐项目资料和整改佐证。',
    flowLabel: '资料协同',
    flowTitle: '联络员办理事项',
    assistLabel: '辅助查询',
    assistTitle: '资料与整改辅助',
    overview: [
      { label: '待提交资料', value: '3', hint: '合同、发票、制度文件' },
      { label: '取证回复', value: '2', hint: '需补充说明和附件' },
      { label: '整改材料', value: '2', hint: '待上传佐证文件' },
      { label: '已办事项', value: '5', hint: '资料和反馈记录可追溯' }
    ],
    flow: [
      { title: '提交资料', status: '办理', type: 'warning', desc: '按清单上传审计所需资料和补充说明。' },
      { title: '回复取证', status: '办理', type: 'warning', desc: '对审计取证事项进行确认、解释和补充附件。' },
      { title: '反馈整改', status: '办理', type: 'warning', desc: '提交整改说明、制度修订和落实证明材料。' },
      { title: '查看进度', status: '只读', type: 'info', desc: '查看本单位资料接收、问题反馈和整改复核状态。' }
    ]
  },
  intermediary_auditor: {
    label: '中介审计人员视图',
    title: '外部审计协同工作台',
    subtitle: '面向受托中介审计人员，聚焦授权项目、资料核查、工程审计底稿和阶段成果提交。',
    briefLabel: '授权状态',
    briefValue: '项目期内有效',
    briefHint: '仅可访问分配项目和授权范围内资料。',
    flowLabel: '协同作业',
    flowTitle: '中介审计办理事项',
    assistLabel: '项目辅助',
    assistTitle: '授权范围内智能能力',
    overview: [
      { label: '授权项目', value: '1', hint: '图书馆工程审计' },
      { label: '待核资料', value: '4', hint: '合同、签证、结算资料' },
      { label: '待交底稿', value: '2', hint: '工程变更和结算核验' },
      { label: '授权剩余', value: '30天', hint: '按项目周期自动回收' }
    ],
    flow: [
      { title: '查看授权项目', status: '只读', type: 'info', desc: '查看受托项目范围、资料清单和审计要求。' },
      { title: '核查工程资料', status: '办理', type: 'warning', desc: '核查合同履行、工程变更、签证和结算资料。' },
      { title: '提交底稿', status: '办理', type: 'warning', desc: '提交阶段性底稿、疑点说明和取证材料。' },
      { title: '交付成果', status: '提交', type: 'success', desc: '形成中介审计成果和项目归档支撑材料。' }
    ]
  }
}

const quickActions = [
  { title: '审计计划', desc: '年度计划、专项计划、审计对象', icon: Tickets, path: '/audit/plan', perms: ['audit:plan:view'], audiences: ['admin', 'audit_director'] },
  { title: '项目工作台', desc: '方案、底稿、报告、协同日志', icon: FolderOpened, path: '/audit/project', perms: ['audit:project:view'], audiences: ['admin', 'audit_director', 'audit_project_leader', 'audit_staff', 'intermediary_auditor'] },
  { title: '项目进度', desc: '甘特图、阶段状态、超期预警', icon: Histogram, path: '/audit/progress', perms: ['audit:progress:view'], audiences: ['admin', 'school_leader', 'audit_director', 'audit_project_leader'] },
  { title: '审计问题', desc: '问题登记、依据关联、整改跟踪', icon: DocumentChecked, path: '/audit/issue', perms: ['audit:issue:view'], audiences: ['admin', 'audit_director', 'audit_project_leader', 'audit_staff', 'intermediary_auditor'] },
  { title: '项目库', desc: '项目清单、资料上传、在线预览', icon: Files, path: '/audit/projectLib', perms: ['audit:projectLib:view'], audiences: ['admin', 'audit_director', 'audit_project_leader', 'audit_staff', 'audited_unit_principal', 'audited_unit_liaison', 'intermediary_auditor'] },
  { title: '审计报告', desc: '报告草拟、审核、定稿与预览', icon: Document, path: '/audit/report', perms: ['audit:report:view'], audiences: ['admin', 'school_leader', 'audit_director', 'audit_project_leader', 'audited_unit_principal'] },
  { title: '整改跟踪', desc: '整改责任、进展反馈、销号闭环', icon: DocumentChecked, path: '/audit/rectification', perms: ['audit:rectification:view'], audiences: ['admin', 'school_leader', 'audit_director', 'audited_unit_principal', 'audited_unit_liaison'] },
  { title: '项目归档', desc: '归档材料、审核状态、档案移交', icon: Files, path: '/audit/archive', perms: ['audit:archive:view'], audiences: ['admin', 'school_leader', 'audit_director', 'audit_project_leader', 'audited_unit_principal'] }
]

const aiCapabilities = [
  { title: '自然语言审计助手', desc: '在统一聊天窗口完成查资料、问依据、读项目和组合任务处理。', path: '/ai/chat', roles: ['school_leader', 'audit_director', 'audit_project_leader', 'audit_staff'] },
  { title: '项目数据分析', desc: '基于项目资料生成数据驾驶舱、图表分析和审计摘要。', path: '/visualization/index', perms: ['audit:visual:view'] },
  { title: '取证单生成', desc: '按问题、证据、依据、结论和建议生成结构化取证单。', path: '/ai/forensic', perms: ['ai:forensic:view'] },
  { title: '审计依据库', desc: '维护法规制度与审计依据，支撑问答、定性和报告撰写。', path: '/ai/basis', perms: ['ai:basis:query', 'audit:basis:query'] }
]

const progressAction = { path: '/audit/progress', perms: ['audit:progress:view'] }
const currentRole = computed(() => {
  const roles = userStore.roles || []
  return rolePriority.find(role => roles.includes(role)) || 'audit_staff'
})
const currentHome = computed(() => roleHomes[currentRole.value] || roleHomes.audit_staff)
const accessibleQuickActions = computed(() => quickActions.filter(item => isAudience(item) && canAccess(item)))
const accessibleAiCapabilities = computed(() => aiCapabilities.filter(canAccess))

function isAudience(item) {
  const roles = userStore.roles || []
  if (roles.includes(superAdminRole)) return true
  if (!item.audiences?.length) return true
  return item.audiences.some(role => roles.includes(role))
}

function canAccess(item) {
  const permissions = userStore.permissions || []
  const roles = userStore.roles || []
  if (permissions.includes(allPermission) || roles.includes(superAdminRole)) return true

  const hasPerm = item.perms?.length
    ? item.perms.some(permission => permissions.includes(permission))
    : false
  const hasRole = item.roles?.length
    ? item.roles.some(role => roles.includes(role))
    : false

  if (item.perms?.length && item.roles?.length) return hasPerm || hasRole
  if (item.perms?.length) return hasPerm
  if (item.roles?.length) return hasRole
  return true
}

function goPage(item) {
  if (!canAccess(item)) {
    ElMessage.warning('当前角色没有访问该功能的权限')
    return
  }
  router.push(item.path)
}
</script>

<style scoped lang="scss">
.audit-home {
  min-height: calc(100vh - 84px);
  padding: 18px;
  color: #1f2937;
  background: #f3f6f9;
}

.page-header {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 20px;
  padding: 26px 28px;
  border: 1px solid #d9e2ec;
  border-radius: 6px;
  background: linear-gradient(135deg, #ffffff 0%, #eef5f7 100%);

  h1 {
    margin: 8px 0 10px;
    color: #12263a;
    font-size: 30px;
    line-height: 1.25;
    font-weight: 700;
    letter-spacing: 0;
  }

  p {
    max-width: 760px;
    margin: 0;
    color: #5f6f82;
    font-size: 15px;
    line-height: 1.8;
  }
}

.system-label {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 10px;
  border-left: 3px solid #176b87;
  color: #176b87;
  background: #e8f3f6;
  font-size: 13px;
  font-weight: 700;
}

.header-brief {
  display: flex;
  flex: 0 0 260px;
  flex-direction: column;
  justify-content: center;
  padding: 18px;
  border-left: 1px solid #d9e2ec;
  background: rgba(255, 255, 255, 0.52);

  span,
  small {
    color: #6b7c90;
  }

  strong {
    margin: 8px 0;
    color: #12263a;
    font-size: 22px;
    line-height: 1.25;
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}

.stat-card,
.panel {
  border: 1px solid #dce4ec;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(31, 41, 55, 0.04);
}

.stat-card {
  padding: 18px 20px;
}

.stat-label {
  color: #687789;
  font-size: 13px;
}

.stat-value {
  margin: 8px 0 6px;
  color: #12263a;
  font-size: 30px;
  line-height: 1;
  font-weight: 700;
}

.stat-desc {
  color: #7b8794;
  font-size: 13px;
}

.panel {
  margin-top: 16px;
  padding: 20px;
}

.fill-height {
  height: calc(100% - 16px);
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;

  span {
    color: #176b87;
    font-size: 13px;
    font-weight: 700;
  }

  h2 {
    margin: 5px 0 0;
    color: #12263a;
    font-size: 18px;
    line-height: 1.35;
    font-weight: 700;
  }
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 86px;
  padding: 14px;
  border: 1px solid #e1e8ef;
  border-radius: 6px;
  color: inherit;
  background: #fbfcfd;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;

  &:hover {
    border-color: #176b87;
    box-shadow: 0 8px 18px rgba(23, 107, 135, 0.1);
    transform: translateY(-1px);
  }
}

.quick-icon {
  display: grid;
  flex: 0 0 42px;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 6px;
  color: #176b87;
  background: #e8f3f6;
  font-size: 22px;
}

.quick-text {
  min-width: 0;

  strong,
  small {
    display: block;
  }

  strong {
    margin-bottom: 5px;
    color: #12263a;
    font-size: 15px;
  }

  small {
    color: #69788a;
    line-height: 1.5;
  }
}

.content-row {
  margin-top: 2px;
}

.flow-list {
  display: grid;
  gap: 12px;
}

.flow-item {
  display: flex;
  gap: 12px;
  padding: 14px;
  border: 1px solid #e5ebf1;
  border-radius: 6px;
  background: #fbfcfd;

  p {
    margin: 8px 0 0;
    color: #64748b;
    line-height: 1.7;
  }
}

.step-index {
  display: grid;
  flex: 0 0 30px;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: #176b87;
  font-weight: 700;
}

.step-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;

  h3 {
    margin: 0;
    color: #12263a;
    font-size: 15px;
  }
}

.ai-list {
  display: grid;
  gap: 10px;
}

.ai-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 13px 0;
  border-bottom: 1px solid #edf1f5;

  &:last-child {
    border-bottom: 0;
  }

  h3 {
    margin: 0 0 7px;
    color: #12263a;
    font-size: 15px;
  }

  p {
    margin: 0;
    color: #64748b;
    line-height: 1.65;
  }
}

@media (max-width: 1200px) {
  .page-header {
    flex-direction: column;
  }

  .header-brief {
    flex-basis: auto;
    border-top: 1px solid #d9e2ec;
    border-left: 0;
  }

  .stats-grid,
  .quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .audit-home {
    padding: 12px;
  }

  .page-header {
    padding: 20px;

    h1 {
      font-size: 24px;
    }
  }

  .stats-grid,
  .quick-grid {
    grid-template-columns: 1fr;
  }

  .panel-title,
  .step-title {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
