package com.zxslsoft.general.activiti;

import com.alibaba.fastjson.JSONArray;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.activiti.image.ProcessDiagramGenerator;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

public class WorkflowManagementServlet extends HttpServlet implements ModelDataJsonConstants {

    private ObjectMapper objectMapper = new ObjectMapper();
    private RepositoryService repositoryService;
    private RuntimeService runtimeService;
    private TaskService taskService;
    private HistoryService historyService;
    private ProcessEngine processEngine;
    private String mappingUrl;
    private String workflowDesignTollUrl;

    public ServletRegistrationBean<HttpServlet> getServletRegistrationBean() {
        return new ServletRegistrationBean<>(this, FileUtils.join(this.mappingUrl, "/*"));
    }

    public WorkflowManagementServlet(String mappingUrl, ProcessEngine processEngine, String workflowDesignTollUrl) {
        this(mappingUrl, processEngine);
        this.workflowDesignTollUrl = workflowDesignTollUrl;
    }


    public WorkflowManagementServlet(String mappingUrl, ProcessEngine processEngine) {
        this.repositoryService = processEngine.getRepositoryService();
        this.runtimeService = processEngine.getRuntimeService();
        this.taskService = processEngine.getTaskService();
        this.historyService = processEngine.getHistoryService();
        this.mappingUrl = mappingUrl;
        this.processEngine = processEngine;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String uri = getURI(req);
        Map<String, String[]> params = req.getParameterMap();

        if (uri.endsWith("/showModels")) {
            resp.setHeader("Content-Type", "application/json;charset=utf-8");
            resp.getWriter().write(JSONArray.toJSONString((showModels())));
        } else if (uri.endsWith("/showHistoryProcessInstances")) {
            resp.setHeader("Content-Type", "application/json;charset=utf-8");
            resp.getWriter().write(JSONArray.toJSONString(showHistoryProcessInstances()));
        } else if (uri.endsWith("/completeTask")) {
            completeTask(params.get("taskId")[0], params.get("comment")[0]);
        } else if (uri.endsWith("/showTasks")) {
            resp.setHeader("Content-Type", "application/json;charset=utf-8");
            resp.getWriter().write(JSONArray.toJSONString(showTasks()));
        } else if (uri.endsWith("/showModelXMLDefinition")) {
            resp.setHeader("Content-Type", "text/plain;charset=utf-8");
            resp.getWriter().write(showModelXMLDefinition(params.get("modelId")[0]));
        } else if (uri.endsWith("/showModelPicture")) {
            showModelPicture(resp, params.get("modelId")[0]);
        } else if (uri.endsWith("/deleteModel")) {
            deleteModel(params.get("modelId")[0]);
        } else if (uri.endsWith("/showProcessInstances")) {
            resp.setHeader("Content-Type", "application/json;charset=utf-8");
            resp.getWriter().write(JSONArray.toJSONString((showProcessInstances())));
        } else if (uri.endsWith("/showProcessDefinitions")) {
            resp.setHeader("Content-Type", "application/json;charset=utf-8");
            resp.getWriter().write(JSONArray.toJSONString(showProcessDefinitions()));
        } else if (uri.endsWith("/showDeployments")) {
            resp.setHeader("Content-Type", "application/json;charset=utf-8");
            resp.getWriter().write(JSONArray.toJSONString(showDeployments()));
        } else if (uri.endsWith("/create")) {
            create(params.get("modelName")[0], params.get("description")[0]);
        } else if (uri.endsWith("/deploy")) {
            String ans = deploy(params.get("modelId")[0]);

            resp.getWriter().write(ans);
        } else if (uri.endsWith("/start")) {
            String ans = startProcess(params.get("processDefinitionId")[0]);

            resp.getWriter().write(ans);
        } else if (uri.endsWith("/index.html")) {
            resp.getOutputStream().write(getBytes("/com/zxslsoft/general/activitimanage/index.html"));
        } else if (uri.endsWith("cfg.js")) {
            resp.setHeader("content-type", "application/javascript;charset=utf-8");
            resp.getWriter().write(
                    "'use strict';\n" +
                            "var SETTINGS = SETTINGS || {};\n" +
                            "SETTINGS = {\n" +
                            String.format("\t'contextRoot' : '%s',\n", FileUtils.join(req.getContextPath(), this.mappingUrl)) +
                            String.format("\t'workflowDesignTollUrl' : '%s',\n", FileUtils.join(req.getContextPath(), this.workflowDesignTollUrl)) +
                            "};"
            );
        } else if (uri.endsWith(".js")) {
            resp.setHeader("content-type", "application/javascript;charset=utf-8");
            resp.getOutputStream().write(getBytes(
                    FileUtils.join("/com/zxslsoft/general/activitimanage", uri)
            ));

        } else if (uri.endsWith(".map")) {
            //pass
        } else {
            resp.getOutputStream().write(getBytes(
                    FileUtils.join("/com/zxslsoft/general/activitimanage", uri)
            ));
        }
    }

