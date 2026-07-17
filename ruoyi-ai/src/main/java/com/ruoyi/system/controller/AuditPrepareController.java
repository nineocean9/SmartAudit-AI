package com.ruoyi.system.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.AuditProjectAccessService;
import com.ruoyi.system.service.IProjectDocService;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/audit/prepare")
public class AuditPrepareController extends BaseController
{
    @Autowired private AuditSchemeTemplateMapper templateMapper;
    @Autowired private AuditProjectMemberMapper memberMapper;
    @Autowired private AuditMaterialChecklistMapper materialMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private AuditProjectAccessService projectAccessService;
    @Autowired private IProjectDocService projectDocService;

    // === 方案模板 ===
    @PreAuthorize("@ss.hasPermi('audit:template:view')")
    @GetMapping("/template/list")
    public TableDataInfo templateList(AuditSchemeTemplate q) { startPage(); return getDataTable(templateMapper.selectList(q)); }

    @PreAuthorize("@ss.hasPermi('audit:template:view')")
    @GetMapping("/template/{id}")
    public AjaxResult getTemplate(@PathVariable Long id) { return success(templateMapper.selectById(id)); }

    @PreAuthorize("@ss.hasPermi('audit:template:view')")
    @GetMapping("/template/byType/{auditType}")
    public AjaxResult templateByType(@PathVariable String auditType) { return success(templateMapper.selectByAuditType(auditType)); }

    @PreAuthorize("@ss.hasPermi('audit:template:edit')")
    @PostMapping("/template")
    public AjaxResult addTemplate(@RequestBody AuditSchemeTemplate t)
    {
        ensureTemplateFileUrlColumn();
        templateMapper.insert(t);
        return success(t);
    }

    @PreAuthorize("@ss.hasPermi('audit:template:edit')")
    @PostMapping("/template/generate-docx")
    public AjaxResult generateDocxTemplate(@RequestBody AuditSchemeTemplate t) throws Exception
    {
        String auditType = t.getAuditType() == null || t.getAuditType().isBlank() ? "经济责任审计" : t.getAuditType();
        String templateName = t.getTemplateName() == null || t.getTemplateName().isBlank()
                ? auditType + "实施方案模板（Word版）" : t.getTemplateName();

        String dirName = "audit-template/" + LocalDate.now();
        File dir = new File(RuoYiConfig.getUploadPath(), dirName);
        if (!dir.exists() && !dir.mkdirs())
        {
            return AjaxResult.error("模板目录创建失败");
        }

        String safeName = templateName.replaceAll("[\\\\/:*?\"<>|]", "_");
        String fileName = safeName + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + ".docx";
        File file = new File(dir, fileName);

        try (XWPFDocument doc = new XWPFDocument(); FileOutputStream out = new FileOutputStream(file))
        {
            writeTitle(doc, templateName);
            writeHeading(doc, "一、项目基本情况");
            writeBody(doc, "（一）被审计单位概况：填写单位性质、组织架构、人员规模、资金资产规模、主要职责及近三年审计情况。");
            writeBody(doc, "（二）审计背景与依据：列明审计立项来源、审计通知书、适用法律法规和内部制度依据。");
            writeHeading(doc, "二、审计目标");
            writeBody(doc, "围绕财政财务收支真实合法效益、重大经济决策执行、内部控制、资产管理和风险防控等方面确定审计目标。");
            writeHeading(doc, "三、审计范围与重点");
            writeBody(doc, "（一）审计期间：一般覆盖近三年，必要时追溯或延伸。");
            writeBody(doc, "（二）重点事项：预算执行、资金使用、资产运营、项目建设、合同管理、采购管理、整改落实等。");
            writeHeading(doc, "四、审计组织与分工");
            writeBody(doc, "明确项目组长、主审、成员职责，列明资料调取、现场核查、数据分析、底稿复核等任务分工。");
            writeHeading(doc, "五、实施步骤与时间安排");
            writeBody(doc, "准备阶段、现场实施阶段、报告征求意见阶段、整改跟踪阶段分别列明起止时间、交付物和质量控制要求。");
            writeHeading(doc, "六、质量控制与风险提示");
            writeBody(doc, "落实三级复核、重大事项请示报告、证据充分性审查、保密要求和廉政纪律要求。");
            doc.write(out);
        }

        String fileUrl = Constants.RESOURCE_PREFIX + "/upload/" + dirName + "/" + fileName;
        AuditSchemeTemplate record = new AuditSchemeTemplate();
        record.setTemplateName(templateName);
        record.setAuditType(auditType);
        record.setContent("[Word模板] " + templateName);
        record.setFileUrl(fileUrl);
        record.setStatus(1);
        record.setCreateBy(getUsername());
        ensureTemplateFileUrlColumn();
        templateMapper.insert(record);
        return success(record);
    }

