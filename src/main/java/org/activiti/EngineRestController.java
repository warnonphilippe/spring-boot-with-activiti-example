package org.activiti;

import org.springframework.beans.factory.annotation.Autowired;

public class EngineRestController {

    @Autowired
    private EngineFacade engineFacade;

    public EngineRestController() {
    }

    // TODO : compléter pour exposer EngineFacade (avec secu)
}
