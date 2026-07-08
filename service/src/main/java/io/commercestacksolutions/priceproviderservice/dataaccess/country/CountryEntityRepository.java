package io.commercestacksolutions.priceproviderservice.dataaccess.country;

import io.commercestacksolutions.priceproviderservice.dataaccess.country.entity.CountryEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryEntityRepository extends JpaRepository<CountryEntity, String>, JpaSpecificationExecutor<CountryEntity> {

    @Query("SELECT c FROM CountryEntity c LEFT JOIN FETCH c.allowedCurrencyRefs LEFT JOIN FETCH c.primaryCurrencyRef WHERE c.isoKey = :isoKey")
    Optional<CountryEntity> findByIdWithCurrencies(@Param("isoKey") String isoKey);

    @Query("SELECT COUNT(c) > 0 FROM CountryEntity c WHERE c.primaryCurrencyRef = :currency OR :currency MEMBER OF c.allowedCurrencyRefs")
    boolean existsByCurrencyRef(@Param("currency") CurrencyEntity currency);

    @Query("SELECT c.isoKey FROM CountryEntity c WHERE c.primaryCurrencyRef = :currency OR :currency MEMBER OF c.allowedCurrencyRefs")
    List<String> findIsoKeysByCurrencyRef(@Param("currency") CurrencyEntity currency);
}
