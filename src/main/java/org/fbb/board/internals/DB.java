/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import java.io.File;
import java.io.IOException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.fbb.board.desktop.Files;

/**
 *
 * @author jvanek
 */
public class DB {

    //git clone -b Flash --single-branch https://github.com/FlashFreeBoard/FreeBoard.git
    private Git db = null;

    private synchronized Git getDB() throws IOException {
        if (db == null) {
            Repository repository = FileRepositoryBuilder.create(Files.repoGit);
            db = new Git(repository);
        }
        return db;
    }

    public synchronized void delte(File... f) throws IOException, GitAPIException {
        if (Files.repoGit.exists()) {
            Git git = getDB();
            for (File boulder : f) {
                try {
                    git.rm().addFilepattern(boulder.getAbsolutePath()).call();
                    //not tracked?
                    if (boulder.exists()) {
                        //boulder.delete();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            git.commit().setMessage("removed " + f.length + " files").setAuthor("pgm", "pgm@pgm").call();
        } else {
            for (File boulder : f) {
                boulder.delete();
            }

        }

    }

}
