package com.sparkit.generator.service;

import com.sparkit.generator.model.entity.GenTable;
import com.sparkit.generator.model.entity.GenTableColumn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器模板引擎
 * 基于模板变量替换生成 Java 实体、Mapper、Service、Controller、Vue 页面等
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenTemplateEngine {

    /**
     * 生成代码并打包为 ZIP
     */
    public byte[] generateAndZip(GenTable table, List<GenTableColumn> columns) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            String basePackage = "com.sparkit." + toCamelCase(table.getTableName().replace("sparkit_", ""));
            String moduleName = toCamelCase(table.getTableName().replace("sparkit_", ""));
            String className = toCamelCase(table.getTableName());
            String classComment = table.getTableComment() != null ? table.getTableComment() : className;

            Map<String, String> vars = buildTemplateVars(table, columns, basePackage, moduleName, className, classComment);

            // Entity
            addToZip(zos, "java/" + basePackage.replace('.', '/') + "/model/entity/" + className + ".java",
                    renderEntity(className, classComment, table.getTableName(), columns, vars));

            // Mapper
            addToZip(zos, "java/" + basePackage.replace('.', '/') + "/mapper/" + className + "Mapper.java",
                    renderMapper(className, basePackage, vars));

            // Service
            addToZip(zos, "java/" + basePackage.replace('.', '/') + "/service/" + className + "Service.java",
                    renderService(className, basePackage, classComment, vars));

            // Controller
            addToZip(zos, "java/" + basePackage.replace('.', '/') + "/controller/" + className + "Controller.java",
                    renderController(className, basePackage, moduleName, classComment));

            // Vue page
            addToZip(zos, "vue/" + toKebabCase(className) + "/index.vue",
                    renderVuePage(className, moduleName, classComment, columns, vars));

            // Vue API
            addToZip(zos, "vue/api/" + toKebabCase(className) + ".js",
                    renderVueApi(className, moduleName));

            // SQL
            addToZip(zos, "sql/" + table.getTableName() + "_menu.sql",
                    renderMenuSql(className, moduleName, classComment));
        }
        return bos.toByteArray();
    }

    private Map<String, String> buildTemplateVars(GenTable table, List<GenTableColumn> columns,
                                                   String basePackage, String moduleName, String className, String classComment) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("className", className);
        vars.put("classComment", classComment);
        vars.put("tableName", table.getTableName());
        vars.put("basePackage", basePackage);
        vars.put("moduleName", moduleName);
        vars.put("author", System.getProperty("user.name", "Sparkit"));
        vars.put("date", java.time.LocalDate.now().toString());
        return vars;
    }

    private String renderEntity(String className, String comment, String tableName,
                                 List<GenTableColumn> columns, Map<String, String> vars) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(vars.get("basePackage")).append(".model.entity;\n\n");
        sb.append("import com.baomidou.mybatisplus.annotation.*;\n");
        sb.append("import com.sparkit.common.model.BaseEntity;\n");
        sb.append("import lombok.Data;\n");
        sb.append("import lombok.EqualsAndHashCode;\n\n");
        sb.append("import java.math.BigDecimal;\n");
        sb.append("import java.time.LocalDateTime;\n\n");
        sb.append("/**\n * ").append(comment).append("\n */\n");
        sb.append("@Data\n");
        sb.append("@EqualsAndHashCode(callSuper = true)\n");
        sb.append("@TableName(\"").append(tableName).append("\")\n");
        sb.append("public class ").append(className).append(" extends BaseEntity {\n\n");
        sb.append("    private static final long serialVersionUID = 1L;\n\n");
        for (GenTableColumn col : columns) {
            if (col.getIsPk() != null && col.getIsPk() == 1) {
                sb.append("    @TableId(type = IdType.ASSIGN_ID)\n");
            }
            sb.append("    private ").append(mapJavaType(col.getColumnType())).append(" ").append(toCamelCase(col.getColumnName())).append(";\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private String renderMapper(String className, String basePackage, Map<String, String> vars) {
        return "package " + basePackage + ".mapper;\n\n" +
                "import com.baomidou.mybatisplus.core.mapper.BaseMapper;\n" +
                "import " + basePackage + ".model.entity." + className + ";\n" +
                "import org.apache.ibatis.annotations.Mapper;\n\n" +
                "/**\n * " + className + " Mapper\n */\n" +
                "@Mapper\n" +
                "public interface " + className + "Mapper extends BaseMapper<" + className + "> {\n" +
                "}\n";
    }

    private String renderService(String className, String basePackage, String comment, Map<String, String> vars) {
        return "package " + basePackage + ".service;\n\n" +
                "import com.baomidou.mybatisplus.core.metadata.IPage;\n" +
                "import com.baomidou.mybatisplus.extension.plugins.pagination.Page;\n" +
                "import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;\n" +
                "import com.sparkit.common.model.PageQuery;\n" +
                "import com.sparkit.common.model.PageResult;\n" +
                "import " + basePackage + ".mapper." + className + "Mapper;\n" +
                "import " + basePackage + ".model.entity." + className + ";\n" +
                "import lombok.RequiredArgsConstructor;\n" +
                "import lombok.extern.slf4j.Slf4j;\n" +
                "import org.springframework.stereotype.Service;\n" +
                "import org.springframework.transaction.annotation.Transactional;\n\n" +
                "/**\n * " + comment + " 服务\n */\n" +
                "@Slf4j\n@Service\n@RequiredArgsConstructor\n" +
                "public class " + className + "Service extends ServiceImpl<" + className + "Mapper, " + className + "> {\n\n" +
                "    public PageResult<" + className + "> page(PageQuery query) {\n" +
                "        IPage<" + className + "> page = new Page<>(query.getPage(), query.getPageSize());\n" +
                "        IPage<" + className + "> result = lambdaQuery().orderByDesc(" + className + "::getCreateTime).page(page);\n" +
                "        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());\n" +
                "    }\n\n" +
                "    @Transactional\n" +
                "    public void create(" + className + " entity) {\n" +
                "        save(entity);\n" +
                "    }\n\n" +
                "    @Transactional\n" +
                "    public void update(" + className + " entity) {\n" +
                "        updateById(entity);\n" +
                "    }\n" +
                "}\n";
    }

    private String renderController(String className, String basePackage, String moduleName, String comment) {
        String basePath = "/api/v1/admin/" + toKebabCase(moduleName);
        return "package " + basePackage + ".controller;\n\n" +
                "import com.sparkit.common.model.PageQuery;\n" +
                "import com.sparkit.common.model.PageResult;\n" +
                "import com.sparkit.common.model.R;\n" +
                "import " + basePackage + ".model.entity." + className + ";\n" +
                "import " + basePackage + ".service." + className + "Service;\n" +
                "import lombok.RequiredArgsConstructor;\n" +
                "import org.springframework.web.bind.annotation.*;\n\n" +
                "/**\n * " + comment + " 控制器\n */\n" +
                "@RestController\n@RequestMapping(\"" + basePath + "\")\n@RequiredArgsConstructor\n" +
                "public class " + className + "Controller {\n\n" +
                "    private final " + className + "Service service;\n\n" +
                "    @GetMapping\n" +
                "    public R<PageResult<" + className + ">> list(PageQuery query) {\n" +
                "        return R.ok(service.page(query));\n" +
                "    }\n\n" +
                "    @GetMapping(\"/{id}\")\n" +
                "    public R<" + className + "> getById(@PathVariable Long id) {\n" +
                "        return R.ok(service.getById(id));\n" +
                "    }\n\n" +
                "    @PostMapping\n" +
                "    public R<?> create(@RequestBody " + className + " entity) {\n" +
                "        service.create(entity);\n" +
                "        return R.ok();\n" +
                "    }\n\n" +
                "    @PutMapping(\"/{id}\")\n" +
                "    public R<?> update(@PathVariable Long id, @RequestBody " + className + " entity) {\n" +
                "        entity.setId(id);\n" +
                "        service.update(entity);\n" +
                "        return R.ok();\n" +
                "    }\n\n" +
                "    @DeleteMapping(\"/{id}\")\n" +
                "    public R<?> delete(@PathVariable Long id) {\n" +
                "        service.removeById(id);\n" +
                "        return R.ok();\n" +
                "    }\n" +
                "}\n";
    }

    private String renderVuePage(String className, String moduleName, String comment,
                                  List<GenTableColumn> columns, Map<String, String> vars) {
        String kebabName = toKebabCase(className);
        StringBuilder sb = new StringBuilder();
        sb.append("<template>\n  <div>\n");
        sb.append("    <div class=\"page-header\">\n");
        sb.append("      <div class=\"page-title\">").append(comment).append("</div>\n");
        sb.append("    </div>\n\n");
        sb.append("    <el-card>\n      <div class=\"toolbar\">\n");
        sb.append("        <div class=\"toolbar-left\">\n");
        sb.append("          <el-button type=\"primary\" @click=\"handleAdd\">\n");
        sb.append("            <el-icon><Plus /></el-icon>新增\n");
        sb.append("          </el-button>\n        </div>\n      </div>\n\n");
        sb.append("      <el-table :data=\"tableData\" v-loading=\"loading\" stripe>\n");
        sb.append("        <el-table-column prop=\"id\" label=\"ID\" width=\"80\" />\n");
        for (GenTableColumn col : columns) {
            if (!"id".equals(col.getColumnName()) && !"create_time".equals(col.getColumnName())
                    && !"update_time".equals(col.getColumnName()) && !"deleted".equals(col.getColumnName())) {
                sb.append("        <el-table-column prop=\"").append(toCamelCase(col.getColumnName())).append("\" label=\"").append(col.getColumnComment() != null ? col.getColumnComment() : col.getColumnName()).append("\" min-width=\"150\" />\n");
            }
        }
        sb.append("        <el-table-column prop=\"createTime\" label=\"创建时间\" width=\"170\" />\n");
        sb.append("        <el-table-column label=\"操作\" width=\"150\" fixed=\"right\">\n");
        sb.append("          <template #default=\"{ row }\">\n");
        sb.append("            <el-button type=\"primary\" link size=\"small\" @click=\"handleEdit(row)\">编辑</el-button>\n");
        sb.append("            <el-button type=\"danger\" link size=\"small\" @click=\"handleDelete(row)\">删除</el-button>\n");
        sb.append("          </template>\n        </el-table-column>\n");
        sb.append("      </el-table>\n");
        sb.append("      <el-pagination v-model:current-page=\"page\" v-model:page-size=\"pageSize\" :total=\"total\"\n");
        sb.append("        :page-sizes=\"[10, 20, 50]\" layout=\"total, sizes, prev, pager, next, jumper\"\n");
        sb.append("        @size-change=\"fetchData\" @current-change=\"fetchData\" />\n");
        sb.append("    </el-card>\n\n");

        // Dialog
        sb.append("    <el-dialog v-model=\"dialogVisible\" :title=\"isEdit ? '编辑' : '新增'\" width=\"550px\" destroy-on-close>\n");
        sb.append("      <el-form ref=\"formRef\" :model=\"formData\" label-width=\"100px\">\n");
        for (GenTableColumn col : columns) {
            if (!"id".equals(col.getColumnName()) && !"create_time".equals(col.getColumnName())
                    && !"update_time".equals(col.getColumnName()) && !"deleted".equals(col.getColumnName())) {
                sb.append("        <el-form-item label=\"").append(col.getColumnComment() != null ? col.getColumnComment() : col.getColumnName()).append("\">\n");
                sb.append("          <el-input v-model=\"formData.").append(toCamelCase(col.getColumnName())).append("\" />\n");
                sb.append("        </el-form-item>\n");
            }
        }
        sb.append("      </el-form>\n      <template #footer>\n");
        sb.append("        <el-button @click=\"dialogVisible = false\">取消</el-button>\n");
        sb.append("        <el-button type=\"primary\" @click=\"handleSubmit\">确定</el-button>\n");
        sb.append("      </template>\n    </el-dialog>\n  </div>\n</template>\n\n");

        sb.append("<script setup>\nimport { Plus } from '@element-plus/icons-vue'\n");
        sb.append("import { ElMessage, ElMessageBox } from 'element-plus'\n\n");
        sb.append("const auth = useAuthStore()\n\n");
        sb.append("const tableData = ref([])\nconst loading = ref(false)\nconst page = ref(1)\nconst pageSize = ref(10)\nconst total = ref(0)\n\n");
        sb.append("const fetchData = async () => {\n  loading.value = true\n  try {\n    const { data } = await useFetch('/api/v1/admin/").append(kebabName).append("', {\n");
        sb.append("      params: { page: page.value, pageSize: pageSize.value },\n");
        sb.append("      headers: { Authorization: `Bearer ${auth.token}` }\n    })\n");
        sb.append("    if (data.value?.code === 200) {\n      tableData.value = data.value.data.records || []\n      total.value = data.value.data.total || 0\n    }\n");
        sb.append("  } catch (e) { ElMessage.error('获取列表失败') } finally { loading.value = false }\n}\n\n");
        sb.append("fetchData()\n\n");

        sb.append("const dialogVisible = ref(false)\nconst isEdit = ref(false)\n");
        sb.append("const formData = reactive({})\n\n");
        sb.append("const handleAdd = () => { isEdit.value = false; Object.assign(formData, {}); dialogVisible.value = true }\n");
        sb.append("const handleEdit = (row) => { isEdit.value = true; Object.assign(formData, row); dialogVisible.value = true }\n\n");
        sb.append("const handleSubmit = async () => {\n  const url = isEdit.value ? `/api/v1/admin/").append(kebabName).append("/${formData.id}` : '/api/v1/admin/").append(kebabName).append("'\n");
        sb.append("  const method = isEdit.value ? 'PUT' : 'POST'\n");
        sb.append("  await useFetch(url, { method, body: formData, headers: { Authorization: `Bearer ${auth.token}` } })\n");
        sb.append("  ElMessage.success(isEdit.value ? '更新成功' : '创建成功')\n");
        sb.append("  dialogVisible.value = false; fetchData()\n}\n\n");
        sb.append("const handleDelete = (row) => {\n  ElMessageBox.confirm('确定删除？', '删除确认', { type: 'warning' }).then(async () => {\n");
        sb.append("    await useFetch(`/api/v1/admin/").append(kebabName).append("/${row.id}`, { method: 'DELETE', headers: { Authorization: `Bearer ${auth.token}` } })\n");
        sb.append("    ElMessage.success('删除成功'); fetchData()\n  }).catch(() => {})\n}\n");
        sb.append("</script>\n");
        return sb.toString();
    }

    private String renderVueApi(String className, String moduleName) {
        String kebab = toKebabCase(moduleName);
        return "import { useFetch } from 'nuxt/app'\n\n" +
                "export const " + toCamelCase(className) + "Api = {\n" +
                "  list: (params) => useFetch('/api/v1/admin/" + kebab + "', { params }),\n" +
                "  getById: (id) => useFetch('/api/v1/admin/" + kebab + "/' + id),\n" +
                "  create: (data) => useFetch('/api/v1/admin/" + kebab + "', { method: 'POST', body: data }),\n" +
                "  update: (id, data) => useFetch('/api/v1/admin/" + kebab + "/' + id, { method: 'PUT', body: data }),\n" +
                "  delete: (id) => useFetch('/api/v1/admin/" + kebab + "/' + id, { method: 'DELETE' })\n" +
                "}\n";
    }

    private String renderMenuSql(String className, String moduleName, String comment) {
        return "-- " + comment + " 菜单SQL\n" +
                "INSERT INTO sparkit_menu (parent_id, name, path, component, icon, sort, status, create_time)\n" +
                "VALUES (0, '" + comment + "', '/" + toKebabCase(moduleName) + "', '" + toKebabCase(moduleName) + "/index', 'Document', 0, 1, NOW());\n";
    }

    private void addToZip(ZipOutputStream zos, String path, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(path));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private String mapJavaType(String columnType) {
        if (columnType == null) return "String";
        return switch (columnType.toLowerCase()) {
            case "bigint" -> "Long";
            case "int", "integer", "tinyint", "smallint" -> "Integer";
            case "decimal", "double", "float", "numeric" -> "BigDecimal";
            case "datetime", "timestamp", "date", "time" -> "LocalDateTime";
            case "text", "longtext", "mediumtext" -> "String";
            default -> "String";
        };
    }

    private String toCamelCase(String name) {
        if (name == null || name.isBlank()) return "";
        name = name.replace("sparkit_", "");
        StringBuilder sb = new StringBuilder();
        boolean upper = true;
        for (char c : name.toCharArray()) {
            if (c == '_') { upper = true; continue; }
            sb.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
        }
        return sb.toString();
    }

    private String toKebabCase(String name) {
        if (name == null || name.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isUpperCase(c)) {
                if (sb.length() > 0) sb.append('-');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}