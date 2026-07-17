package com.ruoyi.framework.web.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.RegisterBody;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.user.CaptchaException;
import com.ruoyi.common.exception.user.CaptchaExpireException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysDeptService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 注册校验方法
 */
@Component
public class SysRegisterService
{
    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysDeptService deptService;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private RedisCache redisCache;

    /**
     * 注册
     */
    @Transactional
    public String register(RegisterBody registerBody)
    {
        String msg = "";
        String username = StringUtils.trim(registerBody.getUsername());
        String password = registerBody.getPassword();
        SysUser checkUser = new SysUser();
        checkUser.setUserName(username);

        boolean captchaEnabled = configService.selectCaptchaEnabled();
        if (captchaEnabled)
        {
            validateCaptcha(username, registerBody.getCode(), registerBody.getUuid());
        }

        if (StringUtils.isEmpty(username))
        {
            msg = "用户名不能为空";
        }
        else if (StringUtils.isEmpty(password))
        {
            msg = "用户密码不能为空";
        }
        else if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            msg = "账户长度必须在 2 到 20 个字符之间";
        }
        else if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            msg = "密码长度必须在 5 到 20 个字符之间";
        }
        else if (!userService.checkUserNameUnique(checkUser))
        {
            msg = "保存用户'" + username + "'失败，注册账号已存在";
        }
        else
        {
            Long deptId = resolveRegisterDept(registerBody, username);
            if (deptId == null)
            {
                return "请选择已有组织，或填写要新建的公司/学院/部门名称";
            }

            SysUser sysUser = new SysUser();
            sysUser.setUserName(username);
            sysUser.setNickName(StringUtils.defaultIfEmpty(registerBody.getNickName(), username));
            sysUser.setDeptId(deptId);
            sysUser.setEmail(registerBody.getEmail());
            sysUser.setPhonenumber(registerBody.getPhonenumber());
            sysUser.setStatus("0");
            sysUser.setCreateBy("register");
            sysUser.setPwdUpdateDate(DateUtils.getNowDate());
            sysUser.setPassword(SecurityUtils.encryptPassword(password));
            sysUser.setRoleIds(new Long[] { resolveDefaultRegisterRoleId() });

            int rows = userService.insertUser(sysUser);
            if (rows <= 0)
            {
                msg = "注册失败，请联系系统管理员";
            }
            else
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.REGISTER, MessageUtils.message("user.register.success")));
            }
        }
        return msg;
    }

    private Long resolveRegisterDept(RegisterBody registerBody, String username)
    {
        if ("existing".equals(registerBody.getDeptMode()))
        {
            Long deptId = resolveExistingDept(registerBody, username);
            if (deptId != null)
            {
                return deptId;
            }
        }

        String orgName = normalizeOrgName(registerBody.getOrgName(), registerBody.getOrgType());
        if (StringUtils.isEmpty(orgName))
        {
            return null;
        }

        Long parentDeptId = resolveParentDeptId(registerBody.getParentDeptId());
        if (parentDeptId == null)
        {
            return null;
        }

        SysDept dept = new SysDept();
        dept.setParentId(parentDeptId);
        dept.setDeptName(orgName);
        dept.setOrderNum(99);
        dept.setLeader(StringUtils.defaultIfEmpty(registerBody.getNickName(), username));
        dept.setPhone(registerBody.getPhonenumber());
        dept.setEmail(registerBody.getEmail());
        dept.setStatus("0");
        dept.setUnitType(StringUtils.defaultIfEmpty(registerBody.getOrgType(), "被审计单位"));
        dept.setIsAuditTarget(1);
        dept.setCreateBy("register");
        deptService.insertDept(dept);
        return dept.getDeptId();
    }

    private Long resolveExistingDept(RegisterBody registerBody, String username)
    {
        String deptRef = StringUtils.trim(registerBody.getDeptRef());
        if (StringUtils.isEmpty(deptRef) && registerBody.getDeptId() != null)
        {
            deptRef = "dept:" + registerBody.getDeptId();
        }

        if (deptRef.startsWith("dept:"))
        {
            Long deptId = Long.valueOf(deptRef.substring("dept:".length()));
            SysDept dept = deptService.selectDeptById(deptId);
            return dept == null ? null : dept.getDeptId();
        }
        if (deptRef.startsWith("unit:"))
        {
            String unitName = StringUtils.trim(deptRef.substring("unit:".length()));
            if (StringUtils.isEmpty(unitName))
            {
                return null;
            }
            SysDept dept = findDeptByName(unitName);
            if (dept != null)
            {
                return dept.getDeptId();
            }
            return createDeptByName(unitName, registerBody, username);
        }
        return null;
    }

    private SysDept findDeptByName(String deptName)
    {
        SysDept query = new SysDept();
        query.setStatus("0");
        query.setDeptName(deptName);
        List<SysDept> depts = deptService.selectDeptList(query);
        if (StringUtils.isNotEmpty(depts))
        {
            for (SysDept dept : depts)
            {
                if (deptName.equals(dept.getDeptName()))
                {
                    return dept;
                }
            }
        }
        return null;
    }

    private Long createDeptByName(String deptName, RegisterBody registerBody, String username)
    {
        Long parentDeptId = resolveParentDeptId(registerBody.getParentDeptId());
        if (parentDeptId == null)
        {
            return null;
        }
        SysDept dept = new SysDept();
        dept.setParentId(parentDeptId);
        dept.setDeptName(deptName);
        dept.setOrderNum(99);
        dept.setLeader(StringUtils.defaultIfEmpty(registerBody.getNickName(), username));
        dept.setPhone(registerBody.getPhonenumber());
        dept.setEmail(registerBody.getEmail());
        dept.setStatus("0");
        dept.setUnitType("被审计单位");
        dept.setIsAuditTarget(1);
        dept.setCreateBy("register");
        deptService.insertDept(dept);
        return dept.getDeptId();
    }

    private Long resolveParentDeptId(Long parentDeptId)
    {
        if (parentDeptId != null && deptService.selectDeptById(parentDeptId) != null)
        {
            return parentDeptId;
        }
        if (deptService.selectDeptById(2100L) != null)
        {
            return 2100L;
        }
        SysDept query = new SysDept();
        query.setStatus("0");
        List<SysDept> depts = deptService.selectDeptList(query);
        if (StringUtils.isNotEmpty(depts))
        {
            return depts.get(0).getDeptId();
        }
        return null;
    }

    private String normalizeOrgName(String orgName, String orgType)
    {
        String name = StringUtils.trim(orgName);
        if (StringUtils.isEmpty(name))
        {
            return "";
        }
        if ("company".equals(orgType) && !name.endsWith("公司") && !name.endsWith("企业"))
        {
            return name + "公司";
        }
        if ("college".equals(orgType) && !name.endsWith("学院") && !name.endsWith("系"))
        {
            return name + "学院";
        }
        if ("school".equals(orgType) && !name.endsWith("大学") && !name.endsWith("学院") && !name.endsWith("学校"))
        {
            return name + "学校";
        }
        return name;
    }

    private Long resolveDefaultRegisterRoleId()
    {
        SysRole role = roleMapper.checkRoleKeyUnique("audited_unit_liaison");
        if (role != null)
        {
            return role.getRoleId();
        }
        return 105L;
    }

    /**
     * 校验验证码
     */
    public void validateCaptcha(String username, String code, String uuid)
    {
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
        String captcha = redisCache.getCacheObject(verifyKey);
        redisCache.deleteObject(verifyKey);
        if (captcha == null)
        {
            throw new CaptchaExpireException();
        }
        if (!code.equalsIgnoreCase(captcha))
        {
            throw new CaptchaException();
        }
    }
}
