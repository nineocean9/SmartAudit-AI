using DocumentFormat.OpenXml;
using DocumentFormat.OpenXml.Packaging;
using DocumentFormat.OpenXml.Wordprocessing;
using WpPageSize = DocumentFormat.OpenXml.Wordprocessing.PageSize;

record Project(int Id, string Name, string Unit, string Type, int Year, string Start, string End, string Leader, decimal BaseAmount);
record Material(int ProjectId, int No, string Title, string Category, string Submitter);

static class Program
{
    const string Navy = "17365D";
    const string Blue = "2F75B5";
    const string LightBlue = "D9EAF7";
    const string LightGray = "F2F2F2";
    const string Border = "A6A6A6";

    static readonly Project[] Projects =
    [
        new(1, "信息工程学院2024年经济责任审计", "信息工程学院", "经济责任审计", 2024, "2024年3月1日", "2024年6月30日", "王建国（审计项目负责人）", 2864.80m),
        new(2, "商学院2023年财务收支审计", "商学院", "财务收支审计", 2023, "2023年9月1日", "2023年12月20日", "李敏（审计项目负责人）", 1732.46m),
        new(3, "后勤处2024年专项审计", "后勤处", "专项审计", 2024, "2024年5月10日", "2024年9月30日", "赵磊（审计项目负责人）", 4658.32m),
        new(4, "财务处2026年预算执行审计", "财务处", "预算执行审计", 2026, "2026年2月20日", "2026年6月30日", "陈静（审计项目负责人）", 12860.00m),
        new(5, "图书馆2026年工程审计", "图书馆", "工程审计", 2026, "2026年4月1日", "2026年8月31日", "周海峰（审计项目负责人）", 1268.40m),
        new(6, "信息工程学院2026年预算执行审计", "信息工程学院", "预算执行审计", 2026, "2026年5月20日", "2026年10月31日", "孙文（审计项目负责人）", 3186.75m),
        new(7, "科研经费2026年专项审计", "科学技术研究院", "专项审计", 2026, "2026年9月1日", "2026年12月15日", "刘婷（审计项目负责人）", 5260.00m),
        new(8, "资产经营公司2026年度财务收支审计", "资产经营公司", "财务收支审计", 2026, "2026年10月8日", "2026年12月31日", "郭强（审计项目负责人）", 9850.60m),
        new(9101, "A公司2026年财务收支审计", "A公司", "财务收支审计", 2026, "2026年6月1日", "2026年9月30日", "何佳（审计项目负责人）", 7342.25m)
    ];

    static readonly Material[] Materials = BuildMaterials();

    static void Main(string[] args)
    {
        if (args.Length < 2)
            throw new ArgumentException("Usage: DemoDocxGenerator <profile-root> <static-template-root>");

        var profileRoot = Path.GetFullPath(args[0]);
        var templateRoot = Path.GetFullPath(args[1]);

        foreach (var material in Materials)
        {
            var project = Projects.Single(p => p.Id == material.ProjectId);
            var path = Path.Combine(profileRoot, "submitted", project.Id.ToString(), $"{material.No}.docx");
            CreateMaterial(path, project, material);
        }

        foreach (var project in Projects)
        {
            var dir = Path.Combine(profileRoot, "demo", project.Id.ToString());
            CreatePlan(Path.Combine(dir, "audit-plan.docx"), project);
            CreateReport(Path.Combine(dir, "audit-report.docx"), project);
            if (project.Id is 7 or 8)
                CreateAnnualProjectList(Path.Combine(dir, "annual-project-list.docx"), project);
            if (project.Id == 8)
                CreateAnnualPlan(Path.Combine(dir, "annual-audit-plan.docx"), project);
        }

        CreatePlan(Path.Combine(templateRoot, "economic-responsibility-audit-plan-template.docx"),
            new Project(0, "示例单位2026年经济责任审计", "被审计单位", "经济责任审计", 2026, "2026年3月1日", "2026年6月30日", "审计组组长", 3200m));
        CreatePlan(Path.Combine(templateRoot, "financial-revenue-expenditure-audit-plan-template.docx"),
            new Project(0, "示例单位2026年财务收支审计", "被审计单位", "财务收支审计", 2026, "2026年4月1日", "2026年7月31日", "审计组组长", 6800m));
        CreatePlan(Path.Combine(templateRoot, "engineering-audit-plan-template.docx"),
            new Project(0, "示例建设项目2026年工程审计", "建设单位", "工程审计", 2026, "2026年4月1日", "2026年8月31日", "审计组组长", 1500m));
        CreatePlan(Path.Combine(templateRoot, "special-audit-plan-template.docx"),
            new Project(0, "示例专项资金2026年专项审计", "被审计单位", "专项审计", 2026, "2026年5月1日", "2026年9月30日", "审计组组长", 4200m));
        CreateReport(Path.Combine(templateRoot, "audit-report-draft-template.docx"),
            new Project(0, "示例单位2026年财务收支审计", "被审计单位", "财务收支审计", 2026, "2026年4月1日", "2026年7月31日", "审计组组长", 6800m));

        Console.WriteLine($"Generated {Materials.Length + Projects.Length * 2 + 8} realistic DOCX files.");
    }

