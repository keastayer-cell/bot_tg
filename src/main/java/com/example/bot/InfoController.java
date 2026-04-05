package com.example.bot;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
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

    private static final String CSS_STYLE = """
            <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', sans-serif;
                line-height: 1.6;
                color: #333;
                background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
                min-height: 100vh;
                padding: 20px;
            }
            .container {
                max-width: 900px;
                margin: 0 auto;
                background: white;
                border-radius: 10px;
                box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                padding: 40px;
                overflow-x: auto;
            }
            h1 { color: #2c3e50; margin: 40px 0 20px 0; border-bottom: 3px solid #3498db; padding-bottom: 10px; font-size: 2em; }
            h2 { color: #34495e; margin: 30px 0 15px 0; border-left: 4px solid #3498db; padding-left: 15px; font-size: 1.5em; }
            h3 { color: #7f8c8d; margin: 20px 0 10px 0; font-size: 1.2em; }
            p { margin: 15px 0; }
            ul, ol { margin: 15px 0 15px 25px; }
            li { margin: 8px 0; }
            table { border-collapse: collapse; width: 100%; margin: 20px 0; }
            th { background: #3498db; color: white; padding: 12px; text-align: left; font-weight: bold; }
            td { border: 1px solid #ddd; padding: 12px; }
            tr:nth-child(even) { background: #f9f9f9; }
            code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; font-family: 'Courier New', monospace; color: #c7254e; }
            pre { background: #2c3e50; color: #ecf0f1; padding: 15px; border-radius: 5px; overflow-x: auto; margin: 15px 0; font-size: 0.9em; line-height: 1.4; }
            pre code { background: none; color: #ecf0f1; padding: 0; }
            blockquote { border-left: 4px solid #3498db; padding-left: 15px; margin-left: 0; color: #7f8c8d; font-style: italic; }
            a { color: #3498db; text-decoration: none; }
            a:hover { text-decoration: underline; }
            strong { color: #2c3e50; font-weight: bold; }
            em { font-style: italic; }
            .info-box { background: #e8f4f8; border-left: 4px solid #3498db; padding: 15px; margin: 20px 0; border-radius: 5px; }
            .warning-box { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 5px; }
            hr { border: none; height: 2px; background: #ecf0f1; margin: 30px 0; }
            </style>
            """;

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
        String markdownContent;
        try (InputStream is = resource.getInputStream()) {
            markdownContent = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
        }
        
        // Парсим Markdown в HTML с помощью flexmark
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        Document document = parser.parse(markdownContent);
        String htmlContent = renderer.render(document);
        
        // Оборачиваем в красивый HTML шаблон
        String html = """
                <!DOCTYPE html>
                <html lang="ru">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Telegram Bot - Инструкции и документация</title>
                    %s
                </head>
                <body>
                    <div class="container">
                        %s
                    </div>
                </body>
                </html>
                """.formatted(CSS_STYLE, htmlContent);
        
        return html;
    }
}
