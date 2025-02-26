package io.kadai.adapter.impl;

import io.kadai.adapter.manager.AdapterManager;
import io.kadai.adapter.systemconnector.api.SystemConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class CsrfTokenRetriever {
  private static final Logger LOGGER = LoggerFactory.getLogger(CsrfTokenRetriever.class);

  private final AdapterManager adapterManager;

  public boolean isCsrfTokenReceived() {
    return csrfTokenReceived;
  }

  private boolean csrfTokenReceived;

  public CsrfTokenRetriever(AdapterManager adapterManager) {
    this.adapterManager = adapterManager;
    this.csrfTokenReceived = false;
  }

  @EventListener(ApplicationReadyEvent.class)
  void init() throws InterruptedException {
    LOGGER.info("Entry to CsrfTokenRetriever");
    adapterManager.init();
    for (SystemConnector systemConnector : (adapterManager.getSystemConnectors().values())) {
      try {
        systemConnector.retrieveCsrfToken();
        this.csrfTokenReceived = true;
      } catch (Exception e) {
        LOGGER.error(
            "Error while retrieving CSRF Token from system with URL "
                + systemConnector.getSystemUrl()
                + " continuing without CSRF Cookie",
            e);
      } finally {
        LOGGER.trace(
            "CsrfToken.retrieveCsrfToken "
                + "Leaving retrieval of CSRF Token for System Connector {}",
            systemConnector.getSystemUrl());
      }
    }
  }
}