    static Material[] BuildMaterials()
    {
        var list = new List<Material>();
        var standard = new[]
        {
            (1, "年度预算批复及调整资料", "预算资料"), (2, "会计凭证及明细账", "财务资料"),
            (3, "采购合同及招投标资料", "合同采购"), (4, "固定资产台账", "资产资料"),
            (5, "内部控制制度汇编", "制度资料"), (6, "整改落实支撑材料", "整改资料")
        };
        var submitters = new Dictionary<int, string>
        {
            [1] = "张立新（信息工程学院联络员）", [2] = "吴倩（商学院联络员）", [3] = "郑伟（后勤处联络员）",
            [4] = "徐燕（财务处联络员）", [5] = "林晓（图书馆联络员）", [6] = "张立新（信息工程学院联络员）"
        };
        foreach (var pid in Enumerable.Range(1, 4))
            foreach (var item in standard)
                list.Add(new(pid, item.Item1, item.Item2, item.Item3, submitters[pid]));

        list.AddRange([
            new(5, 1, "工程立项及批复文件", "工程资料", submitters[5]),
            new(5, 2, "施工合同及补充协议", "合同资料", submitters[5]),
            new(5, 3, "工程量清单与签证资料", "造价资料", submitters[5]),
            new(5, 4, "竣工验收资料", "验收资料", submitters[5]),
            new(5, 5, "付款凭证及发票", "财务资料", submitters[5]),
            new(6, 1, "年度预算批复及执行明细", "预算资料", submitters[6]),
            new(6, 2, "科研经费收支明细", "财务资料", submitters[6]),
            new(6, 3, "采购合同与验收单", "合同采购", submitters[6]),
            new(6, 4, "资产购置及领用台账", "资产资料", submitters[6])
        ]);
        return list.ToArray();
    }

    static void CreateMaterial(string path, Project p, Material m)
    {
        CreateDocument(path, m.Title, $"{p.Name} · {m.Category}", p.Unit, body =>
        {
            body.Append(InfoTable([
                ["资料编号", $"{p.Year}-{p.Id:D2}-{m.No:D2}"], ["所属项目", p.Name],
                ["资料期间", $"{p.Year}年1月1日至{p.Year}年12月31日"], ["提交单位", p.Unit],
                ["提交人", m.Submitter], ["编制日期", $"{p.Year}年{Math.Min(11, m.No + 2)}月{10 + m.No}日"]
            ]));
            body.Append(H("一、资料说明", 1));
            body.Append(P($"本资料由{p.Unit}按照《{p.Name}资料清单》整理报送，所列数据与财务账簿、审批记录及业务台账保持一致。纸质原件由综合办公室归档，电子件用于本次审计核验。"));
            AppendMaterialDetails(body, p, m);
            body.Append(H("三、真实性与完整性声明", 1));
            body.Append(P($"经复核，本资料共收录{6 + m.No}项记录，相关审批手续、合同附件和财务凭证能够相互印证。对审计过程中需要延伸核查的事项，{p.Unit}将按要求补充原始资料。"));
            body.Append(SignatureTable(m.Submitter, p.Unit));
        });
    }

