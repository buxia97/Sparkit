package com.sparkit.system.service;

import com.sparkit.common.model.R;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 数据导入导出服务
 * 支持 Excel / CSV 格式的数据导入导出
 */
@Slf4j
@Service
public class ImportExportService {

    /**
     * 导出为 CSV
     */
    public void exportCsv(String fileName, List<String> headers, List<List<String>> data, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" +
                URLEncoder.encode(fileName, StandardCharsets.UTF_8) + ".csv\"");
        response.setCharacterEncoding("UTF-8");

        // 写入 BOM 头，确保 Excel 正确识别 UTF-8
        OutputStream os = response.getOutputStream();
        os.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        StringBuilder sb = new StringBuilder();
        // 写入表头
        sb.append(String.join(",", headers)).append("\n");
        // 写入数据
        for (List<String> row : data) {
            sb.append(String.join(",", row)).append("\n");
        }
        os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        os.flush();
    }

    /**
     * 导出为 Excel（使用 HTML 表格格式，兼容性好）
     */
    public void exportExcel(String fileName, List<String> headers, List<List<String>> data, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.ms-excel; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" +
                URLEncoder.encode(fileName, StandardCharsets.UTF_8) + ".xls\"");
        response.setCharacterEncoding("UTF-8");

        OutputStream os = response.getOutputStream();
        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset='UTF-8'><style>")
                .append("td{border:1px solid #ccc;padding:6px 10px;}")
                .append("th{background:#f0f0f0;border:1px solid #ccc;padding:6px 10px;}")
                .append("</style></head><body><table>");

        // 表头
        html.append("<tr>");
        for (String h : headers) {
            html.append("<th>").append(escapeHtml(h)).append("</th>");
        }
        html.append("</tr>");

        // 数据行
        for (List<String> row : data) {
            html.append("<tr>");
            for (String cell : row) {
                html.append("<td>").append(escapeHtml(cell != null ? cell : "")).append("</td>");
            }
            html.append("</tr>");
        }

        html.append("</table></body></html>");
        os.write(html.toString().getBytes(StandardCharsets.UTF_8));
        os.flush();
    }

    /**
     * 导入 CSV 文件解析
     */
    public List<String[]> parseCsv(MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        // 去除 BOM
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }
        return content.lines()
                .map(line -> line.split(","))
                .toList();
    }

    /**
     * 文件导入模板下载
     */
    public void downloadTemplate(String fileName, List<String> headers, String module, HttpServletResponse response) throws IOException {
        exportCsv(module + "_" + fileName + "_导入模板", headers, List.of(), response);
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}