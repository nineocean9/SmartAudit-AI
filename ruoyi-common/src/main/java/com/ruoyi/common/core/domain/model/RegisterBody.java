package com.ruoyi.common.core.domain.model;

/**
 * User registration request body.
 *
 * @author ruoyi
 */
public class RegisterBody extends LoginBody
{
    private String nickName;

    private String phonenumber;

    private String email;

    private String deptMode;

    private Long deptId;

    private String deptRef;

    private Long parentDeptId;

    private String orgName;

    private String orgType;

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getPhonenumber()
    {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber)
    {
        this.phonenumber = phonenumber;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getDeptMode()
    {
        return deptMode;
    }

    public void setDeptMode(String deptMode)
    {
        this.deptMode = deptMode;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public String getDeptRef()
    {
        return deptRef;
    }

    public void setDeptRef(String deptRef)
    {
        this.deptRef = deptRef;
    }

    public Long getParentDeptId()
    {
        return parentDeptId;
    }

    public void setParentDeptId(Long parentDeptId)
    {
        this.parentDeptId = parentDeptId;
    }

    public String getOrgName()
    {
        return orgName;
    }

    public void setOrgName(String orgName)
    {
        this.orgName = orgName;
    }

    public String getOrgType()
    {
        return orgType;
    }

    public void setOrgType(String orgType)
    {
        this.orgType = orgType;
    }
}
