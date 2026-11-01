package com.example.ecommerce.integration.hadoop;

import com.example.ecommerce.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Tag(name = "HDFS管理", description = "HDFS 文件上传、目录浏览、删除等操作")
@RestController
@RequestMapping("/hdfs")
public class HdfsController {

    @Resource
    private HadoopSshService hadoopSshService;

    /**
     * 上传文件到 HDFS
     */
    @Operation(summary = "上传文件到 HDFS")
    @PostMapping("/upload")
    public Result<String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "hdfsDir", defaultValue = "/input/user_behavior") String hdfsDir) {
        try {
            String fileName = file.getOriginalFilename();
            hadoopSshService.uploadToHdfs(file.getInputStream(), hdfsDir, fileName);
            String fullPath = hdfsDir.replaceAll("/+$", "") + "/" + fileName;
            return Result.success("上传成功: " + fullPath);
        } catch (Exception e) {
            return Result.error(500, "上传失败: " + e.getMessage());
        }
    }

    /**
     * 列出 HDFS 目录内容
     */
    @Operation(summary = "列出 HDFS 目录")
    @GetMapping("/list")
    public Result<List<Map<String, String>>> list(
            @RequestParam(value = "path", defaultValue = "/") String path) {
        try {
            String raw = hadoopSshService.listHdfsDir(path);
            List<Map<String, String>> files = parseLsOutput(raw);
            return Result.success(files);
        } catch (Exception e) {
            // 目录不存在时返回空列表
            return Result.success(Collections.emptyList());
        }
    }

    /**
     * 在 HDFS 上创建目录
     */
    @Operation(summary = "创建 HDFS 目录")
    @PostMapping("/mkdir")
    public Result<String> mkdir(@RequestParam("path") String path) {
        try {
            hadoopSshService.mkdirHdfs(path);
            return Result.success("目录创建成功: " + path);
        } catch (Exception e) {
            return Result.error(500, "创建失败: " + e.getMessage());
        }
    }

    /**
     * 删除 HDFS 文件或目录
     */
    @Operation(summary = "删除 HDFS 文件或目录")
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam("path") String path) {
        try {
            hadoopSshService.deleteHdfsPath(path);
            return Result.success("删除成功: " + path);
        } catch (Exception e) {
            return Result.error(500, "删除失败: " + e.getMessage());
        }
    }

    /**
     * 解析 hdfs dfs -ls 输出为结构化列表
     * 每行格式: -rw-r--r--   3 hadoop supergroup 4581429 2026-06-16 01:17 /input/user_behavior/data.csv
     */
    private List<Map<String, String>> parseLsOutput(String raw) {
        List<Map<String, String>> result = new ArrayList<>();
        for (String line : raw.split("\n")) {
            if (line.trim().isEmpty() || line.startsWith("Found") || line.startsWith("WARNING")) continue;
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 8) continue;
            Map<String, String> item = new LinkedHashMap<>();
            item.put("permission", parts[0]);
            item.put("replication", parts[1]);
            item.put("owner", parts[2]);
            item.put("group", parts[3]);
            item.put("size", parts[4]);
            item.put("date", parts[5]);
            item.put("time", parts[6]);
            item.put("path", parts[7]);
            item.put("name", parts[7].substring(parts[7].lastIndexOf('/') + 1));
            item.put("type", parts[0].startsWith("d") ? "directory" : "file");
            result.add(item);
        }
        return result;
    }
}