    @RequestMapping("/showModels")
    public List<Model> showModels() {
        List<Model> modelList = repositoryService.createModelQuery()
                .orderByCreateTime().desc().list();
        return modelList;
    }

    @RequestMapping("/showProcessDefinitions")
    public List<Map<String, Object>> showProcessDefinitions() {
        return convertObjBatch(repositoryService.createProcessDefinitionQuery().list());
    }

    @RequestMapping("/showProcessInstances")
    public List<Map<String, Object>> showProcessInstances() {
        List<ProcessInstance> ans = runtimeService.createProcessInstanceQuery().list();
        return convertObjBatch(ans);
    }

    @RequestMapping("/showDeployments")
    public List<Map<String, Object>> showDeployments() {
        List<Deployment> deployments = repositoryService.createDeploymentQuery().orderByDeploymenTime().desc().list();
        return convertObjBatch(deployments);
    }

    @RequestMapping("/create")
    public void create(String modelName, String description) {
        String key = modelName;
        try {

            Model modelData = repositoryService.newModel();
            modelData.setName(modelName);
            modelData.setKey(StringUtils.defaultString(key));
            modelData.setMetaInfo(objectMapper.createObjectNode()
                    .put(ModelDataJsonConstants.MODEL_NAME, modelName)
                    .put(ModelDataJsonConstants.MODEL_REVISION, 1)
                    .put(ModelDataJsonConstants.MODEL_DESCRIPTION, StringUtils.defaultString(description)).toString()
            );

            repositoryService.saveModel(modelData);
            String randomId = UUID.randomUUID().toString();
            repositoryService.addModelEditorSource(modelData.getId(), Utils.getBytes(objectMapper.createObjectNode()
                    .put("id", randomId)
                    .put("resourceId", randomId)
                    .set("stencilset", objectMapper.createObjectNode()
                            .put("namespace", "http://b3mn.org/stencilset/bpmn2.0#")).toString()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @RequestMapping("/showModelXMLDefinition")
    public String showModelXMLDefinition(String modelId) {
        try {
            Model modelData = this.repositoryService.getModel(modelId);
            ObjectNode modelNode = (ObjectNode) new ObjectMapper().readTree(repositoryService.getModelEditorSource(modelData.getId()));
            byte[] bpmnBytes = null;
            BpmnModel model = new BpmnJsonConverter().convertToBpmnModel(modelNode);
            bpmnBytes = new BpmnXMLConverter().convertToXML(model);
            String processName = modelData.getName() + ".bpmn20.xml";


            return Utils.getString(bpmnBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @RequestMapping(value = "/showModelPicture")
    public void showModelPicture(HttpServletResponse response, String modelId) {
        try {
            //ModelAndView mv=new ModelAndView();
            Model modelData = this.repositoryService.getModel(modelId);
            ObjectNode modelNode = null;
            modelNode = (ObjectNode) new ObjectMapper()
                    .readTree(repositoryService.getModelEditorSource(modelData.getId()));
            BpmnModel model = new BpmnJsonConverter().convertToBpmnModel(modelNode);
            ProcessDiagramGenerator processDiagramGenerator = new DefaultProcessDiagramGenerator();

            InputStream inputStream = processDiagramGenerator.generateDiagram(model, "jpg", new ArrayList<String>(),
                    new ArrayList<String>(), "微软雅黑", "微软雅黑", "微软雅黑", null, 1.1);

            OutputStream out = response.getOutputStream();
            for (int b = -1; (b = inputStream.read()) != -1; ) {
                out.write(b);
            }
            out.close();
            inputStream.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @RequestMapping("/deleteModel")
    public void deleteModel(String modelId) {
        repositoryService.deleteModel(modelId);
    }

    @RequestMapping("/deploy")
    public String deploy(String modelId) {
        try {
            Model modelData = this.repositoryService.getModel(modelId);
            ObjectNode modelNode = (ObjectNode) new ObjectMapper().readTree(repositoryService.getModelEditorSource(modelData.getId()));
            BpmnModel model = new BpmnJsonConverter().convertToBpmnModel(modelNode);
            byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(model);

            Deployment deployment = repositoryService.createDeployment()
                    .name(modelData.getName())
                    .addString(modelData.getName() + ".bpmn20.xml", Utils.getString(bpmnBytes)).deploy();

            return deployment.getId();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @RequestMapping("/start")
    public String startProcess(String processDefinitionId) {
        String businessKey = UUID.randomUUID().toString();
        ProcessInstance processInstance = this.runtimeService.startProcessInstanceById(processDefinitionId, businessKey);
        return processInstance.getBusinessKey();
    }

    @RequestMapping("/showTasks")
    public List<Map<String, Object>> showTasks() {
        List<Task> list = this.taskService.createTaskQuery()
                .orderByTaskCreateTime()
                .desc()
                .list();
        return convertObjBatch(list);
    }

    @RequestMapping("/completeTask")
    public void completeTask(String taskId, String comment) {
        //使用任务ID，查询任务对象，获取流程流程实例ID
        Task task = this.taskService.createTaskQuery()//
                .taskId(taskId)//使用任务ID查询
                .singleResult();
        //获取流程实例ID
        String processInstanceId = task.getProcessInstanceId();
//        Authentication.setAuthenticatedUserId(user);
        this.taskService.addComment(taskId, processInstanceId, comment);
        this.taskService.complete(taskId, Utils.asMap());
    }

    @RequestMapping("/showHistoryProcessInstances")
    public List<Map<String, Object>> showHistoryProcessInstances() {
        List<HistoricProcessInstance> list = historyService.createHistoricProcessInstanceQuery().list();
        return convertObjBatch(list);
    }

    @RequestMapping("/getComment")
    @ResponseBody
    public void getComment() throws Exception {
        String businessKey = "LeaveBill.1";
        HistoricProcessInstance processInstance = this.historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKey(businessKey).singleResult();
        List<Comment> list = this.taskService.getProcessInstanceComments(processInstance.getId());
        for (Comment comment : list) {
            System.out.println(comment.getUserId() + ":" + comment.getFullMessage());
        }
    }


    private String getURI(HttpServletRequest req) {
        String url = req.getRequestURI();
        url = url.replaceFirst(req.getContextPath(), "");
        url = url.replaceFirst(this.mappingUrl, "");
        if (!url.startsWith("/")) url = "/" + url;
        return url;
    }

    private byte[] getBytes(String path) {
        try (
                InputStream inputStream = this.getClass()
                        .getResourceAsStream(path);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()

        ) {
            byte[] buffer = new byte[1024];
            int len = -1;
            while ((len = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> convertObj(Object object) {
        try {
            List<Field> fields = ReflectUtils.getFields(object.getClass(), true);
            Map<String, Object> ans = new HashMap<>();
            for (Field field : Utils.nullSafe(fields)) {
                if (!TypeUtil.isComponentType(field.getType())) {
                    field.setAccessible(true);
                    ans.put(field.getName(), field.get(object));
                }
            }
            return ans;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Map<String, Object>> convertObjBatch(List<?> list) {
        List<Map<String, Object>> ans = new ArrayList<>();
        for (Object o : Utils.nullSafe(list)) {
            ans.add(convertObj(o));
        }
        return ans;
    }
}
