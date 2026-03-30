package de.ebusyness.priceproviderservice.dataaccess.currency;

import de.ebusyness.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrencyEntityRepository extends JpaRepository<CurrencyEntity, String>, JpaSpecificationExecutor<CurrencyEntity> {
    CurrencyEntity findByCurrencyKey(String currencyKey);
}
