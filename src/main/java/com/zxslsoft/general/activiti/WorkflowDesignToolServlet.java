package com.zxslsoft.general.activiti;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Model;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class WorkflowDesignToolServlet extends HttpServlet implements ModelDataJsonConstants {
    private String mappingUrl;
    private ObjectMapper objectMapper = new ObjectMapper();
    private ProcessEngine processEngine;
    private RepositoryService repositoryService;

    public ServletRegistrationBean<HttpServlet> getServletRegistrationBean() {
        return new ServletRegistrationBean<>(this, FileUtils.join(this.mappingUrl, "/*"));
    }

    public WorkflowDesignToolServlet(String mappingUrl, ProcessEngine processEngine) {
        this.mappingUrl = mappingUrl;
        this.processEngine = processEngine;
        this.repositoryService = processEngine.getRepositoryService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        Map<String, String[]> params = req.getParameterMap();
        String addr = getURI(req);
        if (addr.endsWith("/json")) {
            String modelId = addr.replaceAll("/json", "")
                    .replaceAll("/model/", "");
            ObjectNode ans = getEditorJson(modelId);
            resp.setHeader("Content-Type", "application/json;charset=utf-8");
            resp.getWriter().write(ans.toString());
        } else if (addr.endsWith("/stencilset")) {
            resp.setHeader("Content-Type", "application/json;charset=utf-8");
            resp.getWriter().write(getStencilset());
        } else if (addr.endsWith("/modeler.html")) {
            resp.setHeader("Content-Type", "text/html");
            resp.getOutputStream().write(getBytes("/com/zxslsoft/general/activiti/modeler.html"));
        } else if (addr.endsWith(("/app-cfg.js"))) {
            resp.setHeader("content-type", "application/javascript;charset=utf-8");
            resp.getOutputStream().write(Utils.getBytes(
                    "'use strict';\n" +
                            "var ACTIVITI = ACTIVITI || {};\n" +
                            "ACTIVITI.CONFIG = {\n" +
                            String.format("\t'contextRoot' : '%s',\n", FileUtils.join(req.getContextPath(), this.mappingUrl)) +
                            "};"
            ));
        } else {
            try {
                byte[] ans = getBytes((FileUtils.join("/com/zxslsoft/general/activiti", addr)));
                if (addr.endsWith(".js"))
                    resp.setHeader("content-type", "application/javascript;charset=utf-8");
                else if (addr.endsWith(".woff")) {
                    resp.setHeader("Accept-Ranges", "bytes");
                    resp.setHeader("Content-Length", String.valueOf(ans.length));
                    resp.setHeader("content-type", "application/font-woff");
                }
                resp.getOutputStream().write(ans);
            } catch (Exception e) {
                //pass
            }
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String[]> params = req.getParameterMap();
        String addr = getURI(req);

        if (addr.endsWith("/save")) {
            String modelId = addr.replaceAll("/save", "")
                    .replaceAll("/model/", "");
            saveModel(modelId, params.get("name")[0], params.get("json_xml")[0], params.get("svg_xml")[0], params.get("description")[0]);
            resp.setStatus(200);
        }
    }


    @RequestMapping(value = "/model/{modelId}/save", method = RequestMethod.PUT)
    @ResponseStatus(value = HttpStatus.OK)
    public void saveModel(@PathVariable String modelId, @RequestParam("name") String name,
                          @RequestParam("json_xml") String json_xml, @RequestParam("svg_xml") String svg_xml,
                          @RequestParam("description") String description) {
        try {

            Model model = repositoryService.getModel(modelId);

            ObjectNode modelJson = (ObjectNode) objectMapper.readTree(model.getMetaInfo());

            modelJson.put(MODEL_NAME, name);
            modelJson.put(MODEL_DESCRIPTION, description);
            model.setMetaInfo(modelJson.toString());
            model.setName(name);

            repositoryService.saveModel(model);

            repositoryService.addModelEditorSource(model.getId(), json_xml.getBytes("utf-8"));

            InputStream svgStream = new ByteArrayInputStream(svg_xml.getBytes("utf-8"));
            TranscoderInput input = new TranscoderInput(svgStream);

            PNGTranscoder transcoder = new PNGTranscoder();
            // Setup output
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            TranscoderOutput output = new TranscoderOutput(outStream);

            // Do the transformation
            transcoder.transcode(input, output);
            final byte[] result = outStream.toByteArray();
            repositoryService.addModelEditorSourceExtra(model.getId(), result);
            outStream.close();

        } catch (Exception e) {
            throw new ActivitiException("Error saving model", e);
        }
    }

    @RequestMapping(value = "/editor/stencilset", method = RequestMethod.GET, produces = "application/json;charset=utf-8")
    public @ResponseBody
    String getStencilset() {
        InputStream stencilsetStream = this.getClass().getResourceAsStream("/com/zxslsoft/general/activiti/stencilset.json");
        try {
            return IOUtils.toString(stencilsetStream, "utf-8");
        } catch (Exception e) {
            throw new ActivitiException("Error while loading stencil set", e);
        }
    }

    @RequestMapping(value = "/model/{modelId}/json", method = RequestMethod.GET, produces = "application/json")
    public ObjectNode getEditorJson(@PathVariable String modelId) {
        ObjectNode modelNode = null;

        Model model = repositoryService.getModel(modelId);

        if (model != null) {
            try {
                if (StringUtils.isNotEmpty(model.getMetaInfo())) {
                    modelNode = (ObjectNode) objectMapper.readTree(model.getMetaInfo());
                } else {
                    modelNode = objectMapper.createObjectNode();
                    modelNode.put(MODEL_NAME, model.getName());
                }
                modelNode.put(MODEL_ID, model.getId());
                ObjectNode editorJsonNode = (ObjectNode) objectMapper.readTree(
                        new String(repositoryService.getModelEditorSource(model.getId()), "utf-8"));
                modelNode.put("model", editorJsonNode);

            } catch (Exception e) {
                throw new ActivitiException("Error creating model JSON", e);
            }
        }
        return modelNode;
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
}
