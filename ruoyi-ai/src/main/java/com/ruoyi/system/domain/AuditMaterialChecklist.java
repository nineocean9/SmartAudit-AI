package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * 审计资料清单实体
 * 对应数据库表 audit_material_checklist
 *
 * @author ruoyi
 */
public class AuditMaterialChecklist extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private String materialName;
    private String materialType;
    private Integer required;
    private Integer submitStatus;
    private String filePath;
    private String submitBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;

    private String source;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long v) { this.projectId = v; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String v) { this.materialName = v; }
    public String getMaterialType() { return materialType; }
    public void setMaterialType(String v) { this.materialType = v; }
    public Integer getRequired() { return required; }
    public void setRequired(Integer v) { this.required = v; }
    public Integer getSubmitStatus() { return submitStatus; }
    public void setSubmitStatus(Integer v) { this.submitStatus = v; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String v) { this.filePath = v; }
    public String getSubmitBy() { return submitBy; }
    public void setSubmitBy(String v) { this.submitBy = v; }
    public Date getSubmitTime() { return submitTime; }
    public void setSubmitTime(Date v) { this.submitTime = v; }
    public String getSource() { return source; }
    public void setSource(String v) { this.source = v; }
}
