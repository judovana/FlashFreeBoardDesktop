/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.fbb.board.desktop.Files;

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

    public synchronized void delte(File... f) throws IOException, GitAPIException {
        Git git = getDB();
        if (git != null) {
            git.pull().call();
            for (File boulder : f) {
                try {
                    git.rm().addFilepattern(boulder.getAbsolutePath()).call();
                    //not tracked?
                    if (boulder.exists()) {
                        boulder.delete();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            git.commit().setMessage("removed " + f.length + " files").setAuthor("pgm", "pgm@pgm").call();
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

    public void push() throws IOException {
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
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }

                    @Override
                    public boolean get(URIish uriish, CredentialItem... cis) throws UnsupportedCredentialItem {
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }

                });
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

}
