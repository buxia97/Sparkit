package com.sparkit.news.service;

import com.sparkit.common.model.PageQuery;
import com.sparkit.news.model.entity.NewsArticle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 新闻 SEO 服务
 * 生成 Sitemap、Meta 标签等
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsSeoService {

    private final NewsArticleService articleService;

    /**
     * 生成 XML Sitemap
     */
    public String generateSitemap(String baseUrl) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"\n");
        xml.append("        xmlns:news=\"http://www.google.com/schemas/sitemap-news/0.9\">\n");

        // 首页
        xml.append("  <url>\n");
        xml.append("    <loc>").append(baseUrl).append("</loc>\n");
        xml.append("    <changefreq>daily</changefreq>\n");
        xml.append("    <priority>1.0</priority>\n");
        xml.append("  </url>\n");

        // 文章列表页
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPage(1);
        pageQuery.setPageSize(1000);
        var pageResult = articleService.page(pageQuery, null, 1);
        for (NewsArticle article : pageResult.getRecords()) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(baseUrl).append("/news/").append(article.getId()).append("</loc>\n");
            if (article.getPublishTime() != null) {
                xml.append("    <lastmod>").append(article.getPublishTime().format(DateTimeFormatter.ISO_DATE)).append("</lastmod>\n");
            }
            xml.append("    <changefreq>weekly</changefreq>\n");
            xml.append("    <priority>0.8</priority>\n");
            xml.append("    <news:news>\n");
            xml.append("      <news:publication>\n");
            xml.append("        <news:name>").append(escapeXml(article.getSource() != null ? article.getSource() : "Sparkit")).append("</news:name>\n");
            xml.append("      </news:publication>\n");
            if (article.getPublishTime() != null) {
                xml.append("      <news:publication_date>").append(article.getPublishTime().format(DateTimeFormatter.ISO_DATE)).append("</news:publication_date>\n");
            }
            xml.append("      <news:title>").append(escapeXml(article.getTitle())).append("</news:title>\n");
            xml.append("    </news:news>\n");
            xml.append("  </url>\n");
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    /**
     * 生成文章 Meta 标签
     */
    public String generateMetaTags(NewsArticle article) {
        StringBuilder meta = new StringBuilder();
        meta.append("<title>").append(escapeXml(article.getTitle())).append(" - Sparkit</title>\n");
        meta.append("<meta name=\"description\" content=\"").append(escapeXml(article.getSummary() != null ? article.getSummary() : "")).append("\">\n");
        meta.append("<meta name=\"keywords\" content=\"").append(escapeXml(article.getAuthor() != null ? article.getAuthor() : "")).append(",新闻,Sparkit\">\n");
        meta.append("<meta property=\"og:title\" content=\"").append(escapeXml(article.getTitle())).append("\">\n");
        meta.append("<meta property=\"og:description\" content=\"").append(escapeXml(article.getSummary() != null ? article.getSummary() : "")).append("\">\n");
        meta.append("<meta property=\"og:type\" content=\"article\">\n");
        if (article.getPublishTime() != null) {
            meta.append("<meta property=\"article:published_time\" content=\"").append(article.getPublishTime().format(DateTimeFormatter.ISO_DATE)).append("\">\n");
        }
        return meta.toString();
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}