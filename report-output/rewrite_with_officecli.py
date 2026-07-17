import json, shutil, subprocess, sys
from pathlib import Path

ROOT=Path(r'F:\project1\RuoYi-Vue-v3.9.2\report-output')
REF=ROOT/'reference.docx'
OUT=ROOT/'officecli-rewrite'
OUT.mkdir(exist_ok=True)
sys.path.insert(0,str(ROOT))
from build_reports import members

def run(args, capture=True):
    cp=subprocess.run(['officecli',*map(str,args)], text=True, encoding='utf-8', errors='replace', capture_output=capture)
    if cp.returncode:
        raise RuntimeError(f"officecli failed: {' '.join(map(str,args))}\n{cp.stdout}\n{cp.stderr}")
    return cp.stdout

def para(text, style='Normal', **extra):
    props={'text':text,'style':style,'color':'000000','font.ea':'宋体','font.latin':'Times New Roman'}
    if style=='Title': props.update({'size':'16pt','bold':'true','spaceBefore':'12pt','spaceAfter':'3pt','keepWithNext':'true'})
    elif style=='heading 2': props.update({'size':'14pt','bold':'true','spaceBefore':'10pt','spaceAfter':'3pt','keepWithNext':'true'})
    else: props.update({'size':'10.5pt','align':'justify','lineSpacing':'1.5x','firstLineIndent':'21pt','spaceAfter':'0pt'})
    props.update(extra)
    return {'command':'add','parent':'/body','type':'paragraph','props':props}

def content(m):
    c=[]
    def H1(t): c.append(para(t,'Title'))
    def H2(t): c.append(para(t,'heading 2'))
    def P(t): c.append(para(t))
    H1('项目实践的目的与要求')
    H2('1.1 项目实践目的')
    P('本项目面向高校审计业务数字化与智能化需求，综合运用Java程序设计、数据库原理、软件工程、Web前端开发和人工智能应用等知识，设计并实现高校智慧审计平台。通过实践掌握前后端分离系统从需求分析、架构设计、数据库设计、编码实现、联调测试到云服务器部署的完整流程。')
    P('平台将大模型能力融入审计计划、项目实施、问题发现、整改跟踪、报告编制和项目归档等环节，使审计人员能够通过自然语言完成资料查询、法规检索、数据分析、风险扫描、文档核查和取证单生成。')
    H2('1.2 项目实践课程要求')
    P('掌握Vue 3、Element Plus、Vite和Axios等前端技术；掌握Spring Boot、MyBatis、Spring Security和JWT等后端技术；掌握PostgreSQL及pgvector向量检索；完成审计业务闭环、AI智能辅助、权限管理、测试和云服务器部署。')
    H1('二、项目总体设计方案')
    H2('2.1 总体概述')
    P('高校智慧审计平台采用前后端分离架构，覆盖审计计划、审计准备、现场实施、问题整改、报告归档和智能分析。系统以审计项目为主线，将计划、资料、底稿、问题、整改、报告和档案数据统一关联。')
    H2('2.2 前端设计')
    P('前端使用Vue 3、Element Plus、Vite和Axios。Vue负责组件化页面开发，Element Plus提供表单、表格、对话框和分页组件，ECharts负责进度图和数据驾驶舱，Axios负责REST接口调用，AI对话页面通过SSE接收流式结果。')
    H2('2.3 后端设计')
    P('后端基于Spring Boot和MyBatis实现Controller、Service、Mapper分层。系统复用RuoYi的用户、角色、菜单、日志和数据权限能力；PostgreSQL保存业务数据，pgvector保存文档向量；LangChain4j及OpenAI兼容接口负责模型调用。')
    H2('2.4 数据库设计')
    P('数据库分为系统基础表、审计业务表和AI知识表。核心业务表包括audit_plan、audit_project、audit_issue、audit_rectification、audit_workpaper和audit_report；知识与AI表包括audit_basis、audit_case_lib、project_document、document_chunk、ai_conversation和ai_message。')
    H1('三、软件主要流程图')
    H2('3.1 项目总体流程')
    P('审计计划编制 → 审计项目立项 → 审计准备与资料收集 → 审计方案和底稿编制 → 审计问题确认 → 整改跟踪 → 审计报告 → 项目归档。')
    H2('3.2 AI智能处理流程')
    P('用户输入自然语言指令 → 系统识别任务类型 → 检索项目资料或审计知识 → 调用对应业务处理器与大模型 → SSE流式返回结果 → 保存会话、分析结果或取证记录。')
    H2('3.3 文件知识化流程')
    P('文件上传 → 文件类型识别 → Word、Excel、PDF、CSV或TXT内容解析 → 文本切分 → Embedding向量化 → PostgreSQL与pgvector入库 → RAG语义检索。')
    H1('四、个人完成的功能及代码')
    H2('4.1 个人完成功能概述')
    P(f"本人承担本项目{m['share']}的工作量，负责{m['title']}。{m['overview']}")
    for i,(h,t) in enumerate(m['modules'],2):
        H2(f'4.{i} {h}')
        P(t)
        P('该功能采用前后端分离方式实现。前端负责数据录入、查询筛选、状态展示和操作反馈；后端按照Controller、Service和Mapper分层处理业务，通过参数校验、权限校验和异常处理保证数据正确性。')
    H1('五、关键实现流程')
    for i,(h,t) in enumerate(m['code'],1):
        H2(f'5.{i} {h}')
        P(t)
    H1('六、系统测试')
    H2('6.1 测试环境')
    P('测试环境包括Windows 11、JDK 21、Node.js、Vue 3、Spring Boot、PostgreSQL和Chrome浏览器。测试采用正常数据、空值、边界值和异常数据，检查页面提示、接口响应和数据库记录。')
    H2('6.2 个人模块测试')
    for i,t in enumerate(m['tests'],1): P(f'测试{i}：{t}。执行相关操作并核对页面、接口和数据库结果，测试结果符合设计要求。')
    H2('6.3 测试结论')
    P('经过功能测试和前后端联调，个人负责模块能够完成预定业务操作，权限不足、参数错误和服务异常能够返回明确提示。发现的问题经过修复后重新验证，未发现影响主要业务流程的严重缺陷。')
    H1('七、云服务器部署')
    H2('7.1 服务器环境')
    P('云服务器采用Linux系统，安装JDK 21、PostgreSQL、pgvector和Nginx。安全组仅开放Web访问和必要管理端口，数据库端口不直接对公网开放。')
    H2('7.2 数据库与后端部署')
    P('创建PostgreSQL数据库和用户，启用pgvector扩展，依次执行RuoYi基础脚本、AI模块脚本和审计业务脚本。修改生产环境数据库、文件目录和AI模型参数，使用Maven打包后端Jar，通过后台服务或进程守护方式启动。')
    H2('7.3 前端与Nginx部署')
    P('使用Vite构建前端dist目录，将构建产物上传到Nginx站点目录。Nginx负责静态资源访问，并将后端接口和文件请求反向代理至Spring Boot服务，同时配置上传大小、连接超时和访问日志。')
    H2('7.4 上线检查')
    P('部署完成后依次检查用户登录、菜单权限、审计业务闭环、文件上传与解析、AI模型调用、SSE流式输出、数据驾驶舱和日志记录，确认云端系统能够稳定访问。')
    H1('八、总结')
    P(f"通过本次高校智慧审计平台开发，我完成了{m['title']}相关工作，对需求分析、模块划分、接口设计、数据库关联、权限控制、联调测试和云端部署有了更加系统的理解。")
    P('项目实践说明软件开发不仅是编码，还需要统一的数据标准、清晰的职责边界、可靠的异常处理和规范的部署维护。后续可继续完善自动化测试、AI结果评价、模型成本统计、移动端适配和运行监控。')
    H1('参考文献')
    for t in ['[1] RuoYi-Vue v3.9.2 项目文档。','[2] Spring Boot Reference Documentation。','[3] Vue.js 3 Documentation。','[4] PostgreSQL Documentation。','[5] LangChain4j Documentation。','[6] Apache ECharts Documentation。']:
        c.append(para(t,'Normal',firstLineIndent='0pt'))
    return c