    @PreAuthorize("@ss.hasPermi('audit:template:edit')")
    @PutMapping("/template")
    public AjaxResult updateTemplate(@RequestBody AuditSchemeTemplate t) { ensureTemplateFileUrlColumn(); return toAjax(templateMapper.update(t)); }

    @PreAuthorize("@ss.hasPermi('audit:template:edit')")
    @PutMapping("/template/{id}/content")
    public AjaxResult updateTemplateContent(@PathVariable Long id, @RequestBody Map<String, String> body)
    {
        return toAjax(templateMapper.updateContent(id, body.get("content")));
    }

    @PreAuthorize("@ss.hasPermi('audit:template:edit')")
    @DeleteMapping("/template/{ids}")
    public AjaxResult delTemplate(@PathVariable Long[] ids) { return toAjax(templateMapper.deleteByIds(ids)); }

    // === 项目成员 ===
    @PreAuthorize("@ss.hasPermi('audit:prepare:view')")
    @GetMapping("/member/{projectId}") public AjaxResult memberList(@PathVariable Long projectId) { if (!projectAccessService.canAccessProject(projectId)) return AjaxResult.error("无权访问该项目准备资料"); return success(memberMapper.selectByProjectId(projectId)); }

    @PreAuthorize("@ss.hasPermi('audit:prepare:edit')")
    @GetMapping("/member/candidates")
    public AjaxResult memberCandidates()
    {
        return success(jdbcTemplate.queryForList(
                "SELECT DISTINCT u.user_id AS \"userId\", u.user_name AS \"userName\", u.nick_name AS \"nickName\", r.role_key AS \"roleKey\", r.role_name AS \"roleName\" "
              + "FROM sys_user u "
              + "JOIN sys_user_role ur ON ur.user_id = u.user_id "
              + "JOIN sys_role r ON r.role_id = ur.role_id "
              + "WHERE u.del_flag = '0' AND u.status = '0' "
              + "AND r.role_key IN ('audit_director','audit_project_leader','audit_staff','intermediary_auditor') "
              + "ORDER BY r.role_key, u.user_id"));
    }

    @PreAuthorize("@ss.hasPermi('audit:prepare:edit')")
    @PostMapping("/member")
    public AjaxResult addMember(@RequestBody AuditProjectMember m)
    {
        if (!projectAccessService.canManageProjectMembers(m.getProjectId()))
        {
            return AjaxResult.error("只有审计处长或项目组长可以维护项目人员分工");
        }
        AjaxResult validation = fillAndValidateAssignableUser(m);
        if (validation != null)
        {
            return validation;
        }
        if (m.getStatus() == null)
        {
            m.setStatus(1);
        }
        return toAjax(memberMapper.insert(m));
    }

    @PreAuthorize("@ss.hasPermi('audit:prepare:edit')")
    @PutMapping("/member")
    public AjaxResult updateMember(@RequestBody AuditProjectMember m)
    {
        Long projectId = findMemberProjectId(m.getId());
        if (!projectAccessService.canManageProjectMembers(projectId))
        {
            return AjaxResult.error("只有审计处长或项目组长可以维护项目人员分工");
        }
        return toAjax(memberMapper.update(m));
    }

    @PreAuthorize("@ss.hasPermi('audit:prepare:edit')")
    @DeleteMapping("/member/{id}")
    public AjaxResult delMember(@PathVariable Long id)
    {
        Long projectId = findMemberProjectId(id);
        if (!projectAccessService.canManageProjectMembers(projectId))
        {
            return AjaxResult.error("只有审计处长或项目组长可以维护项目人员分工");
        }
        return toAjax(memberMapper.deleteById(id));
    }

