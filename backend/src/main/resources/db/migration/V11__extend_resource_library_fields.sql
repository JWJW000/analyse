ALTER TABLE literature
    ADD COLUMN publication_year INT NULL,
    ADD COLUMN doi VARCHAR(128) NULL,
    ADD COLUMN url VARCHAR(512) NULL,
    ADD COLUMN literature_type VARCHAR(100) NULL,
    ADD COLUMN research_method VARCHAR(100) NULL,
    ADD COLUMN applicable_topic VARCHAR(255) NULL,
    ADD COLUMN key_findings TEXT NULL,
    ADD COLUMN evidence_value TEXT NULL;

ALTER TABLE ethics_module
    ADD COLUMN applicable_scenario TEXT NULL,
    ADD COLUMN teaching_objective TEXT NULL,
    ADD COLUMN value_point TEXT NULL,
    ADD COLUMN discussion_questions TEXT NULL,
    ADD COLUMN risk_points TEXT NULL,
    ADD COLUMN integration_suggestion TEXT NULL,
    ADD COLUMN applicable_major VARCHAR(255) NULL,
    ADD COLUMN difficulty_level VARCHAR(50) NULL;

ALTER TABLE ethics_module_revision
    ADD COLUMN applicable_scenario TEXT NULL,
    ADD COLUMN teaching_objective TEXT NULL,
    ADD COLUMN value_point TEXT NULL,
    ADD COLUMN discussion_questions TEXT NULL,
    ADD COLUMN risk_points TEXT NULL,
    ADD COLUMN integration_suggestion TEXT NULL,
    ADD COLUMN applicable_major VARCHAR(255) NULL,
    ADD COLUMN difficulty_level VARCHAR(50) NULL;

UPDATE literature
SET
    publication_year = COALESCE(publication_year, 2025),
    literature_type = COALESCE(literature_type, '教学案例'),
    research_method = COALESCE(research_method, '案例研究'),
    applicable_topic = COALESCE(applicable_topic, '需求分析、文献证据映射'),
    key_findings = COALESCE(key_findings, '文献证据需要服务于需求边界、约束条件和验收标准，而不是停留在资料堆砌。'),
    evidence_value = COALESCE(evidence_value, '可用于支撑需求来源说明、证据可信度分析和报告中的文献综述。')
WHERE title = '需求工程中的证据链构建与可追溯管理';

UPDATE literature
SET
    publication_year = COALESCE(publication_year, 2025),
    literature_type = COALESCE(literature_type, '教学研究'),
    research_method = COALESCE(research_method, '课程实践总结'),
    applicable_topic = COALESCE(applicable_topic, '工程伦理、课程思政、需求嵌入'),
    key_findings = COALESCE(key_findings, '伦理思政内容应嵌入具体工程决策和需求约束，而不是作为报告末尾的独立段落。'),
    evidence_value = COALESCE(evidence_value, '可用于支撑思政融合章节、伦理约束提取和课程项目评审。')
WHERE title = '工程伦理融入软件工程课程项目的实践路径';

UPDATE literature
SET
    publication_year = COALESCE(publication_year, 2025),
    literature_type = COALESCE(literature_type, '教学案例'),
    research_method = COALESCE(research_method, '场景分析'),
    applicable_topic = COALESCE(applicable_topic, '隐私保护、数据最小化、非功能需求'),
    key_findings = COALESCE(key_findings, '个人信息分类、采集目的、保存期限、访问权限和删除机制应在需求阶段明确。'),
    evidence_value = COALESCE(evidence_value, '可用于检查学生项目中的过度采集、模糊授权和权限扩散风险。')
WHERE title = '面向学生项目的隐私保护与数据最小化需求分析';

UPDATE ethics_module
SET
    applicable_scenario = COALESCE(applicable_scenario, '公共服务系统、校园管理系统、涉及公共安全或弱势群体影响的工程项目。'),
    teaching_objective = COALESCE(teaching_objective, '帮助学生在需求分析阶段识别公共利益、利益相关者和长期社会影响。'),
    value_point = COALESCE(value_point, '人民至上、公共安全、可持续发展、社会公平。'),
    discussion_questions = COALESCE(discussion_questions, '系统效率与公共安全发生冲突时如何取舍？哪些指标不能只用成本或速度评价？'),
    risk_points = COALESCE(risk_points, '忽视弱势群体、过度收集个人信息、只关注功能交付而忽视社会影响。'),
    integration_suggestion = COALESCE(integration_suggestion, '在非功能需求、约束条件和验收标准中写明公共安全、隐私保护、可访问性和责任边界。'),
    applicable_major = COALESCE(applicable_major, '软件工程、计算机科学、人工智能、信息管理'),
    difficulty_level = COALESCE(difficulty_level, '中')
WHERE title = '工程社会责任';

UPDATE ethics_module
SET
    applicable_scenario = COALESCE(applicable_scenario, '需求调研、文献综述、实验记录、报告撰写、AI 辅助生成内容审核。'),
    teaching_objective = COALESCE(teaching_objective, '帮助学生区分事实、假设和观点，形成可追溯、可复核的工程表达。'),
    value_point = COALESCE(value_point, '诚实守信、求真务实、职业责任、学术规范。'),
    discussion_questions = COALESCE(discussion_questions, '哪些结论必须标注来源？AI 生成内容可以直接作为工程依据吗？'),
    risk_points = COALESCE(risk_points, '伪造数据、遗漏风险、复制未标注文献、把未经验证的 AI 输出作为事实。'),
    integration_suggestion = COALESCE(integration_suggestion, '在需求来源、文献引用、假设条件和验收依据中明确证据来源与核验方式。'),
    applicable_major = COALESCE(applicable_major, '软件工程、计算机科学、数据科学、工程管理'),
    difficulty_level = COALESCE(difficulty_level, '中')
WHERE title = '诚信与职业操守';

UPDATE ethics_module_revision r
JOIN ethics_module m ON r.module_id = m.id AND r.version = m.current_version
SET
    r.applicable_scenario = m.applicable_scenario,
    r.teaching_objective = m.teaching_objective,
    r.value_point = m.value_point,
    r.discussion_questions = m.discussion_questions,
    r.risk_points = m.risk_points,
    r.integration_suggestion = m.integration_suggestion,
    r.applicable_major = m.applicable_major,
    r.difficulty_level = m.difficulty_level
WHERE m.title IN ('工程社会责任', '诚信与职业操守');
