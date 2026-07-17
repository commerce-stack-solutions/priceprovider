package io.commercestacksolutions.corebusinessentities.service.language;

import io.commercestacksolutions.corebusinessentities.dataaccess.language.entity.LanguageEntity;

import java.util.List;

public interface LanguageService {

    List<? extends LanguageEntity> getMandatoryLanguages();
}