    static void AppendMaterialDetails(Body body, Project p, Material m)
    {
        var b = p.BaseAmount;
        body.Append(H("二、主要内容", 1));
        if (m.Title.Contains("预算"))
        {
            body.Append(P($"{p.Year}年度批复预算总额为{b:N2}万元，年中依据重点任务进度履行两次预算调整程序，调整后预算为{b * 1.03m:N2}万元。下表列示主要科目执行情况。"));
            body.Append(DataTable(["预算科目", "年初预算（万元）", "调整预算（万元）", "实际执行（万元）", "执行率"], [
                ["人员及劳务支出", M(b*.31m), M(b*.32m), M(b*.306m), "95.6%"],
                ["业务运行经费", M(b*.27m), M(b*.28m), M(b*.261m), "93.2%"],
                ["设备及软件购置", M(b*.24m), M(b*.25m), M(b*.226m), "90.4%"],
                ["专项建设经费", M(b*.18m), M(b*.18m), M(b*.171m), "95.0%"],
                ["合计", M(b), M(b*1.03m), M(b*.964m), "93.6%"]
            ]));
            body.Append(P("预算调整均经单位党政联席会议审议，并通过财务系统完成指标调剂。未发现无预算、超预算列支情况；设备购置科目执行偏慢，主要因集中采购交付周期延长。"));
        }
        else if (m.Title.Contains("凭证") || m.Title.Contains("经费"))
        {
            body.Append(P($"抽取{p.Year}年度金额较大、摘要异常及月末集中入账凭证进行汇总，覆盖收入、采购、劳务和往来结算等业务。抽查金额{b*.42m:N2}万元，占年度资金发生额的42.0%。"));
            body.Append(DataTable(["凭证号", "日期", "摘要", "借方科目", "金额（元）", "复核结果"], [
                [$"记-{p.Year}-0218", $"{p.Year}-02-18", "设备采购首付款", "固定资产", "386,500.00", "合同及验收资料齐全"],
                [$"记-{p.Year}-0426", $"{p.Year}-04-26", "业务系统服务费", "业务活动费用", "128,000.00", "审批链完整"],
                [$"记-{p.Year}-0712", $"{p.Year}-07-12", "科研项目测试费", "科研支出", "96,800.00", "成果附件已核验"],
                [$"记-{p.Year}-1019", $"{p.Year}-10-19", "差旅费集中报销", "差旅费", "42,736.50", "标准符合规定"]
            ]));
        }
        else if (m.Title.Contains("合同") || m.Title.Contains("招投标"))
        {
            body.Append(P($"本期纳入审计范围的合同共{8 + m.No}份，合同总额{b*.36m:N2}万元。采购事项均履行需求论证、预算审核和采购方式审批，重大合同经法律顾问审查。"));
            body.Append(DataTable(["合同编号", "合同名称", "供应商", "合同金额（万元）", "签订日期", "履行状态"], [
                [$"HT-{p.Year}-017", "信息化设备采购合同", "华辰科技有限公司", "386.50", $"{p.Year}-03-18", "已验收"],
                [$"HT-{p.Year}-026", "业务系统运维服务合同", "启明数字技术有限公司", "128.00", $"{p.Year}-05-06", "履行中"],
                [$"HT-{p.Year}-041", "空间改造配套采购合同", "恒远建设有限公司", "214.80", $"{p.Year}-07-22", "已验收"],
                [$"HT-{p.Year}-053", "专业数据库服务合同", "博知信息服务有限公司", "76.20", $"{p.Year}-09-15", "履行中"]
            ]));
        }
        else if (m.Title.Contains("资产"))
        {
            body.Append(P($"截至{p.Year}年12月31日，{p.Unit}资产账面原值{b*.68m:N2}万元。本次盘点覆盖重点设备、通用设备和家具用具，账、卡、物核对一致率为99.2%。"));
            body.Append(DataTable(["资产编号", "资产名称", "规格型号", "原值（元）", "存放地点", "使用状态"], [
                [$"ZC-{p.Year}-00186", "计算服务器", "SR650 V3", "286,000.00", "中心机房A区", "在用"],
                [$"ZC-{p.Year}-00231", "业务存储阵列", "ME5024", "418,600.00", "中心机房B区", "在用"],
                [$"ZC-{p.Year}-00308", "多媒体交互终端", "86英寸", "58,900.00", "报告厅", "在用"],
                [$"ZC-{p.Year}-00417", "办公家具一批", "定制", "96,450.00", "综合办公区", "在用"]
            ]));
        }
        else if (m.Title.Contains("制度"))
        {
            body.Append(P($"制度汇编覆盖预算、收支、采购、合同、资产、项目和信息系统管理七个方面。本年度修订制度4项，新制定操作规程2项，均经{p.Unit}党政联席会议审议通过。"));
            body.Append(DataTable(["制度名称", "文号", "施行日期", "责任部门", "本年变化"], [
                ["预算与绩效管理办法", $"{p.Unit}发〔{p.Year}〕3号", $"{p.Year}-01-15", "综合办公室", "修订"],
                ["采购与合同管理实施细则", $"{p.Unit}发〔{p.Year}〕6号", $"{p.Year}-03-01", "资产管理岗", "修订"],
                ["重大事项集体决策规则", $"{p.Unit}党〔{p.Year}〕2号", $"{p.Year}-02-20", "党政办公室", "继续执行"],
                ["项目验收操作规程", $"{p.Unit}办〔{p.Year}〕9号", $"{p.Year}-06-12", "项目管理岗", "新制定"]
            ]));
        }
        else if (m.Title.Contains("整改"))
        {
            body.Append(P("针对上次内部检查提出的预算执行、资产标签和合同归档问题，责任部门已逐项制定措施并完成复核。整改台账实行问题、措施、责任、时限和证据“五对应”。"));
            body.Append(DataTable(["问题编号", "问题摘要", "整改措施", "责任人", "完成时限", "状态"], [
                ["ZG-01", "预算绩效指标不够量化", "补充产出及效益指标并纳入月度跟踪", "财务负责人", $"{p.Year}-04-30", "已完成"],
                ["ZG-02", "个别资产标签信息滞后", "完成盘点并同步更新资产系统", "资产管理员", $"{p.Year}-05-20", "已完成"],
                ["ZG-03", "合同附件分散保管", "建立一合同一档电子目录", "综合办公室", $"{p.Year}-06-15", "已完成"]
            ]));
        }
        else if (m.Title.Contains("立项"))
        {
            body.Append(P("图书馆智慧学习空间改造项目于2025年11月完成需求论证，2026年1月经校长办公会审议立项。批复建设内容包括空间改造、消防及强弱电更新、智能门禁和学习终端部署。"));
            body.Append(DataTable(["批复事项", "文件编号", "批复日期", "批复金额（万元）", "批复单位"], [
                ["项目建议书", "校发改〔2026〕8号", "2026-01-12", "1,268.40", "学校发展规划处"],
                ["初步设计及概算", "校基建〔2026〕14号", "2026-02-06", "1,241.75", "学校基建工作领导小组"],
                ["采购实施方案", "校采购〔2026〕21号", "2026-02-25", "1,198.60", "学校采购管理委员会"]
            ]));
        }
        else if (m.Title.Contains("工程量") || m.Title.Contains("签证"))
        {
            body.Append(P("送审工程量清单共计286个清单项目。施工期间形成设计变更7项、现场签证11项，累计申报增加造价46.82万元，经监理和建设单位审核后暂定增加38.64万元。"));
            body.Append(DataTable(["编号", "变更/签证内容", "申报金额（元）", "审核金额（元）", "核减原因"], [
                ["BG-03", "二层阅读区强电回路调整", "86,420.00", "72,180.00", "部分工程量重复计取"],
                ["QZ-05", "原墙体基层加固处理", "128,600.00", "118,350.00", "材料价按合同口径调整"],
                ["BG-06", "消防联动模块增补", "96,800.00", "91,240.00", "综合单价修正"],
                ["QZ-09", "夜间施工保护措施", "44,900.00", "31,760.00", "措施费计取依据不足"]
            ]));
        }
        else if (m.Title.Contains("验收"))
        {
            body.Append(P("项目于2026年7月8日组织竣工验收，建设、设计、监理、施工及使用单位参加。工程质量评定为合格，消防联动、照明、网络和门禁系统测试结果符合设计要求。"));
            body.Append(DataTable(["验收专业", "验收结论", "遗留事项", "责任单位", "完成日期"], [
                ["装饰装修", "合格", "局部收口修整", "恒远建设有限公司", "2026-07-15"],
                ["电气及照明", "合格", "无", "恒远建设有限公司", "2026-07-08"],
                ["消防联动", "合格", "补充测试记录签章", "安信消防工程有限公司", "2026-07-12"],
                ["智能化系统", "合格", "完善管理员培训记录", "启明数字技术有限公司", "2026-07-18"]
            ]));
        }
    }

