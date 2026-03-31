package io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.dbupdate;

import io.commercestacksolutions.commons.dataaccess.dbupdate.AbstractEnumConstraintUpdater;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.enums.PriceType;
import org.springframework.stereotype.Component;

/**
 * Schema updater for PriceType enum constraint.
 * Ensures the database CHECK constraint includes all PriceType enum values.
 */
@Component
public class PriceTypeEnumConstraintUpdater extends AbstractEnumConstraintUpdater {

    @Override
    protected String getTableName() {
        return "price_row_entity";
    }

    @Override
    protected String getColumnName() {
        return "price_type";
    }

    @Override
    protected Class<? extends Enum<?>> getEnumClass() {
        return PriceType.class;
    }

    @Override
    public int getPriority() {
        return 102;
    }
}
