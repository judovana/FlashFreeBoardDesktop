/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.fbb.board.Translator;
import org.fbb.board.desktop.Files;
import org.fbb.board.desktop.gui.GitAuthenticator;

/**
 *
 * @author jvanek
 */
public class DB {

    //git clone -b Flash --single-branch https://github.com/FlashFreeBoard/FreeBoard.git
    private Git db = null;
    private final GlobalSettings gs;

    public DB(GlobalSettings gs) {
        this.gs = gs;
    }

    private synchronized Git getDB() throws IOException {
        if (Files.repoGit.exists()) {
            if (db == null) {
                Repository repository = FileRepositoryBuilder.create(Files.repoGit);
                db = new Git(repository);
            }
        }
        return db;
    }

    public synchronized void delte(String appendix, File... f) throws IOException, GitAPIException {
        Git git = getDB();
        if (git != null) {
            pullCatched();
            for (File boulder : f) {
                try {
                    git.rm().addFilepattern(boulder.getAbsolutePath()).call();
                    //not tracked?
                    if (boulder.exists()) {
                        boulder.delete();
                    }
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                }
            }
            git.commit().setMessage("removed " + f.length + " files. " + appendix).setAuthor(getAuthor()).call();
            push();
        } else {
            for (File boulder : f) {
                boulder.delete();
            }

        }

    }

    private boolean canUp() {
        return gs.getRuser() != null && !gs.getRuser().trim().isEmpty();
    }

    public void pullCatched() {
        try {
            pullUncatched();
        } catch (Exception e) {
            GuiLogHelper.guiLogger.loge(e);
        }
    }

    public void pullUncatched() throws IOException, GitAPIException {
        if (canUp()) {
            Git git = getDB();
            if (git != null) {
                PullResult r = git.pull().setProgressMonitor(new ProgressMonitorImpl()).call();
                if (!r.isSuccessful()) {
                    throw new GitAPIException("pull failed; consult logs and fix manually") {
                    };
                }
            }
        }
    }

    public void push() throws IOException, GitAPIException {
        if (canUp()) {
            Git git = getDB();
            if (git != null) {
                git.push().setCredentialsProvider(new CredentialsProvider() {
                    @Override
                    public boolean isInteractive() {
                        return false;
                    }

                    @Override
                    public boolean supports(CredentialItem... cis) {
                        for (CredentialItem ci : cis) {
                            if (ci instanceof CredentialItem.Password) {
                                GitAuthenticator gi = new GitAuthenticator();
                                char[] v = gi.authenticate(Translator.R("authenticateGit", gs.getRuser(), gs.getUrl(), gs.getBranch()));
                                ((CredentialItem.Password) ci).setValue(v);
                            } else if (ci instanceof CredentialItem.Username) {
                                ((CredentialItem.Username) ci).setValue(gs.getRuser().trim());
                            } else {
                                System.err.println("Unsupported Credentialitem " + ci.getClass().getSimpleName());
                            }
                        }
                        return true;
                    }

                    @Override
                    public boolean get(URIish uriish, CredentialItem... cis) throws UnsupportedCredentialItem {
                        System.out.println(cis);
                        return true;
                    }

                }).call();
            }
        }
    }

    public boolean init(boolean force) throws IOException, GitAPIException {
        Git git = getDB();
        if (!force && git != null) {
            return true;
        }
        if (git != null) {
            git.close();
            git = null;
            db = null;
            FileUtils.deleteDirectory(Files.repoGit);
        }
        File tmp = new File(Files.configDir, "" + new Date().getTime() + ".tmpGit");
        Git.cloneRepository()
                .setURI(gs.getUrl())
                .setBranch(gs.getBranch())
                .setDirectory(tmp)
                .setProgressMonitor(new ProgressMonitorImpl())
                .call();
        FileUtils.copyDirectory(Files.repo, tmp);
        Files.repo.renameTo(new File(Files.repo.getParent(), "backup-" + tmp.getName()));
        tmp.renameTo(Files.repo);
        //if canUp -> git add all? + commit?
        //push()?
        return false;
    }

    public boolean unregisterRm(boolean force) throws IOException {
        Git git = getDB();
        if (!force && git != null) {
            return true;
        }
        if (git != null) {
            git.close();
            git = null;
            db = null;
            FileUtils.deleteDirectory(Files.repoGit);
        }
        return false;
    }

    private PersonIdent getAuthor() {
        return new PersonIdent(
                System.getProperty("user.name"),
                System.getProperty("user.name").replaceAll("[^A-Za-z0-9]", ".") + "@ffb.org"
        );
    }

    public void revoke() {
        GitAuthenticator.revoke();
    }

    public void add(String appendix, File... fs) throws IOException, GitAPIException {
        Git git = getDB();
        if (git != null) {
            pullCatched();
            for (File f : fs) {
                String toAdd = f.getAbsolutePath().replaceAll(Files.repo.getAbsolutePath(), "");
                if (toAdd.startsWith("/") || toAdd.startsWith("\\")) {
                    toAdd = toAdd.substring(1);
                }
                git.add().setUpdate(false).addFilepattern(toAdd).call();
            }
            git.commit().setMessage("added " + fs.length + " files. " + appendix).setAuthor(getAuthor()).call();
            push();
        }
    }

    public void addAll() throws IOException, GitAPIException {
        Path start = Paths.get(Files.repo.toURI());
        List<String> collect = new ArrayList<>();
        java.nio.file.Files.walkFileTree(start, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (dir.toFile().getName().equals(".git")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                collect.add(file.toString());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;

            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });

        File[] f = new File[collect.size()];
        for (int i = 0; i < collect.size(); i++) {
            f[i] = new File(collect.get(i));
        }
        add("Add all used", f);
    }

    public void hardReset() throws IOException, GitAPIException {
        Git git = getDB();
        if (git != null) {
            pullCatched();
            git.reset().setMode(ResetCommand.ResetType.HARD).call();
        }
    }

    private static class ProgressMonitorImpl implements ProgressMonitor {

        String s = "unknown";

        @Override
        public void start(int i) {
            System.out.println(s + "started");
        }

        @Override
        public void beginTask(String string, int i) {
            this.s = string;
        }

        @Override
        public void update(int i) {
            System.out.println(s + " updated");
        }

        @Override
        public void endTask() {
            System.out.println(s + " ended");
        }

        @Override
        public boolean isCancelled() {
            //System.out.println(s + " canceled");
            return false;
        }
    }

}
