/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.desktop.tutorial;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 *
 * @author jvanek
 */
public class ImgProvider {

    private final Map<String, BufferedImage> cache = new HashMap<>(15);

    public ImgProvider() {

    }

    public BufferedImage get(String name) throws IOException {
        BufferedImage bi = cache.get(name);
        if (bi == null) {
            bi = ImageIO.read((this.getClass().getClassLoader().getResourceAsStream("org/fbb/board/tutorial/imgs/" + name)));
            cache.put(name, bi);
        }
        return bi;
    }

}
