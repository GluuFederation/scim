package org.gluu.oxtrust.service.metric;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.gluu.model.ApplicationType;
import org.gluu.persist.PersistenceEntryManager;

/**
 * Store and retrieve metric
 *
 * @author Yuriy Movchan Date: 01/15/2021
 */
@ApplicationScoped
@Named(MetricService.METRIC_SERVICE_COMPONENT_NAME)
public class MetricService extends org.gluu.service.metric.MetricService {

	public static final String METRIC_SERVICE_COMPONENT_NAME = "metricService";

	private static final long serialVersionUID = 7875838160379126796L;

	@Override
	public String baseDn() {
		return null;
	}

	@Override
	public ApplicationType getApplicationType() {
		return null;
	}

	@Override
	public PersistenceEntryManager getEntryManager() {
		return null;
	}

	@Override
	public org.gluu.service.metric.MetricService getMetricServiceInstance() {
		return null;
	}

	@Override
	public String getNodeIndetifier() {
		return null;
	}

	@Override
	public boolean isMetricReporterEnabled() {
		return false;
	}

} 
