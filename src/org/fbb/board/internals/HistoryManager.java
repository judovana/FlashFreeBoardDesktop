/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fbb.board.internals;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 *
 * @author jvanek
 */
public class HistoryManager {

    protected final List<Boulder> history = new ArrayList<>();
    protected int historyIndex = -1;

    //returns whether we are at end or not;
    //return true, if index is NOT last (and thus forward button can be enabld)
    public Boulder getCurrentInHistory() {
        if (history.isEmpty() || historyIndex < 0 || historyIndex > history.size()) {
            return null;
        }
        return history.get(historyIndex);
    }

    public void addToBoulderHistory(Boulder b) {
        if (history.isEmpty()) {
            history.add(b);
            historyIndex = 0;
            return;
        }
        if (historyIndex == history.size() - 1) {
            historyIndex++;
            history.add(b);
            return;
        }
        historyIndex++;
        history.add(historyIndex, b);
        return;
    }

    public boolean canBack() {
        return historyIndex > 0;
    }

    public Vector<Boulder> getHistory() {
        return new Vector<Boulder>(history);
    }

    public void setIndex(String id) {
        for (int i = 0; i < history.size(); i++) {
            Boulder get = history.get(i);
            if (get.getFile().getName().equals(id)) {
                this.historyIndex = i;
                break;
            }
        }

    }

    public Boulder forward() {
        if (history.isEmpty()) {
            return null;
        }
        if (canFwd()) {
            historyIndex++;
            return history.get(historyIndex);
        }
        return history.get(historyIndex);
    }

    public Boulder back() {
        if (history.isEmpty()) {
            return null;
        }
        if (canBack()) {
            historyIndex--;
            return history.get(historyIndex);
        }
        return history.get(historyIndex);
    }

    public boolean canFwd() {
        return historyIndex < history.size() - 1;
    }

    public void clearHistory() {
        historyIndex = -1;
        history.clear();
    }
}
