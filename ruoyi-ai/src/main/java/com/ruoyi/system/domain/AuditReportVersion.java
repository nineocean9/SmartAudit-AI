package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 审计报告版本实体
 * 对应数据库表 audit_report_version
 *
 * @author ruoyi
 */
public class AuditReportVersion extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long reportId;
    private Integer versionNo;
    private String content;
    private String changeDesc;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getReportId() { return reportId; }
    public void setReportId(Long v) { this.reportId = v; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer v) { this.versionNo = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public String getChangeDesc() { return changeDesc; }
    public void setChangeDesc(String v) { this.changeDesc = v; }
}
