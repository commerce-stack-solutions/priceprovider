package io.commercestacksolutions.corebusinessentities.dataaccess.language;

import io.commercestacksolutions.corebusinessentities.dataaccess.language.entity.LanguageEntity;

import java.util.List;

public interface LanguageEntityRepository {

    List<? extends LanguageEntity> findByMandatory(Boolean mandatory);
}