    static void CreatePlan(string path, Project p)
    {
        CreateDocument(path, p.Name.Replace("2024年", $"{p.Year}年").Replace("2023年", $"{p.Year}年") + "实施方案", $"项目编号：AUD-{p.Year}-{(p.Id == 0 ? 1 : p.Id):D4}", "审计处", body =>
        {
            body.Append(InfoTable([["被审计单位", p.Unit], ["审计类型", p.Type], ["实施期间", $"{p.Start}至{p.End}"], ["项目负责人", p.Leader], ["审计期间", $"{p.Year - 1}年1月1日至{p.Year}年12月31日"], ["方案审批", "审计处负责人批准后执行"]]));
            body.Append(H("一、项目背景与基本情况", 1));
            body.Append(P($"根据学校{p.Year}年度审计项目计划，审计处决定对{p.Unit}开展{p.Type}。{p.Unit}本期纳入审计范围的资金及资产规模约{p.BaseAmount:N2}万元，业务涉及预算执行、财务收支、采购合同、资产管理和内部控制等事项。"));
            body.Append(P($"本项目现场审计自{p.Start}开始，计划于{p.End}前完成报告出具和结果反馈。必要时对重大事项以前年度及关联单位进行延伸核查。"));
            body.Append(H("二、审计目标", 1));
            body.Append(Numbered("核实财务收支和业务数据的真实性、完整性，确认重大事项账实相符。", "评价预算管理、决策审批和内部控制制度设计及执行的有效性。", "揭示资金、资产、工程或经营活动中的突出风险，明确问题责任和影响金额。", "提出可执行的整改建议，推动被审计单位完善长效机制。"));
            body.Append(H("三、审计依据", 1));
            body.Append(Numbered("《中华人民共和国审计法》及其实施条例。", "《审计署关于内部审计工作的规定》《教育系统内部审计工作规定》。", "政府会计准则制度、学校预算管理办法、采购与合同管理办法及资产管理制度。", $"学校{p.Year}年度审计项目计划、审计通知书及经批准的项目任务书。"));
            body.Append(H("四、审计范围与重点", 1));
            body.Append(DataTable(["审计领域", "主要风险", "拟实施程序", "责任人"], PlanRows(p)));
            body.Append(H("五、人员分工", 1));
            body.Append(P("人员分工由审计项目负责人依据审计人员专业能力确定，成员均为系统内已授权的审计人员；被审计单位联络员只负责资料报送和沟通，不参与审计结论形成。"));
            body.Append(DataTable(["角色", "姓名及岗位", "工作职责", "成果要求"], [
                ["项目负责人", p.Leader, "统筹实施、重大事项判断、复核底稿和沟通汇报", "总体方案、审计报告"],
                ["财务审计", "陈静（高级审计师）", "预算、收支、凭证和往来款核查", "财务审计底稿"],
                ["业务审计", "刘婷（审计师）", "采购、合同、项目执行和绩效核查", "业务审计底稿"],
                ["数据分析", "孙文（数据审计岗）", "采集数据、规则筛查和异常样本核验", "数据分析记录"],
                ["质量复核", "郭强（审计处副处长）", "实施方案、重大问题和报告三级复核", "复核意见单"]
            ]));
            body.Append(H("六、实施进度", 1));
            body.Append(DataTable(["阶段", "时间安排", "主要任务", "交付物"], [
                ["审前准备", p.Start, "送达通知、收集资料、访谈和风险评估", "资料清单、访谈记录"],
                ["现场实施", "第1至第4周", "数据分析、抽样核查、盘点及外部印证", "审计工作底稿"],
                ["问题确认", "第5周", "事实核对、法规复核、征求被审计单位意见", "问题确认单"],
                ["报告阶段", p.End, "形成报告、三级复核并提交审定", "审计报告及整改清单"]
            ]));
            body.Append(H("七、抽样与重要性水平", 1));
            body.Append(P($"以{p.BaseAmount:N2}万元为总体规模，初步重要性水平按总体金额的0.5%确定，即{p.BaseAmount*.005m:N2}万元。对单笔超过重要性水平、管理层交易、异常供应商、年末集中入账及制度明确要求全查的事项实施100%检查；其余事项采用金额单位抽样与判断抽样相结合。"));
            body.Append(H("八、质量控制与廉政纪律", 1));
            body.Append(Numbered("严格执行审计工作底稿编制、交叉复核和项目负责人复核制度。", "重大问题实行事实、证据、定性、法规依据和责任主体“五要素”审核。", "审计人员不得接受宴请、礼品或影响独立性的安排，涉及利害关系的主动申请回避。", "电子资料在项目权限范围内使用，未经批准不得对外复制或传播。"));
            body.Append(H("九、审批意见", 1));
            body.Append(ApprovalTable(p));
        });
    }

