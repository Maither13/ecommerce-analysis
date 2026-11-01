package com.example.ecommerce.integration.hadoop;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Service
public class HadoopSshService {

    private static final Logger log = LoggerFactory.getLogger(HadoopSshService.class);

    @Value("${hadoop.vm.host}")
    private String host;

    @Value("${hadoop.vm.port:22}")
    private int port;

    @Value("${hadoop.vm.username}")
    private String username;

    @Value("${hadoop.vm.password}")
    private String password;

    @Value("${hadoop.jar.path}")
    private String jarPath;

    @Value("${hadoop.input.path}")
    private String inputPath;

    @Value("${hadoop.output.base-path:/output}")
    private String outputBasePath;

    /**
     * 通过 SSH 在 VM 上执行命令，返回 stdout
     */
    public String executeCommand(String command) throws Exception {
        log.info("[SSH] {}", command);
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(60000);

        try {
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            // source profile 确保 HADOOP_HOME/PATH 已设置
            channel.setCommand("source /etc/profile && source ~/.bashrc 2>/dev/null; " + command);
            channel.setInputStream(null);

            ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
            ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
            channel.setOutputStream(stdOut);
            channel.setErrStream(stdErr);

            channel.connect();
            // 等待命令执行完毕
            while (!channel.isClosed()) {
                Thread.sleep(300);
            }

            int exitCode = channel.getExitStatus();
            channel.disconnect();

            String out = stdOut.toString("UTF-8");
            String err = stdErr.toString("UTF-8");

            log.info("[SSH] exit={} stdout.len={}", exitCode, out.length());
            if (!err.isEmpty()) {
                log.debug("[SSH] stderr={}", err.substring(0, Math.min(err.length(), 500)));
            }

            // hadoop jar 成功时 exit=0；但 Hadoop 大量日志走 stderr，不应视为错误
            if (exitCode != 0) {
                throw new RuntimeException("SSH command failed (exit " + exitCode + "): "
                        + err.substring(0, Math.min(err.length(), 1000)));
            }
            return out;
        } finally {
            session.disconnect();
        }
    }

    /**
     * 在 VM 上运行 MapReduce 任务
     *
     * @param mainClass  Driver 全限定类名
     * @param outputPath HDFS 输出目录
     */
    public void runMapReduceJob(String mainClass, String outputPath) throws Exception {
        // 若输出目录已存在则先删除（Driver 内部也会删，但先删更安全）
        try {
            executeCommand("hdfs dfs -rm -r -f " + outputPath);
        } catch (Exception ignored) {
            // 目录不存在时会报错，忽略
        }

        String cmd = String.format("hadoop jar %s %s %s %s",
                jarPath, mainClass, inputPath, outputPath);
        executeCommand(cmd);
    }

    /**
     * 读取 HDFS 文件内容
     */
    public String readHdfsFile(String hdfsPath) throws Exception {
        return executeCommand("hdfs dfs -cat " + hdfsPath);
    }

    public String getOutputBasePath() {
        return outputBasePath;
    }

    // =========================================================
    // HDFS 文件管理
    // =========================================================

    /**
     * 上传本地文件流到 HDFS
     * 步骤：SFTP 传到 VM 临时目录 → hdfs dfs -mkdir -p → hdfs dfs -put -f
     *
     * @param fileStream  文件输入流
     * @param hdfsDir     目标 HDFS 目录（如 /input/user_behavior）
     * @param fileName    文件名
     */
    public void uploadToHdfs(InputStream fileStream, String hdfsDir, String fileName) throws Exception {
        String tmpPath = "/tmp/hdfs_upload_" + System.currentTimeMillis() + "_" + fileName;
        String hdfsFullPath = hdfsDir.replaceAll("/+$", "") + "/" + fileName;

        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(60000);

        try {
            // 1. SFTP 上传到 VM 临时目录
            ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
            sftp.put(fileStream, tmpPath);
            sftp.disconnect();
            log.info("[SFTP] Uploaded to VM: {}", tmpPath);

            // 2. 在 HDFS 上创建目录
            runViaExec(session, "source /etc/profile && source ~/.bashrc 2>/dev/null; "
                    + "hdfs dfs -mkdir -p " + hdfsDir);

            // 3. 推送到 HDFS（-f 覆盖已存在的同名文件）
            runViaExec(session, "source /etc/profile && source ~/.bashrc 2>/dev/null; "
                    + "hdfs dfs -put -f " + tmpPath + " " + hdfsFullPath);
            log.info("[HDFS] File pushed: {}", hdfsFullPath);

            // 4. 删除 VM 临时文件
            runViaExec(session, "rm -f " + tmpPath);
        } finally {
            session.disconnect();
        }
    }

    /**
     * 列出 HDFS 目录内容，返回 ls -l 输出
     */
    public String listHdfsDir(String hdfsPath) throws Exception {
        return executeCommand("hdfs dfs -ls " + hdfsPath);
    }

    /**
     * 在 HDFS 上创建目录
     */
    public void mkdirHdfs(String hdfsPath) throws Exception {
        executeCommand("hdfs dfs -mkdir -p " + hdfsPath);
    }

    /**
     * 删除 HDFS 文件或目录
     */
    public void deleteHdfsPath(String hdfsPath) throws Exception {
        executeCommand("hdfs dfs -rm -r -f " + hdfsPath);
    }

    /** 复用已有 Session 执行命令（内部辅助方法） */
    private void runViaExec(Session session, String command) throws Exception {
        ChannelExec ch = (ChannelExec) session.openChannel("exec");
        ch.setCommand(command);
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        ch.setErrStream(err);
        ch.connect();
        while (!ch.isClosed()) Thread.sleep(200);
        int code = ch.getExitStatus();
        ch.disconnect();
        if (code != 0) {
            throw new RuntimeException("Command failed (exit " + code + "): " + err.toString("UTF-8"));
        }
    }
}
