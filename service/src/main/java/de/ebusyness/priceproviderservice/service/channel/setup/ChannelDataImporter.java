package de.ebusyness.priceproviderservice.service.channel.setup;

import de.ebusyness.commons.service.setup.AbstractSetupDataImporter;
import de.ebusyness.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
import de.ebusyness.priceproviderservice.service.channel.ChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChannelDataImporter extends AbstractSetupDataImporter<ChannelEntity> {

    @Autowired
    public ChannelDataImporter(ChannelService entityService) {
        super(entityService);
    }

    @Override
    public int getPriority() {
        return 80; // Load after tax classes (70) but before price rows
    }

    @Override
    public String getEntityTypeName() {
        return "Channel";
    }

}