    static string[][] PlanRows(Project p)
    {
        var common = new List<string[]>
        {
            new[] { "预算及收支", "预算调整依据不足、收入未及时入账、支出审批不完整", "核对预算指标、总账明细账和大额凭证，实施截止性测试", "陈静" },
            new[] { "采购与合同", "采购方式规避、合同条款不完整、验收付款脱节", "比对采购计划、评审记录、合同、验收单和付款信息", "刘婷" },
            new[] { "资产管理", "账卡物不一致、资产闲置或处置程序缺失", "抽盘重点资产，核对资产卡片、使用人和存放地点", "孙文" },
            new[] { "内部控制", "岗位不相容、授权边界不清、关键控制未留痕", "穿行测试关键流程，检查制度执行证据和系统权限", "项目负责人" }
        };
        if (p.Type.Contains("工程")) common.Insert(0, new[] { "工程建设", "变更签证失控、工程量高估、结算资料不完整", "复核立项招标合同、现场踏勘并重算重点工程量", "刘婷" });
        if (p.Type.Contains("经济责任")) common.Insert(0, new[] { "重大决策", "“三重一大”事项决策程序不完整、个人决策", "查阅会议纪要、请示批复并访谈班子成员", "项目负责人" });
        if (p.Type.Contains("专项")) common.Insert(0, new[] { "专项政策与绩效", "资金投向偏离、项目进度滞后、绩效指标未达成", "核对项目库、资金流向和成果验收，开展绩效评价", "刘婷" });
        return common.ToArray();
    }

    static void CreateReport(string path, Project p)
    {
        var issue = Math.Max(12.6m, p.BaseAmount * .0068m);
        CreateDocument(path, p.Name + "报告", $"报告编号：审报〔{p.Year}〕{(p.Id == 0 ? 1 : p.Id % 100):D2}号", "审计处", body =>
        {
            body.Append(InfoTable([["被审计单位", p.Unit], ["审计类型", p.Type], ["审计期间", $"{p.Year - 1}年1月1日至{p.Year}年12月31日"], ["现场实施", $"{p.Start}至{p.End}"], ["报告版本", "处内审核稿"], ["项目负责人", p.Leader]]));
            body.Append(H("一、审计实施情况", 1));
            body.Append(P($"根据学校年度审计计划，审计组于{p.Start}至{p.End}对{p.Unit}开展了{p.Type}。审计采取数据分析、抽样检查、访谈询证和现场盘点等方式，重点检查预算执行、财务收支、采购合同、资产管理及内部控制。{p.Unit}对其提供资料的真实性和完整性负责，审计处依法独立实施审计并出具本报告。"));
            body.Append(H("二、基本情况", 1));
            body.Append(P($"审计期间，{p.Unit}纳入审计范围的资金及资产规模为{p.BaseAmount:N2}万元；年度收入{p.BaseAmount*.73m:N2}万元，支出{p.BaseAmount*.69m:N2}万元，年末资产账面价值{p.BaseAmount*.58m:N2}万元。审计抽查凭证、合同和项目支出合计{p.BaseAmount*.47m:N2}万元，资金覆盖率47.0%。"));
            body.Append(DataTable(["指标", $"{p.Year - 1}年度（万元）", $"{p.Year}年度（万元）", "变动率", "说明"], [
                ["收入", M(p.BaseAmount*.66m), M(p.BaseAmount*.73m), "10.6%", "专项拨款及服务收入增加"],
                ["支出", M(p.BaseAmount*.62m), M(p.BaseAmount*.69m), "11.3%", "项目建设和设备购置增加"],
                ["资产", M(p.BaseAmount*.53m), M(p.BaseAmount*.58m), "9.4%", "新增设备及在建项目转固"],
                ["合同金额", M(p.BaseAmount*.28m), M(p.BaseAmount*.33m), "17.9%", "重点项目集中采购"]
            ]));
            body.Append(H("三、审计评价", 1));
            body.Append(P($"审计结果表明，{p.Unit}能够执行学校财经制度，重大支出基本履行集体决策和审批程序，会计核算总体规范，资产和合同管理基础较为完整。但在预算绩效约束、合同归档和资产信息维护方面仍存在薄弱环节，需要进一步落实业务部门主体责任和财务监督责任。"));
            body.Append(H("四、审计发现的主要问题", 1));
            AppendFinding(body, "（一）部分项目预算绩效指标设置不够量化", $"抽查的12个项目中，有3个项目仅设置“完成建设任务”等定性指标，涉及预算{issue*3.2m:N2}万元，难以客观衡量成本、时效和使用效益。", "依据学校预算绩效管理办法关于绩效指标应当细化量化的规定，建议补充数量、质量、时效和效益指标，并与预算执行同步监控。", "综合办公室", $"{p.Year}年9月30日");
            AppendFinding(body, "（二）个别合同归档资料不完整", $"2份已履行合同未将需求论证记录和验收附件同步归入合同档案，合同金额合计{issue*1.7m:N2}万元。经审计指出后，相关原始资料已补充。", "依据学校合同管理办法关于“一合同一档”的要求，建议在合同系统设置归档完整性检查，未完成归档的合同不得办理尾款。", "合同管理员", $"{p.Year}年8月31日");
            AppendFinding(body, "（三）资产信息更新不及时", $"现场抽盘发现4台设备已调整存放地点，但资产系统仍登记原地点，资产原值{issue*.9m:N2}万元。实物在用且保管责任明确，未形成资产损失。", "建议建立资产变动月度核对机制，使用部门在调拨后5个工作日内办理系统变更，资产管理员按季抽盘。", "资产管理员", $"{p.Year}年8月15日");
            body.Append(H("五、审计处理意见和建议", 1));
            body.Append(Numbered("完善预算绩效目标审核机制，将绩效目标质量纳入项目立项和预算安排条件。", "统一采购、合同、验收和付款资料目录，实现业务系统与财务凭证双向关联。", "开展年度资产全面盘点，对存放地点、使用人和资产状态实行动态维护。", "对本报告指出的问题建立整改台账，明确责任人、完成时限和佐证材料。"));
            body.Append(H("六、整改要求", 1));
            body.Append(P($"请{p.Unit}自收到本报告之日起30日内制定整改方案，90日内向审计处报送整改结果和支撑材料。对受客观条件限制未能按期完成的事项，应说明原因、阶段性进展和后续计划。审计处将适时组织整改复核。"));
            body.Append(H("七、被审计单位意见", 1));
            body.Append(P($"{p.Unit}对报告反映的事实和金额无异议，同意审计提出的整改要求，并已明确综合办公室、财务管理岗和资产管理岗分别牵头整改。"));
            body.Append(H("附件：问题整改清单", 1));
            body.Append(DataTable(["序号", "问题", "涉及金额（万元）", "责任部门", "完成时限", "整改状态"], [
                ["1", "预算绩效指标不够量化", M(issue*3.2m), "综合办公室", $"{p.Year}-09-30", "整改中"],
                ["2", "合同归档资料不完整", M(issue*1.7m), "采购与合同岗", $"{p.Year}-08-31", "已补充"],
                ["3", "资产信息更新不及时", M(issue*.9m), "资产管理岗", $"{p.Year}-08-15", "整改中"]
            ]));
        });
    }

