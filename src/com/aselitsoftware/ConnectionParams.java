package com.aselitsoftware;

public class ConnectionParams {

		private String name;
		private String masterDatabase;
		private String slaveDatabase;
		
		public ConnectionParams(String name, String masterDatabase, String slaveDatabase) {
			
			super();
			this.setName(name);
			this.setMasterDatabase(masterDatabase);
			this.setSlaveDatabase(slaveDatabase);
		}

		public void setName(String name) {
			
			this.name = name;
		}

		public String getName() {
			
			return name;
		}
		
		public String getMasterDatabase() {
			
			return masterDatabase;
		}

		public void setMasterDatabase(String masterDatabase) {
			
			this.masterDatabase = masterDatabase;
		}

		public String getSlaveDatabase() {
			
			return slaveDatabase;
		}

		public void setSlaveDatabase(String slaveDatabase) {
			
			this.slaveDatabase = slaveDatabase;
		}
}
