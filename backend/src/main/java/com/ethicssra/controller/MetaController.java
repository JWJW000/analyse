package com.ethicssra.controller;

import com.ethicssra.dto.ApiResponse;
import com.ethicssra.dto.RequirementTemplateDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/meta")
public class MetaController {

    @GetMapping("/requirement-templates")
    public ApiResponse<List<RequirementTemplateDto>> requirementTemplates() {
        return ApiResponse.ok(List.of(
                new RequirementTemplateDto(
                        "smart-campus",
                        "智慧校园一卡通",
                        "面向校园场景的身份认证与消费管理，关注隐私与数据安全。",
                        List.of("校园", "隐私", "数据安全"),
                        "作为在校学生，我希望通过一卡通完成门禁与食堂支付，以便减少携带现金；"
                                + "系统应记录交易流水并支持挂失。需要与教务系统对接身份。",
                        "描述学校规模、现有信息化基础及一卡通建设背景。",
                        "列出核心用户（学生/后勤/财务）与系统边界，说明不做哪些事。",
                        "说明个人信息最小化、日志留存期限、公平使用（贫困生补助）等伦理点。"
                ),
                new RequirementTemplateDto(
                        "health-elder",
                        "社区养老健康监测",
                        "可穿戴设备采集老人健康数据，涉及知情同意与紧急联动。",
                        List.of("医疗", "知情同意", "弱势群体"),
                        "作为社区护理员，我希望在获得家属授权后查看老人心率与步数异常告警，"
                                + "以便及时上门；系统应区分紧急与普通告警。",
                        "交代社区人口结构、设备来源（政府采购/自费）。",
                        "明确告警分级、响应时限、与 120 的联动边界。",
                        "强调老人知情权、数据用途告知、家属代理同意的边界。"
                ),
                new RequirementTemplateDto(
                        "gov-service",
                        "政务一网通办",
                        "线上办事大厅，关注算法公平与可解释性。",
                        List.of("政务", "公平", "透明"),
                        "作为办事群众，我希望在线提交材料并查询进度，以便少跑腿；"
                                + "系统应提供材料清单与补正原因说明。",
                        "说明对接部门、事项类型（许可/备案）。",
                        "界定线上/线下协同流程与超时处理。",
                        "阐述审批辅助算法的可解释性、申诉渠道与对弱势群体的辅助。"
                ),
                new RequirementTemplateDto(
                        "ai-recruit",
                        "招聘简历智能筛选辅助",
                        "HR 场景下的辅助排序，需防范歧视与隐私泄露。",
                        List.of("招聘", "公平", "算法"),
                        "作为 HR，我希望按岗位关键词对简历排序并标注匹配原因，以便提高初筛效率；"
                                + "系统不得自动淘汰候选人，仅提供参考。",
                        "公司简介与招聘岗位背景。",
                        "说明人工复核环节、候选人通知方式。",
                        "列出禁止使用的敏感特征（性别、地域等）与审计要求。"
                ),
                new RequirementTemplateDto(
                        "carbon-accounting",
                        "企业碳排放核算",
                        "碳数据上报与核查，关注数据真实性与第三方审计。",
                        List.of("环保", "数据真实性", "合规"),
                        "作为企业环保专员，我希望汇总各厂区能耗与排放因子并生成报告，以便向监管报送；"
                                + "系统应保留原始凭证与修订历史。",
                        "行业与监管框架（国标/地方细则）。",
                        "数据来源（仪表/手工录入）与校验规则。",
                        "阐述数据造假防范、第三方核查接口与责任划分。"
                )
        ));
    }
}
