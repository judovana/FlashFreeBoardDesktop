/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals.grades;

import java.math.BigDecimal;

/**
 *
 * @author jvanek
 */
public class RandomBoulder extends Grade {

    @Override
    public BigDecimal toNumber() {
        return new BigDecimal("0");
    }

    @Override
    public String toString() {
        return "RANDOM";
    }
    
    
}
