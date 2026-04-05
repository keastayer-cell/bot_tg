package com.example.bot;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Controller
public class InfoController {

    @GetMapping(value = "/info", produces = MediaType.TEXT_MARKDOWN_VALUE)
    @ResponseBody
    public String getInfo() throws IOException {
        Resource resource = new ClassPathResource("INFO_LOG.md");
        try (InputStream is = resource.getInputStream()) {
            return StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        }
    }

    @GetMapping(value = "/info.html", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String getInfoHtml() throws IOException {
        Resource resource = new ClassPathResource("INFO_LOG.md");
        String content;
        try (InputStream is = resource.getInputStream()) {
            content = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        }
        
        // Простой конвертер Markdown в HTML
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Bot Info</title></head><body>";
        html += "<pre style='white-space: pre-wrap; word-wrap: break-word; font-family: monospace; font-size: 14px; line-height: 1.5;'>";
        html += content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        html += "</pre></body></html>";
        return html;
    }
}