def rewrite(m):
    target=OUT/f"{m['name']}_项目综合实践III_高校智慧审计平台_officecli重写.docx"
    shutil.copy2(REF,target)
    run(['open',target])
    # Cover and front-matter identity edits keep the original run/paragraph formatting.
    replacements=[('陈宇豪',m['name']),('231302102',m['sid']),('231320102',m['sid']),('计科2301','计科2201'),('陈宏宇',m['name'])]
    for old,new in replacements:
        if old!=new: run(['set',target,'/','--find',old,'--replace',new])
    raw=run(['get',target,'/body','--depth','1','--json'])
    data=json.loads(raw); children=data['data']['results'][0]['children']
    anchor=next(i for i,x in enumerate(children) if x.get('type')=='paragraph' and x.get('text','').strip()=='项目实践的目的与要求')
    removable=[x['path'] for x in children[anchor:] if x.get('type') not in ('section','sectPr')]
    # Remove in reverse order so positional table paths remain stable; stable paraId paths do not shift.
    ops=[{'command':'remove','path':p} for p in reversed(removable)]
    for start in range(0,len(ops),250):
        batch=OUT/f'_remove_{m["name"]}_{start}.json'; batch.write_text(json.dumps(ops[start:start+250],ensure_ascii=False),encoding='utf-8')
        run(['batch',target,'--input',batch,'--stop-on-error']); batch.unlink()
    addops=content(m)
    batch=OUT/f'_add_{m["name"]}.json'; batch.write_text(json.dumps(addops,ensure_ascii=False),encoding='utf-8')
    run(['batch',target,'--input',batch,'--stop-on-error']); batch.unlink()
    run(['set',target,'/settings','--prop','updateFields=true'])
    # Explicitly force all source heading styles and inserted heading runs to black.
    run(['set',target,'/body/paragraph[style=Title]','--prop','color=000000'])
    run(['set',target,'/body/paragraph[style="heading 2"]','--prop','color=000000'])
    run(['save',target]); run(['close',target])
    return target

if __name__=='__main__':
    for m in members: print(rewrite(m))
