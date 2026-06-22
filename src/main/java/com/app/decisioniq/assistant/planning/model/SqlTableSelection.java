package com.app.decisioniq.assistant.planning.model;

import lombok.Data;

import java.util.List;

/**
 * One SQL table and the fields required from it for the current getQuery plan.
 */
@Data
public class SqlTableSelection {

    private String name;
    private List<String> fields;
}