    // === 资源负载 ===
    @PreAuthorize("@ss.hasPermi('audit:prepare:view')")
    @GetMapping("/workload")
    public AjaxResult workload() { return success(memberMapper.selectUserWorkload()); }

    // === 资料清单 ===
    @PreAuthorize("@ss.hasPermi('audit:prepare:view')")
    @GetMapping("/material/{projectId}") public AjaxResult materialList(@PathVariable Long projectId) { if (!projectAccessService.canAccessProject(projectId)) return AjaxResult.error("无权访问该项目资料清单"); return success(materialMapper.selectByProjectId(projectId)); }

    @PreAuthorize("@ss.hasPermi('audit:prepare:edit')")
    @PostMapping("/material") public AjaxResult addMaterial(@RequestBody AuditMaterialChecklist m) { if (!projectAccessService.canAccessProject(m.getProjectId())) return AjaxResult.error("无权维护该项目资料清单"); return toAjax(materialMapper.insert(m)); }

    @PreAuthorize("@ss.hasPermi('audit:prepare:edit')")
    @PutMapping("/material")
    public AjaxResult updateMaterial(@RequestBody AuditMaterialChecklist m)
    {
        AuditMaterialChecklist existing = materialMapper.selectById(m.getId());
        if (existing == null)
        {
            return AjaxResult.error("资料项不存在");
        }
        if (!projectAccessService.canAccessProject(existing.getProjectId()))
        {
            return AjaxResult.error("无权维护该项目资料清单");
        }
        return toAjax(materialMapper.update(m));
    }

    @PreAuthorize("@ss.hasAnyPermi('audit:prepare:submit,audit:prepare:edit')")
    @PutMapping("/material/{id}/submit")
    @Transactional
    public AjaxResult submitMaterial(@PathVariable Long id, @RequestBody Map<String, String> body)
    {
        AuditMaterialChecklist existing = materialMapper.selectById(id);
        if (existing == null)
        {
            return AjaxResult.error("资料项不存在");
        }
        if (!projectAccessService.canAccessProject(existing.getProjectId()))
        {
            return AjaxResult.error("无权提交该项目资料");
        }
        String filePath = body.get("filePath");
        if (filePath == null || filePath.isBlank())
        {
            return AjaxResult.error("请先上传资料文件");
        }
        existing.setFilePath(filePath);
        existing.setSubmitBy(getUsername());
        existing.setSubmitStatus(1);
        int rows = materialMapper.update(existing);
        if (rows <= 0)
        {
            return AjaxResult.error("资料提交失败");
        }

        Long planId = findProjectPlanId(existing.getProjectId());
        String fileName = buildMaterialFileName(existing.getMaterialName(), filePath);
        String docType = existing.getMaterialType() == null || existing.getMaterialType().isBlank()
                ? "审计准备资料" : existing.getMaterialType();
        String contentText = "审计准备资料：" + existing.getMaterialName()
                + "；提交人：" + getUsername()
                + "；状态：待被审计单位负责人确认";
        projectDocService.syncUploadedDocument(existing.getProjectId(), planId, docType,
                fileName, filePath, contentText, getUsername(), false);
        return success(existing);
    }

    @PreAuthorize("@ss.hasPermi('audit:prepare:confirm')")
    @PutMapping("/material/{id}/confirm")
    @Transactional
    public AjaxResult confirmMaterial(@PathVariable Long id)
    {
        AuditMaterialChecklist existing = materialMapper.selectById(id);
        if (existing == null)
        {
            return AjaxResult.error("资料项不存在");
        }
        if (!projectAccessService.canAccessProject(existing.getProjectId()))
        {
            return AjaxResult.error("无权确认该项目资料");
        }
        if (existing.getFilePath() == null || existing.getFilePath().isBlank())
        {
            return AjaxResult.error("资料尚未提交文件，不能确认");
        }
        existing.setSubmitStatus(2);
        int rows = materialMapper.update(existing);
        if (rows <= 0)
        {
            return AjaxResult.error("资料确认失败");
        }
        Long planId = findProjectPlanId(existing.getProjectId());
        String fileName = buildMaterialFileName(existing.getMaterialName(), existing.getFilePath());
        String docType = existing.getMaterialType() == null || existing.getMaterialType().isBlank()
                ? "审计准备资料" : existing.getMaterialType();
        String contentText = "审计准备资料：" + existing.getMaterialName()
                + "；提交人：" + existing.getSubmitBy() + "；状态：已确认";
        projectDocService.syncUploadedDocument(existing.getProjectId(), planId, docType,
                fileName, existing.getFilePath(), contentText, existing.getSubmitBy(), true);
        return success(existing);
    }