    static void AppendFinding(Body body, string heading, string fact, string recommendation, string owner, string due)
    {
        body.Append(H(heading, 2));
        body.Append(P("问题事实：" + fact));
        body.Append(P("审计意见：" + recommendation));
        body.Append(P($"整改责任：{owner}；整改期限：{due}。"));
    }

    static void CreateAnnualProjectList(string path, Project p)
    {
        CreateDocument(path, "2026年度审计项目计划清单", "经学校审计委员会审议通过", "审计处", body =>
        {
            body.Append(P("本清单根据学校年度重点工作、风险评估结果和审计资源安排编制，项目实施时间可根据学校重大任务统筹调整。"));
            body.Append(DataTable(["序号", "项目名称", "被审计单位", "类型", "实施时间", "项目负责人", "重点内容"], [
                ["1", "财务处2026年预算执行审计", "财务处", "预算执行", "2月至6月", "陈静", "预算编制、执行和绩效"],
                ["2", "图书馆2026年工程审计", "图书馆", "工程审计", "4月至8月", "周海峰", "招投标、变更签证和结算"],
                ["3", "信息工程学院2026年预算执行审计", "信息工程学院", "预算执行", "5月至10月", "孙文", "科研经费、采购和资产"],
                ["4", p.Name, p.Unit, p.Type, p.Start.Replace("2026年", "") + "起", p.Leader.Split('（')[0], "资金使用、项目绩效和内控"],
                ["5", "A公司2026年财务收支审计", "A公司", "财务收支", "6月至9月", "何佳", "收入成本、往来和经营绩效"]
            ]));
            body.Append(H("项目安排说明", 1));
            body.Append(Numbered("年度项目实行项目负责人制，审计组成员由审计处统一调配。", "项目实施前完成审前调查和风险评估，形成经审批的实施方案。", "审计结果按规定向学校审计委员会报告，并纳入整改跟踪范围。"));
        });
    }

    static void CreateAnnualPlan(string path, Project p)
    {
        CreateDocument(path, "某高校2026年度内部审计工作计划", "校审委办〔2026〕2号", "审计委员会办公室", body =>
        {
            body.Append(P("2026年内部审计工作坚持风险导向和问题导向，围绕学校年度重点任务，统筹开展预算执行、经济责任、建设工程、科研经费和校办企业审计，强化整改闭环与成果运用。"));
            body.Append(H("一、年度工作目标", 1));
            body.Append(Numbered("完成9个计划内审计项目和2项专项审计调查。", "推动以前年度审计发现问题按期整改率达到95%以上。", "完善数据审计规则库，实现重点财务数据持续监测。", "规范审计项目质量控制，落实分级复核和审理制度。"));
            body.Append(H("二、重点审计任务", 1));
            body.Append(DataTable(["任务类别", "项目数量", "重点对象", "计划时间", "预期成果"], [
                ["预算执行审计", "2", "财务处、信息工程学院", "2月至10月", "评价预算绩效与财经纪律执行"],
                ["财务收支审计", "2", "资产经营公司、A公司", "6月至12月", "揭示经营和资金风险"],
                ["建设工程审计", "1", "图书馆改造项目", "4月至8月", "控制投资和工程结算"],
                ["专项审计", "2", "科研经费、后勤专项资金", "5月至12月", "检查政策落实和项目绩效"],
                ["整改复核", "2", "以前年度被审计单位", "全年", "核验整改真实性和长效机制"]
            ]));
            body.Append(H("三、保障措施", 1));
            body.Append(Numbered("加强审计委员会对重大事项、重要报告和整改工作的统筹领导。", "优化项目资源配置，推进财务、工程、信息技术等专业协同。", "严格审计纪律、保密纪律和数据权限管理。", "按季度通报计划进展，对延期项目说明原因并履行调整审批。"));
            body.Append(P($"附件：2026年度审计项目计划清单。重点项目包括：{p.Name}。"));
        });
    }

