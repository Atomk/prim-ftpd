package org.primftpd.filesystem;

import org.primftpd.pojo.LsOutputBean;
import org.primftpd.pojo.LsOutputParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class RootFile<T> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final LsOutputBean bean;
    private final String absPath;
    protected final String name;

    public RootFile(LsOutputBean bean, String absPath) {
        this.bean = bean;
        this.absPath = absPath;
        this.name = bean.getName();
    }

    protected abstract T createFile(LsOutputBean bean, String absPath);

    public String getAbsolutePath() {
        logger.trace("[{}] getAbsolutePath() -> '{}'", name, absPath);
        return absPath;
    }

    public String getName() {
        logger.trace("[{}] getName()", name);
        return name;
    }

    public boolean isDirectory() {
        logger.trace("[{}] isDirectory() -> {}", name, bean.isDir());
        return bean.isDir();
    }

    public boolean isFile() {
        logger.trace("[{}] isFile() -> {}", name, bean.isFile());
        return bean.isFile();
    }

    public boolean doesExist() {
        logger.trace("[{}] doesExist() -> {}", name, bean.isExists());
        return bean.isExists();
    }

    public boolean isReadable() {
        logger.trace("[{}] isReadable()", name);
        return true;
    }

    public boolean isWritable() {
        logger.trace("[{}] isWritable()", name);
        return true;
    }

    public boolean isRemovable() {
        logger.trace("[{}] isRemovable()", name);
        return true;
    }

    public long getLastModified() {
        long time = bean.getDate().getTime();
        logger.trace("[{}] getLastModified() -> {}", name, time);
        return time;
    }

    public boolean setLastModified(long time) {
        logger.trace("[{}] setLastModified({})", name, time);
        // TODO root setLastModified()
        return false;
    }

    public long getSize() {
        logger.trace("[{}] getSize() -> {}", name, bean.getSize());
        return bean.getSize();
    }

    public boolean mkdir() {
        logger.trace("[{}] mkdir()", name);
        return runCommand(new String[]{"mkdir", absPath});
    }

    public boolean delete() {
        logger.trace("[{}] delete()", name);
        return runCommand(new String[]{"rm", "-rf", absPath});
    }

    public boolean move(RootFile<T> destination) {
        logger.trace("[{}] move({})", name, destination.getAbsolutePath());
        return runCommand(new String[]{"mv", absPath, destination.getAbsolutePath()});
    }

    public List<T> listFiles() {
        logger.trace("[{}] listFiles()", name);

        List<T> result = new ArrayList<>();
        LsOutputParser parser = new LsOutputParser();
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("su", "-c", "ls", "-lA", absPath);
        try {
            Process proc = processBuilder.start();
            List<LsOutputBean> beans = parser.parse(proc.getInputStream());
            for (LsOutputBean bean : beans) {
                String path = absPath + "/" + bean.getName();
                result.add(createFile(bean, path));
            }
        } catch (IOException e) {
            logger.error("could not run su", e);
        }

        return result;
    }

    public OutputStream createOutputStream(long offset) throws IOException {
        logger.trace("[{}] createOutputStream(offset: {})", name, offset);
        // TODO root createOutputStream()
        return null;
    }

    public InputStream createInputStream(long offset) throws IOException {
        logger.trace("[{}] createInputStream(offset: {})", name, offset);

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("su", "-c", "cat", absPath);
        Process proc = processBuilder.start();
        return proc.getInputStream();
    }

    protected boolean runCommand(String[] cmd) {
        String[] suCmd = new String[]{"su", "-c"};
        String[] wholeCmd = new String[suCmd.length + cmd.length];
        System.arraycopy(suCmd, 0, wholeCmd, 0, suCmd.length);
        System.arraycopy(cmd, 0, wholeCmd, suCmd.length, cmd.length);
        logger.trace("running cmd: '{}'", Arrays.toString(wholeCmd));
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(wholeCmd);
        try {
            Process proc = processBuilder.start();
            proc.waitFor();
            return proc.exitValue() == 0;
        } catch (Exception e) {
            logger.error("could not run command", e);
        }
        return false;
    }
}
