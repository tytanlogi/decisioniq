package com.app.decisioniq.assistant.planning.model;

import lombok.Data;

import java.util.List;

/**
 * SQL-side evidence requirements for a getQuery plan.
 */
@Data
public class SqlData {

    private List<SqlTableSelection> tables;
}
