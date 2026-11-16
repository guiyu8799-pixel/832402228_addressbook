package com.fzu.addressbook.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Outcome of an Excel import: how many rows were stored and which rows were rejected.
 */
public class ImportResult {

    private int imported;
    private int failed;
    private List<String> errors = new ArrayList<>();

    public ImportResult() {
    }

    public ImportResult(int imported, int failed, List<String> errors) {
        this.imported = imported;
        this.failed = failed;
        this.errors = errors;
    }

    public int getImported() {
        return imported;
    }

    public void setImported(int imported) {
        this.imported = imported;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
