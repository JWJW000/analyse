-- Enrich resource library content. This migration focuses on substantive body text,
-- not on using record counts as the quality signal.

UPDATE ethics_module
SET
    description = CASE
        WHEN description IS NULL OR CHAR_LENGTH(description) < 80 THEN
            '工程社会责任要求工程活动在技术可行、经济可行之外，同时回应公共安全、生态环境、资源消耗和社会公平等影响。教学应用中可引导学生在需求分析阶段识别利益相关者，说明系统上线后可能影响的人群、风险暴露场景和减缓措施，并把这些约束写入非功能需求、验收标准或运行规程。'
        ELSE description
    END,
    case_text = CASE
        WHEN case_text IS NULL OR CHAR_LENGTH(case_text) < 80 THEN
            '案例：某城市计划建设校园出入与访客预约系统，方案初稿强调通行效率和管理便利，却没有充分评估未成年人信息采集、家长知情同意、异常滞留告警误报、数据共享边界和弱势群体通行便利。讨论问题：需求文档中应增加哪些公共安全与隐私保护约束？哪些指标不能只用效率评价？'
        ELSE case_text
    END,
    reference = CASE
        WHEN reference IS NULL OR CHAR_LENGTH(reference) < 40 THEN
            '参考：NSPE Code of Ethics；《工程伦理》公共安全与社会责任章节；GB/T 35273 个人信息安全规范。'
        ELSE reference
    END
WHERE title = '工程社会责任';

UPDATE ethics_module
SET
    description = CASE
        WHEN description IS NULL OR CHAR_LENGTH(description) < 80 THEN
            '诚信与职业操守强调工程人员在数据采集、实验记录、需求确认、报告撰写和项目沟通中保持真实、可追溯和可复核。教学应用中可要求学生标注文献来源、说明需求结论依据，区分事实、假设和个人判断；涉及 AI 辅助生成内容时，还应记录使用范围并进行人工核验，防止把未经验证的输出作为工程结论。'
        ELSE description
    END,
    case_text = CASE
        WHEN case_text IS NULL OR CHAR_LENGTH(case_text) < 80 THEN
            '案例：学生团队为了让需求分析报告显得更充分，直接引用网络材料和 AI 生成段落，却没有标注文献来源，也没有验证其中关于用户规模、故障概率和法规要求的数据。讨论问题：哪些内容必须提供来源？哪些结论需要原始访谈、日志或实验数据支撑？教师审核时可以如何发现和纠正这类问题？'
        ELSE case_text
    END,
    reference = CASE
        WHEN reference IS NULL OR CHAR_LENGTH(reference) < 40 THEN
            '参考：IEEE Code of Ethics；ACM Code of Ethics and Professional Conduct；高校学术诚信与论文写作规范。'
        ELSE reference
    END
WHERE title = '诚信与职业操守';

UPDATE ethics_module_revision r
JOIN ethics_module m ON r.module_id = m.id AND r.version = 1
SET
    r.description = m.description,
    r.case_text = m.case_text,
    r.reference = m.reference
WHERE m.title IN ('工程社会责任', '诚信与职业操守')
  AND (
      r.description IS NULL OR CHAR_LENGTH(r.description) < 80
      OR r.case_text IS NULL OR CHAR_LENGTH(r.case_text) < 80
      OR r.reference IS NULL OR CHAR_LENGTH(r.reference) < 40
  );

INSERT INTO literature (title, author, source, abstract_text, keywords, created_by)
SELECT
    '需求工程中的证据链构建与可追溯管理',
    '课程案例组',
    '工程教育需求分析案例库',
    '本文围绕工程教育中的需求分析训练，讨论如何把访谈记录、问卷结果、业务日志、政策文件和学术文献组织为可追溯的证据链。文章强调，文献调研不应停留在资料罗列，而应服务于需求边界界定、冲突识别、优先级排序和验收标准形成。教学应用中，学生可以把每条核心需求关联到至少一类证据，并说明证据可信度、适用范围和潜在偏差，从而提升需求文档的可解释性与可审核性。',
    '需求工程,证据链,可追溯,文献调研,教学应用',
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM literature WHERE title = '需求工程中的证据链构建与可追溯管理'
);

