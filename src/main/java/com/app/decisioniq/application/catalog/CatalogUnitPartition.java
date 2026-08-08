package com.app.decisioniq.application.catalog;

import java.util.List;

public record CatalogUnitPartition(
        List<SupportedCatalogUnit> supportedUnits,
        List<UnsupportedCatalogUnit> unsupportedUnits
) {
    public CatalogUnitPartition {
        supportedUnits = List.copyOf(supportedUnits);
        unsupportedUnits = List.copyOf(unsupportedUnits);
    }
}
