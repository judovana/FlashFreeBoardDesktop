/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals.db;

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
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.fbb.board.Translator;
import org.fbb.board.desktop.Files;
import org.fbb.board.desktop.gui.GitAuthenticator;
import org.fbb.board.internals.GlobalSettings;
import org.fbb.board.internals.GuiLogHelper;

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
            pullCatched(new ExceptionHandler.LoggingEater());
            for (File boulder : f) {
                try {
                    String toAdd = toGitAblePath(boulder);
                    git.rm().addFilepattern(toAdd).call();
                    //not tracked?
                    if (boulder.exists()) {
                        deadlyDeletion(boulder);
                    }
                } catch (Exception ex) {
                    GuiLogHelper.guiLogger.loge(ex);
                }
            }
            git.commit().setMessage("removed " + f.length + " files. " + appendix).setAuthor(getAuthor()).call();
            push();
        } else {
            for (File boulder : f) {
                deadlyDeletion(boulder);
            }

        }

    }

    private boolean canUp() {
        return gs.getRuser() != null && !gs.getRuser().trim().isEmpty();
    }

    public void pullCatched(ExceptionHandler<Throwable> t) {
        try {
            pullUncatched();
        } catch (GitAPIException |  org.eclipse.jgit.errors.CheckoutConflictException ex) {
            GuiLogHelper.guiLogger.loge(ex);
            try {
                hardReset();
            } catch (Throwable ee) {
                t.handleCleanly(ee);
            }
        } catch (Throwable e) {
            t.handleCleanly(e);
        }
    }

    private void pullUncatched() throws IOException, GitAPIException {
        Git git = getDB();
        if (git != null) {
            PullResult r = git.pull().setStrategy(MergeStrategy.THEIRS).setProgressMonitor(new ProgressMonitorImpl()).call();
            if (!r.isSuccessful()) {
                throw new GitAPIException("pull failed; consult logs and fix manually") {
                };
            }
        }
    }

    public void pushCatched(ExceptionHandler x) {
        try {
            push();
        } catch (Exception e) {
            x.handleCleanly(e);
        }
    }

    private void push() throws IOException, GitAPIException {
        GuiLogHelper.guiLogger.logo("Push tempted");
        if (canUp()) {
            Git git = getDB();
            if (git != null) {
                GuiLogHelper.guiLogger.logo("Push running");
                CredentialsProvider cp = getCredentialsProvider();
                git.push().setCredentialsProvider(cp).call();
            }
            GuiLogHelper.guiLogger.logo("Push done");
        } else {
            GuiLogHelper.guiLogger.logo("Push cant");
        }
    }

    private CredentialsProvider getCredentialsProvider() {
        if (gs.getUrl().contains("@") && getToken(gs.getUrl()).length() > 10) {
            return new CredentialsProvider() {
                @Override
                public boolean isInteractive() {
                    return false;
                }

                @Override
                public boolean supports(CredentialItem... cis) {
                    for (CredentialItem ci : cis) {
                        if (ci instanceof CredentialItem.Password) {
                            GitAuthenticator gi = new GitAuthenticator();
                            char[] v = new char[0];
                            ((CredentialItem.Password) ci).setValue(v);
                        } else if (ci instanceof CredentialItem.Username) {
                            ((CredentialItem.Username) ci).setValue(getToken(gs.getUrl()).trim());
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

            };
        } else {
            return new CredentialsProvider() {
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

            };
        }
    }

    private String getToken(String url) {
        String token = url.replaceAll("@.*", "");
        token = token.replaceAll(".*://", "");
        return token;
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
        if (!Files.configDir.exists()) {
            Files.configDir.mkdirs();
        }
        File tmp = new File(Files.configDir, "" + new Date().getTime() + ".tmpGit");
        Git g = Git.cloneRepository()
                .setURI(gs.getUrl())
                .setBranch(gs.getBranch())
                .setDirectory(tmp)
                .setProgressMonitor(new ProgressMonitorImpl())
                .call();
        g.close();//we need to rename, will init it later
        boolean b1 = true;
        if (Files.repo.exists()) {
            FileUtils.copyDirectory(Files.repo, tmp);
            b1 = Files.repo.renameTo(new File(Files.repo.getParent(), "backup-" + tmp.getName()));
        }
        boolean b2 = tmp.renameTo(Files.repo);
        //if canUp -> git add all? + commit?
        //push()?
        if (b1 == false && b2 == false) {
            throw new RuntimeException("Failed both sync and renaming");
        }
        if (b1 == false) {
            throw new RuntimeException("Failed sync");
        }
        if (b2 == false) {
            throw new RuntimeException("Failed renaming");
        }
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

    public void add(ExceptionHandler x, String appendix, File... fs) {
        try {
            add(appendix, fs);
        } catch (IOException | GitAPIException a) {
            x.handleCleanly(a);
        }

    }

    private void add(String appendix, File... fs) throws IOException, GitAPIException {
        Git git = getDB();
        if (git != null) {
            pullCatched(new ExceptionHandler.LoggingEater());
            for (File f : fs) {
                String toAdd = toGitAblePath(f);
                GuiLogHelper.guiLogger.logo("git add "+f.getAbsolutePath()+" as "+toAdd);
                git.add().setUpdate(false).addFilepattern(toAdd).call();
            }
            git.commit().setMessage("added " + fs.length + " files. " + appendix).setAuthor(getAuthor()).call();
            push();
        }
    }

    public String toGitAblePath(File f) {
        String toAdd = f.getAbsolutePath().replace(Files.repo.getAbsolutePath(), "");
        if (toAdd.startsWith("/") || toAdd.startsWith("\\")) {
            toAdd = toAdd.substring(1);
        }
        toAdd = toAdd.replace('\\', '/');
        return toAdd;
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

    public String logCatched() {
        try {
            return log();
        } catch (Exception ex) {
            GuiLogHelper.guiLogger.loge(ex);
        }
        return "Issue occured during log reading.";

    }

    private String log() throws IOException, GitAPIException {
        StringBuilder sb = new StringBuilder("DB LOG\n");
        Git git = getDB();
        if (git != null) {
            Iterable<RevCommit> r = git.log().call();
            for (RevCommit rev : r) {
                sb.append(" * " + rev.getAuthorIdent().getName() + ": " + rev.getAuthorIdent().getEmailAddress() + "\n"
                        + " * " + rev.getAuthorIdent().getWhen().toString() + "\n"
                        + "      " + rev.getShortMessage() + "\n");
            }
            sb.append("END\n");
        }
        return sb.toString();
    }

    public void hardReset() throws IOException, GitAPIException {
        Git git = getDB();
        if (git != null) {
            git.fetch();
            git.reset().setMode(ResetCommand.ResetType.HARD).call();
            pullUncatched();
            
        }
    }

    private static void deadlyDeletion(File boulder) {
        boolean deleted = boulder.delete();
        if (!deleted){
            GuiLogHelper.guiLogger.loge(boulder.getAbsolutePath()+" was not deleted. Will be enforced on shutdown");
            boulder.deleteOnExit();
        }
    }

    private static class ProgressMonitorImpl implements ProgressMonitor {

        String s = "unknown";
        private boolean duration = false;

        @Override
        public void start(int i) {
            System.out.println(s + "started");
            if (duration) {
                System.out.println(new Date());
            }
        }

        @Override
        public void beginTask(String string, int i) {
            this.s = string;
            if (duration) {
                System.out.println(new Date());
            }
        }

        @Override
        public void update(int i) {
            System.out.println(s + " updated");
            if (duration) {
                System.out.println(new Date());
            }
        }

        @Override
        public void endTask() {
            System.out.println(s + " ended");
            if (duration) {
                System.out.println(new Date());
            }
        }

        @Override
        public boolean isCancelled() {
            //System.out.println(s + " canceled");
            return false;
        }

        @Override
        public void showDuration(boolean b) {
            this.duration = b;
        }
    }

}
