/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Date;
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
            if (gs.getRuser() != null && !gs.getRuser().trim().isEmpty()) {
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
        } else {
            for (File boulder : f) {
                boulder.delete();
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
            java.nio.file.Files.walk(Files.repoGit.toPath())
                    .map(Path::toFile)
                    .sorted(Comparator.comparing(File::isDirectory))
                    .forEach(File::delete);
        }
        File tmp = new File(Files.configDir, "" + new Date().getTime() + ".tmpGit");
        Git.cloneRepository()
                .setURI(gs.getUrl())
                .setBranch(gs.getBranch())
                .setDirectory(tmp)
                .call();
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
            java.nio.file.Files.walk(Files.repoGit.toPath())
                    .map(Path::toFile)
                    .sorted(Comparator.comparing(File::isDirectory))
                    .forEach(File::delete);
        }
        return false;
    }

}