    static void CreateDocument(string path, string title, string subtitle, string issuer, Action<Body> write)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(path)!);
        if (File.Exists(path)) File.Delete(path);
        using var doc = WordprocessingDocument.Create(path, WordprocessingDocumentType.Document);
        var main = doc.AddMainDocumentPart();
        main.Document = new Document(new Body());
        AddStyles(main);
        var body = main.Document.Body!;
        body.Append(Title(title));
        body.Append(Subtitle(subtitle));
        body.Append(Centered($"编制单位：{issuer}    编制日期：2026年7月"));
        body.Append(new Paragraph(new Run(new Break { Type = BreakValues.Page })));
        write(body);
        var sect = new SectionProperties(
            new WpPageSize { Width = 11906U, Height = 16838U },
            new PageMargin { Top = 1440, Bottom = 1440, Left = 1580U, Right = 1440U, Header = 720U, Footer = 720U });
        AddHeader(main, sect, title);
        AddFooter(main, sect);
        body.Append(sect);
        main.Document.Save();
    }

    static void AddStyles(MainDocumentPart main)
    {
        var part = main.AddNewPart<StyleDefinitionsPart>();
        var styles = new Styles(new DocDefaults(
            new RunPropertiesDefault(new RunPropertiesBaseStyle(new RunFonts { Ascii = "Aptos", HighAnsi = "Aptos", EastAsia = "Microsoft YaHei" }, new FontSize { Val = "22" }, new Languages { Val = "zh-CN", EastAsia = "zh-CN" })),
            new ParagraphPropertiesDefault(new ParagraphPropertiesBaseStyle(new SpacingBetweenLines { Line = "276", LineRule = LineSpacingRuleValues.Auto, After = "120" }))));
        styles.Append(new Style(new StyleName { Val = "正文" }, new PrimaryStyle(), new StyleParagraphProperties(new SpacingBetweenLines { Line = "276", LineRule = LineSpacingRuleValues.Auto, After = "120" }), new StyleRunProperties(new RunFonts { EastAsia = "SimSun" }, new FontSize { Val = "22" }, new Color { Val = "222222" })) { Type = StyleValues.Paragraph, StyleId = "Normal", Default = true });
        styles.Append(new Style(new StyleName { Val = "标题" }, new BasedOn { Val = "Normal" }, new StyleParagraphProperties(new Justification { Val = JustificationValues.Center }, new SpacingBetweenLines { Before = "200", After = "260" }, new KeepNext()), new StyleRunProperties(new RunFonts { EastAsia = "Microsoft YaHei" }, new Bold(), new FontSize { Val = "38" }, new Color { Val = Navy })) { Type = StyleValues.Paragraph, StyleId = "Title" });
        styles.Append(new Style(new StyleName { Val = "副标题" }, new BasedOn { Val = "Normal" }, new StyleParagraphProperties(new Justification { Val = JustificationValues.Center }, new SpacingBetweenLines { After = "160" }), new StyleRunProperties(new RunFonts { EastAsia = "Microsoft YaHei" }, new FontSize { Val = "24" }, new Color { Val = "666666" })) { Type = StyleValues.Paragraph, StyleId = "Subtitle" });
        for (var i = 1; i <= 3; i++)
            styles.Append(new Style(new StyleName { Val = $"标题 {i}" }, new BasedOn { Val = "Normal" }, new NextParagraphStyle { Val = "Normal" }, new PrimaryStyle(), new StyleParagraphProperties(new KeepNext(), new KeepLines(), new SpacingBetweenLines { Before = i == 1 ? "360" : "240", After = "100" }, new OutlineLevel { Val = i - 1 }), new StyleRunProperties(new RunFonts { EastAsia = "Microsoft YaHei" }, new Bold(), new FontSize { Val = i == 1 ? "30" : i == 2 ? "26" : "24" }, new Color { Val = i == 1 ? Navy : Blue })) { Type = StyleValues.Paragraph, StyleId = $"Heading{i}" });
        part.Styles = styles;
        styles.Save();
    }

    static Paragraph Title(string text) => new(new ParagraphProperties(new ParagraphStyleId { Val = "Title" }), new Run(new Text(text)));
    static Paragraph Subtitle(string text) => new(new ParagraphProperties(new ParagraphStyleId { Val = "Subtitle" }), new Run(new Text(text)));
    static Paragraph H(string text, int level) => new(new ParagraphProperties(new ParagraphStyleId { Val = $"Heading{Math.Clamp(level, 1, 3)}" }), new Run(new Text(text)));
    static Paragraph P(string text) => new(new ParagraphProperties(new Indentation { FirstLineChars = 200 }, new AutoSpaceDE(), new AutoSpaceDN()), new Run(new Text(text)));
    static Paragraph Centered(string text) => new(new ParagraphProperties(new Justification { Val = JustificationValues.Center }), new Run(new Text(text)));

    static OpenXmlElement Numbered(params string[] items)
    {
        var container = new SdtBlock(new SdtProperties(), new SdtContentBlock());
        var content = container.GetFirstChild<SdtContentBlock>()!;
        for (var i = 0; i < items.Length; i++)
            content.Append(new Paragraph(new ParagraphProperties(new Indentation { Left = "360", Hanging = "360" }), new Run(new Text($"{i + 1}. {items[i]}"))));
        return container;
    }

    static Table InfoTable(string[][] rows) => DataTable(["项目", "内容"], rows, false);
    static Table SignatureTable(string submitter, string unit) => DataTable(["复核环节", "责任人", "意见", "日期"], [["资料编制", submitter, "数据已核对", "同编制日期"], ["部门复核", $"{unit}负责人", "同意报送", "同编制日期"], ["审计接收", "审计组资料管理员", "已登记", "接收当日"]]);
    static Table ApprovalTable(Project p) => DataTable(["审批环节", "审批意见", "签名", "日期"], [["项目负责人", "方案内容完整，建议按计划实施。", p.Leader.Split('（')[0], p.Start], ["部门复核", "同意实施，重大事项及时报告。", "郭强", p.Start], ["分管领导", "批准。", "王明远", p.Start]]);

    static Table DataTable(string[] headers, string[][] rows, bool repeatHeader = true)
    {
        var table = new Table(new TableProperties(
            new TableWidth { Width = "5000", Type = TableWidthUnitValues.Pct },
            new TableBorders(
                new TopBorder { Val = BorderValues.Single, Color = Border, Size = 6U }, new LeftBorder { Val = BorderValues.Single, Color = Border, Size = 4U },
                new BottomBorder { Val = BorderValues.Single, Color = Border, Size = 6U }, new RightBorder { Val = BorderValues.Single, Color = Border, Size = 4U },
                new InsideHorizontalBorder { Val = BorderValues.Single, Color = "D9D9D9", Size = 4U }, new InsideVerticalBorder { Val = BorderValues.Single, Color = "D9D9D9", Size = 4U }),
            new TableCellMarginDefault(new TopMargin { Width = "80", Type = TableWidthUnitValues.Dxa }, new TableCellLeftMargin { Width = 100, Type = TableWidthValues.Dxa }, new BottomMargin { Width = "80", Type = TableWidthUnitValues.Dxa }, new TableCellRightMargin { Width = 100, Type = TableWidthValues.Dxa })));
        var header = new TableRow();
        if (repeatHeader) header.TableRowProperties = new TableRowProperties(new TableHeader());
        foreach (var text in headers) header.Append(Cell(text, Navy, "FFFFFF", true));
        table.Append(header);
        for (var i = 0; i < rows.Length; i++)
        {
            var tr = new TableRow();
            foreach (var text in rows[i]) tr.Append(Cell(text, i % 2 == 1 ? LightGray : "FFFFFF", "222222", false));
            table.Append(tr);
        }
        return table;
    }

    static TableCell Cell(string text, string fill, string color, bool bold)
    {
        var rp = new RunProperties(new RunFonts { EastAsia = "Microsoft YaHei" }, new FontSize { Val = "20" }, new Color { Val = color });
        if (bold) rp.Append(new Bold());
        return new TableCell(new TableCellProperties(new Shading { Fill = fill, Val = ShadingPatternValues.Clear }), new Paragraph(new ParagraphProperties(new SpacingBetweenLines { After = "0", Line = "240", LineRule = LineSpacingRuleValues.Auto }), new Run(rp, new Text(text))));
    }

    static void AddHeader(MainDocumentPart main, SectionProperties sect, string title)
    {
        var part = main.AddNewPart<HeaderPart>();
        part.Header = new Header(new Paragraph(new ParagraphProperties(new Justification { Val = JustificationValues.Right }, new ParagraphBorders(new BottomBorder { Val = BorderValues.Single, Color = LightBlue, Size = 8U })), new Run(new RunProperties(new FontSize { Val = "18" }, new Color { Val = "6B7A90" }), new Text(title))));
        sect.Append(new HeaderReference { Type = HeaderFooterValues.Default, Id = main.GetIdOfPart(part) });
    }

    static void AddFooter(MainDocumentPart main, SectionProperties sect)
    {
        var part = main.AddNewPart<FooterPart>();
        var p = new Paragraph(new ParagraphProperties(new Justification { Val = JustificationValues.Center }));
        p.Append(new Run(new RunProperties(new FontSize { Val = "18" }, new Color { Val = "808080" }), new Text("审计管理系统演示数据  |  第 ")));
        p.Append(new Run(new FieldChar { FieldCharType = FieldCharValues.Begin }), new Run(new FieldCode(" PAGE ") { Space = SpaceProcessingModeValues.Preserve }), new Run(new FieldChar { FieldCharType = FieldCharValues.End }), new Run(new Text(" 页")));
        part.Footer = new Footer(p);
        sect.Append(new FooterReference { Type = HeaderFooterValues.Default, Id = main.GetIdOfPart(part) });
    }

    static string M(decimal value) => value.ToString("N2");
}
