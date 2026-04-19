package com.ethicssra.service;

import com.ethicssra.dto.DiagramGenerationDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DiagramGenerationService {

    private int nodeCounter = 0;
    private int edgeCounter = 0;

    public DiagramGenerationDto generateFlowchart(String requirementText) {
        nodeCounter = 0;
        edgeCounter = 0;
        
        List<DiagramGenerationDto.DiagramNode> nodes = new ArrayList<>();
        List<DiagramGenerationDto.DiagramEdge> edges = new ArrayList<>();

        nodes.add(createNode("start", "开始", "start", "项目启动"));

        List<String> processes = extractProcesses(requirementText);
        List<String> decisions = extractDecisions(requirementText);
        List<String> inputs = extractInputs(requirementText);
        List<String> outputs = extractOutputs(requirementText);

        String lastNodeId = "start";
        
        for (int i = 0; i < Math.min(processes.size(), 5); i++) {
            String nodeId = createNodeId();
            nodes.add(createNode(nodeId, processes.get(i), "process", "主要处理流程"));
            edges.add(createEdge(lastNodeId, nodeId, ""));
            lastNodeId = nodeId;
        }

        for (String decision : decisions) {
            String nodeId = createNodeId();
            nodes.add(createNode(nodeId, decision, "decision", "判断条件"));
            edges.add(createEdge(lastNodeId, nodeId, ""));
            
            String trueNodeId = createNodeId();
            nodes.add(createNode(trueNodeId, "满足条件", "process", "条件满足时的处理"));
            edges.add(createEdge(nodeId, trueNodeId, "是"));
            
            String falseNodeId = createNodeId();
            nodes.add(createNode(falseNodeId, "不满足条件", "process", "条件不满足时的处理"));
            edges.add(createEdge(nodeId, falseNodeId, "否"));
            
            lastNodeId = trueNodeId;
        }

        if (!inputs.isEmpty()) {
            String nodeId = createNodeId();
            nodes.add(createNode(nodeId, inputs.get(0), "input", "数据输入"));
            edges.add(createEdge(lastNodeId, nodeId, ""));
            lastNodeId = nodeId;
        }

        if (!outputs.isEmpty()) {
            String nodeId = createNodeId();
            nodes.add(createNode(nodeId, outputs.get(0), "output", "结果输出"));
            edges.add(createEdge(lastNodeId, nodeId, ""));
            lastNodeId = nodeId;
        }

        nodes.add(createNode("end", "结束", "end", "流程结束"));
        edges.add(createEdge(lastNodeId, "end", ""));

        String explanation = generateExplanation(requirementText, processes, decisions);
        List<String> recommendations = generateRecommendations(requirementText);

        return new DiagramGenerationDto(
            "flowchart",
            nodes,
            edges,
            explanation,
            recommendations
        );
    }

    public DiagramGenerationDto generateUseCaseDiagram(String requirementText) {
        nodeCounter = 0;
        edgeCounter = 0;
        
        List<DiagramGenerationDto.DiagramNode> nodes = new ArrayList<>();
        List<DiagramGenerationDto.DiagramEdge> edges = new ArrayList<>();

        List<String> actors = extractActors(requirementText);
        List<String> useCases = extractUseCases(requirementText);

        Map<String, String> actorIds = new LinkedHashMap<>();
        for (int i = 0; i < actors.size(); i++) {
            String actorId = "actor-" + (i + 1);
            actorIds.put(actors.get(i), actorId);
            nodes.add(createNode(actorId, actors.get(i), "actor", "参与系统交互的角色"));
        }

        Map<String, String> useCaseIds = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(useCases.size(), 8); i++) {
            String ucId = "uc" + (i + 1);
            useCaseIds.put(useCases.get(i), ucId);
            nodes.add(createNode(ucId, useCases.get(i), "usecase", "由需求正文提取的用例"));
        }

        edges.addAll(buildUseCaseEdges(requirementText, actorIds, useCaseIds));
        if (edges.isEmpty() && !actorIds.isEmpty()) {
            String firstActorId = actorIds.values().iterator().next();
            for (String ucId : useCaseIds.values()) {
                edges.add(createEdge(firstActorId, ucId, "使用"));
            }
        }

        String explanation = "该用例图从需求正文中提取了 " + actors.size() + " 个参与者、"
                + useCaseIds.size() + " 个主要用例，并建立参与者与用例之间的交互关系。";
        List<String> recommendations = new ArrayList<>();
        if (useCaseIds.size() < 3) {
            recommendations.add("建议补充更多用例以完整描述系统功能");
        }
        if (actors.size() == 1) {
            recommendations.add("建议明确区分学生、教师、管理员等不同参与者");
        }
        recommendations.add("确保每个用例都有明确的前置条件和后置条件");

        return new DiagramGenerationDto(
            "usecase",
            nodes,
            edges,
            explanation,
            recommendations
        );
    }

    private List<String> extractActors(String text) {
        Set<String> actors = new LinkedHashSet<>();
        String source = text == null ? "" : text;
        String[] knownActors = {"学生", "教师", "老师", "管理员", "用户", "卖家", "买家", "访客", "审核员", "系统"};
        for (String actor : knownActors) {
            if (source.contains(actor)) {
                actors.add(actor);
            }
        }
        if (actors.isEmpty()) {
            actors.add("用户");
        }
        return actors.stream().limit(5).toList();
    }

    private List<String> extractProcesses(String text) {
        List<String> processes = new ArrayList<>();
        if (text == null || text.isBlank()) {
            processes.add("数据处理");
            processes.add("业务逻辑");
            processes.add("结果保存");
            return processes;
        }

        String[] keywords = {"用户", "系统", "管理", "处理", "查询", "添加", "修改", "删除", "审核", "审批"};
        for (String kw : keywords) {
            if (text.contains(kw)) {
                processes.add(kw + "模块");
                if (processes.size() >= 4) break;
            }
        }

        if (processes.isEmpty()) {
            processes.add("主要流程");
            processes.add("辅助流程");
        }

        return processes;
    }

    private List<String> extractDecisions(String text) {
        List<String> decisions = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return decisions;
        }

        String[] decisionKeywords = {"是否", "是否合法", "是否通过", "有无权限", "是否满足"};
        for (String kw : decisionKeywords) {
            if (text.contains(kw)) {
                decisions.add(kw);
            }
        }

        return decisions;
    }

    private List<String> extractInputs(String text) {
        List<String> inputs = new ArrayList<>();
        if (text == null || text.isBlank()) {
            inputs.add("用户输入");
            return inputs;
        }

        String[] inputKeywords = {"输入", "接收", "采集", "录入"};
        for (String kw : inputKeywords) {
            if (text.contains(kw)) {
                inputs.add(kw + "数据");
            }
        }

        if (inputs.isEmpty()) {
            inputs.add("数据输入");
        }

        return inputs;
    }

    private List<String> extractOutputs(String text) {
        List<String> outputs = new ArrayList<>();
        if (text == null || text.isBlank()) {
            outputs.add("结果输出");
            return outputs;
        }

        String[] outputKeywords = {"输出", "显示", "返回", "生成", "导出", "打印"};
        for (String kw : outputKeywords) {
            if (text.contains(kw)) {
                outputs.add(kw + "结果");
            }
        }

        if (outputs.isEmpty()) {
            outputs.add("处理结果");
        }

        return outputs;
    }

    private List<String> extractUseCases(String text) {
        Set<String> useCases = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            useCases.add("用户登录");
            useCases.add("信息查询");
            useCases.add("数据管理");
            return new ArrayList<>(useCases);
        }

        String[] actionKeywords = {
                "注册", "登录", "发布", "搜索", "查询", "提交", "审核", "导出", "管理",
                "上传", "下载", "查看", "创建", "添加", "修改", "删除", "设置", "沟通", "统计", "打印"
        };
        for (String kw : actionKeywords) {
            Matcher matcher = Pattern
                    .compile(Pattern.quote(kw) + "[^，。、；;,.\\n]{0,14}")
                    .matcher(text);
            while (matcher.find() && useCases.size() < 10) {
                String label = cleanUseCaseLabel(matcher.group());
                if (label.length() >= 2 && !startsWithActor(label)) {
                    useCases.add(label);
                }
            }
        }

        if (useCases.isEmpty()) {
            useCases.add("基本操作");
        }

        return new ArrayList<>(useCases);
    }

    private String cleanUseCaseLabel(String raw) {
        String s = raw == null ? "" : raw.trim();
        s = s.split("并|以及|同时|，|、")[0].trim();
        s = s.replaceAll("(并|和|及|与)$", "");
        s = s.replaceAll("(可以|需要|能够|功能)$", "");
        return s.length() > 12 ? s.substring(0, 12) : s;
    }

    private boolean startsWithActor(String label) {
        String[] actors = {"学生", "教师", "老师", "管理员", "用户", "卖家", "买家", "访客", "审核员", "系统"};
        for (String actor : actors) {
            if (label.startsWith(actor)) {
                return true;
            }
        }
        return false;
    }

    private List<DiagramGenerationDto.DiagramEdge> buildUseCaseEdges(
            String text,
            Map<String, String> actorIds,
            Map<String, String> useCaseIds
    ) {
        List<DiagramGenerationDto.DiagramEdge> edges = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        String[] sentences = splitSentences(text);
        for (Map.Entry<String, String> actor : actorIds.entrySet()) {
            for (String sentence : sentences) {
                if (!sentence.contains(actor.getKey())) {
                    continue;
                }
                for (Map.Entry<String, String> useCase : useCaseIds.entrySet()) {
                    if (sentence.contains(useCase.getKey()) || sentenceContainsAction(sentence, useCase.getKey())) {
                        String key = actor.getValue() + "->" + useCase.getValue();
                        if (seen.add(key)) {
                            edges.add(createEdge(actor.getValue(), useCase.getValue(), "使用"));
                        }
                    }
                }
            }
        }
        return edges;
    }

    private String[] splitSentences(String text) {
        if (text == null || text.isBlank()) {
            return new String[0];
        }
        return text.split("[。！？!?\\n]+");
    }

    private boolean sentenceContainsAction(String sentence, String useCaseLabel) {
        if (useCaseLabel.length() < 2) {
            return false;
        }
        String action = useCaseLabel.length() > 4 ? useCaseLabel.substring(0, 2) : useCaseLabel;
        return sentence.contains(action);
    }

    private String generateExplanation(String text, List<String> processes, List<String> decisions) {
        StringBuilder sb = new StringBuilder();
        sb.append("该流程图描述了");
        if (!processes.isEmpty()) {
            sb.append(processes.get(0));
        }
        sb.append("的主要处理过程。");
        
        if (!decisions.isEmpty()) {
            sb.append("包含").append(decisions.size()).append("个判断节点，用于处理不同情况。");
        }
        
        sb.append("共").append(processes.size()).append("个主要处理步骤。");
        
        return sb.toString();
    }

    private List<String> generateRecommendations(String text) {
        List<String> recommendations = new ArrayList<>();
        
        if (text == null || text.length() < 100) {
            recommendations.add("建议补充更详细的需求描述以便生成更准确的流程图");
        }
        
        if (!text.contains("异常") && !text.contains("错误") && !text.contains("失败")) {
            recommendations.add("建议增加异常处理流程以提高系统健壮性");
        }
        
        if (!text.contains("日志") && !text.contains("记录")) {
            recommendations.add("建议增加日志记录功能以便问题追踪");
        }
        
        return recommendations;
    }

    private String createNodeId() {
        return "node" + (++nodeCounter);
    }

    private String createEdgeId() {
        return "edge" + (++edgeCounter);
    }

    private DiagramGenerationDto.DiagramNode createNode(String id, String label, String type, String description) {
        return new DiagramGenerationDto.DiagramNode(id, label, type, description);
    }

    private DiagramGenerationDto.DiagramEdge createEdge(String source, String target, String label) {
        return new DiagramGenerationDto.DiagramEdge(createEdgeId(), source, target, label);
    }
}