INSERT INTO literature (title, author, source, abstract_text, keywords, created_by)
SELECT
    '工程伦理融入软件工程课程项目的实践路径',
    '课程案例组',
    '工程伦理与课程思政教学资料',
    '本文从软件工程课程项目出发，梳理工程伦理与课程思政融入需求分析、系统设计、测试评价和项目汇报的关键位置。文章提出，伦理内容不应作为报告末尾的独立段落，而应嵌入利益相关者分析、数据治理、可访问性、公平性、环境影响和职业责任等具体工程决策。教学应用中，教师可要求学生在需求说明中标出伦理约束，在设计方案中说明取舍理由，并在评审阶段反思技术方案对真实用户和公共利益的影响。',
    '工程伦理,课程思政,软件工程,需求分析,教学应用',
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM literature WHERE title = '工程伦理融入软件工程课程项目的实践路径'
);

INSERT INTO literature (title, author, source, abstract_text, keywords, created_by)
SELECT
    '面向学生项目的隐私保护与数据最小化需求分析',
    '课程案例组',
    '数据治理与隐私保护教学案例',
    '本文聚焦学生项目中常见的用户注册、行为日志、学习分析和推荐服务场景，说明如何在需求阶段落实隐私保护和数据最小化原则。文章建议将个人信息分类、采集目的、保存期限、访问权限、脱敏策略和删除机制写入需求文档，并通过异常场景分析识别过度采集、二次使用和权限扩散风险。教学应用中，学生可基于该文献检查自己的项目是否存在不必要字段、模糊授权或缺失退出机制等问题。',
    '隐私保护,数据最小化,学生项目,需求分析,工程伦理',
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM literature WHERE title = '面向学生项目的隐私保护与数据最小化需求分析'
);

UPDATE literature
SET
    abstract_text = CASE
        WHEN abstract_text IS NULL OR CHAR_LENGTH(abstract_text) < 180 THEN
            '本文围绕工程教育中的需求分析训练，讨论如何把访谈记录、问卷结果、业务日志、政策文件和学术文献组织为可追溯的证据链。文章强调，文献调研不应停留在资料罗列，而应服务于需求边界界定、冲突识别、优先级排序和验收标准形成。教学应用中，学生可以把每条核心需求关联到至少一类证据，并说明证据可信度、适用范围和潜在偏差，从而提升需求文档的可解释性与可审核性。'
        ELSE abstract_text
    END,
    keywords = CASE
        WHEN keywords IS NULL OR CHAR_LENGTH(keywords) < 20 THEN '需求工程,证据链,可追溯,文献调研,教学应用'
        ELSE keywords
    END
WHERE title = '需求工程中的证据链构建与可追溯管理';

UPDATE literature
SET
    abstract_text = CASE
        WHEN abstract_text IS NULL OR CHAR_LENGTH(abstract_text) < 180 THEN
            '本文从软件工程课程项目出发，梳理工程伦理与课程思政融入需求分析、系统设计、测试评价和项目汇报的关键位置。文章提出，伦理内容不应作为报告末尾的独立段落，而应嵌入利益相关者分析、数据治理、可访问性、公平性、环境影响和职业责任等具体工程决策。教学应用中，教师可要求学生在需求说明中标出伦理约束，在设计方案中说明取舍理由，并在评审阶段反思技术方案对真实用户和公共利益的影响。'
        ELSE abstract_text
    END,
    keywords = CASE
        WHEN keywords IS NULL OR CHAR_LENGTH(keywords) < 20 THEN '工程伦理,课程思政,软件工程,需求分析,教学应用'
        ELSE keywords
    END
WHERE title = '工程伦理融入软件工程课程项目的实践路径';

UPDATE literature
SET
    abstract_text = CASE
        WHEN abstract_text IS NULL OR CHAR_LENGTH(abstract_text) < 180 THEN
            '本文聚焦学生项目中常见的用户注册、行为日志、学习分析和推荐服务场景，说明如何在需求阶段落实隐私保护和数据最小化原则。文章建议将个人信息分类、采集目的、保存期限、访问权限、脱敏策略和删除机制写入需求文档，并通过异常场景分析识别过度采集、二次使用和权限扩散风险。教学应用中，学生可基于该文献检查自己的项目是否存在不必要字段、模糊授权或缺失退出机制等问题。'
        ELSE abstract_text
    END,
    keywords = CASE
        WHEN keywords IS NULL OR CHAR_LENGTH(keywords) < 20 THEN '隐私保护,数据最小化,学生项目,需求分析,工程伦理'
        ELSE keywords
    END
WHERE title = '面向学生项目的隐私保护与数据最小化需求分析';
