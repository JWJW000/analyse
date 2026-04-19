-- 幂等种子数据：已有同名标题或已有 revision 时跳过
INSERT INTO ethics_module (title, category, keywords, description, case_text, reference, current_version)
SELECT v.title, v.category, v.keywords, v.description, v.case_text, v.reference, v.current_version
FROM (
    SELECT '工程社会责任' AS title, '责任伦理' AS category, '社会责任,公众安全' AS keywords,
           '工程师在决策中应考虑对社会、环境与可持续发展的影响。' AS description,
           '大型公共设施需进行社会影响评估。' AS case_text, 'NSPE伦理准则' AS reference, 1 AS current_version
) AS v
WHERE NOT EXISTS (SELECT 1 FROM ethics_module m WHERE m.title = v.title);

INSERT INTO ethics_module (title, category, keywords, description, case_text, reference, current_version)
SELECT v.title, v.category, v.keywords, v.description, v.case_text, v.reference, v.current_version
FROM (
    SELECT '诚信与职业操守' AS title, '职业伦理' AS category, '诚信,利益冲突' AS keywords,
           '禁止伪造数据、隐瞒风险或接受不当利益。' AS description,
           '学术与工程报告须可追溯、可验证。' AS case_text, 'IEEE伦理规范' AS reference, 1 AS current_version
) AS v
WHERE NOT EXISTS (SELECT 1 FROM ethics_module m WHERE m.title = v.title);

INSERT INTO ethics_module_revision (module_id, version, title, category, keywords, description, case_text, reference)
SELECT m.id, 1, m.title, m.category, m.keywords, m.description, m.case_text, m.reference
FROM ethics_module m
WHERE NOT EXISTS (
    SELECT 1 FROM ethics_module_revision r WHERE r.module_id = m.id AND r.version = 1
);
