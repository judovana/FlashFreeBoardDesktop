/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals.grades;

import java.math.BigDecimal;
import java.util.Objects;

/**
 *
 * @author jvanek
 */
public abstract class Grade {

    @Override
    public abstract  String toString();

    //two difits in precission to look nice and bee ocmfortbale
    public  abstract  BigDecimal toNumber() ;

    @Override
    public int hashCode() {
        return toNumber().intValue();
    }

    @Override
    public boolean equals(Object obj) {
        return Objects.equals(toNumber(), toNumber());
    }

}
