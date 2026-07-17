package com.ruoyi.web.controller.system;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.RegisterBody;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.SysRegisterService;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 注册验证
 * 
 * @author ruoyi
 */
@RestController
public class SysRegisterController extends BaseController
{
    @Autowired
    private SysRegisterService registerService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/register")
    public AjaxResult register(@RequestBody RegisterBody user)
    {
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser"))))
        {
            return error("当前系统没有开启注册功能！");
        }
        String msg = registerService.register(user);
        return StringUtils.isEmpty(msg) ? success() : error(msg);
    }

    @GetMapping("/register/deptTree")
    public AjaxResult deptTree()
    {
        List<Map<String, Object>> tree = new ArrayList<>();
        tree.add(group("组织/被审计单位", deptOptions()));
        return success(tree);
    }

    private Map<String, Object> group(String label, List<Map<String, Object>> children)
    {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", "group:" + label);
        item.put("label", label);
        item.put("disabled", true);
        item.put("children", children);
        return item;
    }

    private List<Map<String, Object>> deptOptions()
    {
        List<Map<String, Object>> depts = jdbcTemplate.queryForList(
                "SELECT dept_id, dept_name FROM sys_dept WHERE status='0' AND del_flag='0' ORDER BY parent_id, order_num, dept_id");
        List<Map<String, Object>> options = new ArrayList<>();
        for (Map<String, Object> item : depts)
        {
            Number deptId = (Number) item.get("dept_id");
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("id", "dept:" + deptId.longValue());
            option.put("label", item.get("dept_name"));
            option.put("source", "sys_dept");
            option.put("deptId", deptId.longValue());
            options.add(option);
        }
        return options;
    }
}
