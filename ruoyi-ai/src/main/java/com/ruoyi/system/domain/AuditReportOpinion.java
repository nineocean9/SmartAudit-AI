package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * 审计报告意见实体
 * 对应数据库表 audit_report_opinion
 *
 * @author ruoyi
 */
public class AuditReportOpinion extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long reportId;
    private Integer roundNo;
    private String opinionType;
    private String content;
    private String attachment;
    private String submitBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getReportId() { return reportId; }
    public void setReportId(Long v) { this.reportId = v; }
    public Integer getRoundNo() { return roundNo; }
    public void setRoundNo(Integer v) { this.roundNo = v; }
    public String getOpinionType() { return opinionType; }
    public void setOpinionType(String v) { this.opinionType = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public String getAttachment() { return attachment; }
    public void setAttachment(String v) { this.attachment = v; }
    public String getSubmitBy() { return submitBy; }
    public void setSubmitBy(String v) { this.submitBy = v; }
    public Date getSubmitTime() { return submitTime; }
    public void setSubmitTime(Date v) { this.submitTime = v; }
}