    @PreAuthorize("@ss.hasPermi('audit:prepare:edit')")
    @DeleteMapping("/material/{id}")
    @Transactional
    public AjaxResult delMaterial(@PathVariable Long id)
    {
        AuditMaterialChecklist existing = materialMapper.selectById(id);
        if (existing == null)
        {
            return AjaxResult.error("资料项不存在");
        }
        if (!projectAccessService.canAccessProject(existing.getProjectId()))
        {
            return AjaxResult.error("无权维护该项目资料清单");
        }
        int rows = materialMapper.deleteById(id);
        if (rows > 0)
        {
            projectDocService.hideSyncedDocument(existing.getProjectId(), existing.getFilePath());
        }
        return toAjax(rows);
    }

    private void writeTitle(XWPFDocument doc, String text)
    {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontFamily("宋体");
        run.setFontSize(18);
    }

    private void writeHeading(XWPFDocument doc, String text)
    {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontFamily("黑体");
        run.setFontSize(14);
    }

    private void writeBody(XWPFDocument doc, String text)
    {
        XWPFParagraph p = doc.createParagraph();
        p.setIndentationFirstLine(480);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontFamily("仿宋");
        run.setFontSize(12);
    }

    private void ensureTemplateFileUrlColumn()
    {
        jdbcTemplate.execute("ALTER TABLE audit_scheme_template ADD COLUMN IF NOT EXISTS file_url VARCHAR(500)");
    }

    private AjaxResult fillAndValidateAssignableUser(AuditProjectMember m)
    {
        if (m.getUserId() == null)
        {
            return AjaxResult.error("请选择系统中的审计人员账号");
        }
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT u.user_id, u.user_name, u.nick_name, r.role_key "
              + "FROM sys_user u "
              + "JOIN sys_user_role ur ON ur.user_id = u.user_id "
              + "JOIN sys_role r ON r.role_id = ur.role_id "
              + "WHERE u.user_id = ? AND u.del_flag = '0' AND u.status = '0' "
              + "AND r.role_key IN ('audit_director','audit_project_leader','audit_staff','intermediary_auditor') "
              + "LIMIT 1",
                m.getUserId());
        if (users.isEmpty())
        {
            return AjaxResult.error("只能添加系统内有效的审计人员，不能添加被审计单位账号或自由填写姓名");
        }
        Map<String, Object> user = users.get(0);
        m.setUserName((String) user.get("nick_name"));
        if (m.getUserName() == null || m.getUserName().isBlank())
        {
            m.setUserName((String) user.get("user_name"));
        }
        return null;
    }

    private Long findMemberProjectId(Long id)
    {
        if (id == null) return null;
        List<Long> ids = jdbcTemplate.queryForList("SELECT project_id FROM audit_project_member WHERE id = ?", Long.class, id);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private Long findProjectPlanId(Long projectId)
    {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT plan_id FROM audit_project WHERE id = ?", Long.class, projectId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private String buildMaterialFileName(String materialName, String filePath)
    {
        String name = materialName == null || materialName.isBlank() ? "审计准备资料" : materialName;
        String cleanPath = filePath == null ? "" : filePath.split("[?#]", 2)[0];
        int slash = Math.max(cleanPath.lastIndexOf('/'), cleanPath.lastIndexOf('\\'));
        String uploadedName = slash >= 0 ? cleanPath.substring(slash + 1) : cleanPath;
        int dot = uploadedName.lastIndexOf('.');
        return dot >= 0 ? name + uploadedName.substring(dot) : name;
    }
}
