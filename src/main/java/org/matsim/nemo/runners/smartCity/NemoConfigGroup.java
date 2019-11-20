package org.matsim.nemo.runners.smartCity;

import org.matsim.core.config.ReflectiveConfigGroup;

public class NemoConfigGroup extends ReflectiveConfigGroup {

	public static final String NAME = "nemo";

	private String serviceAreaShapeFile;

	public NemoConfigGroup() {
		super(NAME);
	}

	public String getServiceAreaShapeFile() {
		return serviceAreaShapeFile;
	}

	public void setServiceAreaShapeFile(String serviceAreaShapeFile) {
		this.serviceAreaShapeFile = serviceAreaShapeFile;
	}
}
