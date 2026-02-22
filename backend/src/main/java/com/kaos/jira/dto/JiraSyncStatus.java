package com.kaos.jira.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Estado de una operación de sincronización con Jira.
 * Registra el resultado de importar issues de un board.
 */
@Getter
@Setter
public class JiraSyncStatus {

    private int issuesImportadas;
    private int issuesActualizadas;
    private final List<String> errores = new ArrayList<>();

    public void addError(String error) {
        this.errores.add(error);
    }

    public boolean tieneErrores() {
        return !errores.isEmpty();
    }
}
